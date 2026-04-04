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
    public Command sendSosCommand(String childEmail, CommandDto.SendCommandRequest request) {
        log.info("=== SOS COMMAND RECEIVED ===");
        log.info("Child email: {}", childEmail);
        log.info("Request: {}", request);
        
        User child = userRepository.findByEmail(childEmail)
                .orElseThrow(() -> new RuntimeException("Child not found"));
        
        log.info("Child found: {} (ID: {})", child.getFullName(), child.getId());
        
        // Get child's device
        Device childDevice = deviceRepository.findByUserId(child.getId())
                .orElseThrow(() -> new RuntimeException("Device not found"));
        
        log.info("Child device found: ID {}", childDevice.getId());
        
        // Get parent
        User parent = userRepository.findById(child.getParentId())
                .orElseThrow(() -> new RuntimeException("Parent not found"));
        
        log.info("Parent found: {} (ID: {})", parent.getFullName(), parent.getId());
        
        // Get parent's device
        Device parentDevice = deviceRepository.findByUserId(parent.getId())
                .orElseThrow(() -> new RuntimeException("Parent device not found"));
        
        log.info("Parent device found: ID {}, FCM: {}", parentDevice.getId(), parentDevice.getFcmToken());
        
        // Save SOS command
        Command command = new Command();
        command.setSenderId(child.getId());
        command.setTargetDeviceId(parentDevice.getId());
        command.setCommandType(Command.CommandType.SOS);
        command.setStatus(Command.Status.DELIVERED);
        commandRepository.save(command);
        
        log.info("SOS command saved: ID {}", command.getId());
        
        // Send urgent notification to parent with sound/vibration
        log.info("Sending FCM SOS alert to parent...");
        fcmService.sendSosAlert(parentDevice.getFcmToken(), child.getFullName(), request.getMetadata());
        
        log.info("=== SOS COMMAND COMPLETED ===");
        return command;
    }
    
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
