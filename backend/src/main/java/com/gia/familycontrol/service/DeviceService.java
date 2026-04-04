package com.gia.familycontrol.service;

import com.gia.familycontrol.dto.AuthDto;
import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.firebase.FcmService;
import com.gia.familycontrol.model.Device;
import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.DeviceRepository;
import com.gia.familycontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional
    public Device pairDevice(String childEmail, AuthDto.PairRequest request) {
        User child = userRepository.findByEmail(childEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User parent = userRepository.findByPairCode(request.getPairCode())
                .orElseThrow(() -> new RuntimeException("Invalid pair code"));

        if (parent.getRole() != User.Role.PARENT) {
            throw new RuntimeException("Invalid pair code");
        }

        System.out.println("Pairing: Child=" + child.getEmail() + " (ID=" + child.getId() + ") with Parent=" + parent.getEmail() + " (ID=" + parent.getId() + ")");
        
        child.setParentId(parent.getId());
        userRepository.save(child);
        
        System.out.println("Child parent_id set to: " + child.getParentId());

        Device device = deviceRepository.findByUserId(child.getId()).orElse(new Device());
        device.setUserId(child.getId());
        device.setDeviceName(request.getDeviceName());
        device.setDeviceModel(request.getDeviceModel());
        device.setAndroidVersion(request.getAndroidVersion());
        device.setFcmToken(request.getFcmToken());
        device.setIsOnline(true);
        device.setLastSeen(LocalDateTime.now());
        Device savedDevice = deviceRepository.save(device);
        
        System.out.println("Device saved: ID=" + savedDevice.getId() + ", Name=" + savedDevice.getDeviceName());
        
        // Notify parent about new pairing (if parent has FCM token)
        try {
            deviceRepository.findByUserId(parent.getId()).ifPresentOrElse(
                parentDevice -> {
                    if (parentDevice.getFcmToken() != null && !parentDevice.getFcmToken().isEmpty()) {
                        System.out.println("Sending CHILD_PAIRED notification to parent FCM: " + parentDevice.getFcmToken());
                        try {
                            fcmService.sendCommand(parentDevice.getFcmToken(), "CHILD_PAIRED", 
                                Map.of(
                                    "childDeviceId", String.valueOf(savedDevice.getId()),
                                    "deviceName", request.getDeviceName() != null ? request.getDeviceName() : "Child Device",
                                    "deviceModel", request.getDeviceModel() != null ? request.getDeviceModel() : ""
                                ));
                            System.out.println("CHILD_PAIRED notification sent successfully");
                        } catch (Exception e) {
                            System.err.println("Failed to send FCM notification: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Parent FCM token is null or empty, skipping notification");
                    }
                },
                () -> System.out.println("Parent device not found in database")
            );
        } catch (Exception e) {
            System.err.println("Error sending pairing notification: " + e.getMessage());
        }
        
        return savedDevice;
    }

    @Transactional
    public void unpairDevice(String parentEmail, Long deviceId) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
        
        User child = userRepository.findById(device.getUserId())
                .orElseThrow(() -> new RuntimeException("Child user not found"));
        
        // Verify parent owns this child device
        if (!parent.getId().equals(child.getParentId())) {
            throw new RuntimeException("Unauthorized: You don't own this device");
        }
        
        // Send unpair command to child device
        if (device.getFcmToken() != null) {
            fcmService.sendCommand(device.getFcmToken(), "UNPAIR", Map.of());
        }
        
        // Remove parent relationship
        child.setParentId(null);
        userRepository.save(child);
    }

    @Transactional
    public void updateStatus(String email, CommandDto.DeviceStatusUpdate update) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Find or create device
        Device device = deviceRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    System.out.println("Creating new device for user: " + user.getEmail() + " (ID: " + user.getId() + ")");
                    Device newDevice = new Device();
                    newDevice.setUserId(user.getId());
                    newDevice.setDeviceName(user.getRole() == User.Role.PARENT ? "Parent Device" : "Child Device");
                    return newDevice;
                });

        if (update.getBatteryLevel() != null) device.setBatteryLevel(update.getBatteryLevel());
        if (update.getIsOnline() != null) device.setIsOnline(update.getIsOnline());
        if (update.getFcmToken() != null) device.setFcmToken(update.getFcmToken());
        if (update.getConnectionType() != null) device.setConnectionType(update.getConnectionType());
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
        
        System.out.println("Device status updated: User=" + user.getEmail() + ", Device ID=" + device.getId() + ", Battery=" + device.getBatteryLevel() + "%, Connection=" + device.getConnectionType());
    }

    public Device getDeviceByUserId(Long userId) {
        return deviceRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
    }
    
    public List<Device> getChildDevices(String parentEmail) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Find all children of this parent
        List<User> children = userRepository.findByParentId(parent.getId());
        
        System.out.println("Parent ID: " + parent.getId() + ", Children count: " + children.size());
        
        // Get devices for all children
        List<Device> devices = children.stream()
                .map(child -> {
                    System.out.println("Child ID: " + child.getId() + ", Email: " + child.getEmail());
                    return deviceRepository.findByUserId(child.getId());
                })
                .filter(opt -> opt.isPresent())
                .map(opt -> opt.get())
                .toList();
        
        System.out.println("Devices found: " + devices.size());
        return devices;
    }
}
