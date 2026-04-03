package com.gia.familycontrol.service;

import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.firebase.FcmService;
import com.gia.familycontrol.model.Command;
import com.gia.familycontrol.model.Device;
import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.CommandRepository;
import com.gia.familycontrol.repository.DeviceRepository;
import com.gia.familycontrol.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommandService {

    private final CommandRepository commandRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional
    public Command sendCommand(String parentEmail, CommandDto.SendCommandRequest request) {
        User parent = userRepository.findByEmail(parentEmail)
                .orElseThrow(() -> new RuntimeException("Parent not found"));

        Device device = deviceRepository.findById(request.getTargetDeviceId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        // Verify parent owns this child device
        User child = userRepository.findById(device.getUserId())
                .orElseThrow(() -> new RuntimeException("Child user not found"));
        if (!parent.getId().equals(child.getParentId())) {
            throw new RuntimeException("Unauthorized: device does not belong to your child");
        }

        Command command = new Command();
        command.setSenderId(parent.getId());
        command.setTargetDeviceId(device.getId());
        command.setCommandType(Command.CommandType.valueOf(request.getCommandType()));
        command.setStatus(Command.Status.PENDING);
        commandRepository.save(command);

        // Send via FCM
        switch (command.getCommandType()) {
            case LOCK -> {
                fcmService.sendLockCommand(device.getFcmToken(), true);
                device.setIsLocked(true);
                deviceRepository.save(device);
            }
            case UNLOCK -> {
                fcmService.sendLockCommand(device.getFcmToken(), false);
                device.setIsLocked(false);
                deviceRepository.save(device);
            }
            case BLOCK_APP -> fcmService.sendAppBlockCommand(device.getFcmToken(), request.getPackageName(), true);
            case UNBLOCK_APP -> fcmService.sendAppBlockCommand(device.getFcmToken(), request.getPackageName(), false);
            default -> fcmService.sendCommand(device.getFcmToken(), command.getCommandType().name(), null);
        }

        command.setStatus(Command.Status.DELIVERED);
        return commandRepository.save(command);
    }

    public List<Command> getCommandHistory(Long deviceId) {
        return commandRepository.findByTargetDeviceIdOrderBySentAtDesc(deviceId);
    }

    @Transactional
    public void markExecuted(Long commandId) {
        commandRepository.findById(commandId).ifPresent(cmd -> {
            cmd.setStatus(Command.Status.EXECUTED);
            cmd.setExecutedAt(LocalDateTime.now());
            commandRepository.save(cmd);
        });
    }
}
