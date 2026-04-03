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

        child.setParentId(parent.getId());
        userRepository.save(child);

        Device device = deviceRepository.findByUserId(child.getId()).orElse(new Device());
        device.setUserId(child.getId());
        device.setDeviceName(request.getDeviceName());
        device.setDeviceModel(request.getDeviceModel());
        device.setAndroidVersion(request.getAndroidVersion());
        device.setFcmToken(request.getFcmToken());
        device.setIsOnline(true);
        device.setLastSeen(LocalDateTime.now());
        return deviceRepository.save(device);
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
        Device device = deviceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (update.getBatteryLevel() != null) device.setBatteryLevel(update.getBatteryLevel());
        if (update.getIsOnline() != null) device.setIsOnline(update.getIsOnline());
        if (update.getFcmToken() != null) device.setFcmToken(update.getFcmToken());
        device.setLastSeen(LocalDateTime.now());
        deviceRepository.save(device);
    }

    public Device getDeviceByUserId(Long userId) {
        return deviceRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Device not found"));
    }
}
