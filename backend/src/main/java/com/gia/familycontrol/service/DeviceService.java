package com.gia.familycontrol.service;

import com.gia.familycontrol.dto.AuthDto;
import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.model.Device;
import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.DeviceRepository;
import com.gia.familycontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

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
