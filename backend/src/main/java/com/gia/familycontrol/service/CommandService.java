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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
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
        
        // Get child user
        User child = userRepository.findByEmail(childEmail)
                .orElseThrow(() -> {
                    log.error("Child user not found: {}", childEmail);
                    return new RuntimeException("Child user not found");
                });
        
        log.info("Child found: {} (ID: {})", child.getFullName(), child.getId());
        log.info("Child parent_id: {}", child.getParentId());
        
        // Check if child is paired with parent
        if (child.getParentId() == null) {
            log.error("CRITICAL: Child has no parent_id! Child must be paired first.");
            log.error("Child ID: {}, Email: {}, Role: {}", child.getId(), child.getEmail(), child.getRole());
            throw new RuntimeException("This device is not paired with a parent. Please pair with parent first to use SOS.");
        }
        
        // Get child's device
        Device childDevice = deviceRepository.findByUserId(child.getId())
                .orElseThrow(() -> {
                    log.error("Child device not found for user ID: {}", child.getId());
                    return new RuntimeException("Child device not found");
                });
        
        log.info("Child device found: ID {}, Name: {}", childDevice.getId(), childDevice.getDeviceName());
        
        // Get parent user
        User parent = userRepository.findById(child.getParentId())
                .orElseThrow(() -> {
                    log.error("Parent user not found with ID: {}", child.getParentId());
                    return new RuntimeException("Parent user not found");
                });
        
        log.info("Parent found: {} (ID: {})", parent.getFullName(), parent.getId());
        
        // Get parent's device — optional, only needed for FCM
        Device parentDevice = deviceRepository.findByUserId(parent.getId()).orElse(null);
        
        // Save SOS command to database regardless
        Command command = new Command();
        command.setSenderId(child.getId());
        command.setTargetDeviceId(childDevice.getId());
        command.setCommandType(Command.CommandType.SOS);
        command.setStatus(Command.Status.PENDING);
        commandRepository.save(command);
        
        // Send FCM only if parent has a device with FCM token
        if (parentDevice != null && parentDevice.getFcmToken() != null && !parentDevice.getFcmToken().trim().isEmpty()) {
            try {
                fcmService.sendSosAlert(parentDevice.getFcmToken(), child.getFullName(), request.getMetadata());
                command.setStatus(Command.Status.DELIVERED);
                log.info("✅ SOS alert sent via FCM");
            } catch (Exception e) {
                log.error("❌ FCM SOS failed: {}", e.getMessage());
                command.setStatus(Command.Status.FAILED);
            }
        } else {
            log.warn("⚠️ Parent has no FCM token — SOS saved to DB only");
            command.setStatus(Command.Status.DELIVERED);
        }
        
        commandRepository.save(command);
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
        try {
            command.setCommandType(Command.CommandType.valueOf(request.getCommandType()));
        } catch (IllegalArgumentException e) {
            // Unknown command type — store as a generic command, still send via FCM
            command.setCommandType(Command.CommandType.LOCK); // placeholder, overridden below
            log.warn("Unknown command type: {}, sending via FCM anyway", request.getCommandType());
        }
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
            default -> {
                java.util.Map<String, String> data = new java.util.HashMap<>();
                if (request.getPackageName() != null) data.put("packageName", request.getPackageName());
                if (request.getMetadata()    != null) data.put("metadata",    request.getMetadata());
                if ("GRANT_TEMP_ACCESS".equals(request.getCommandType()) && request.getMetadata() != null) {
                    data.put("minutes", request.getMetadata());
                }
                if ("SET_PIN".equals(request.getCommandType()) && request.getMetadata() != null) {
                    data.put("pin", request.getMetadata());
                }
                // Always use the original request commandType string for FCM (not the enum name)
                fcmService.sendCommand(device.getFcmToken(), request.getCommandType(),
                        data.isEmpty() ? null : data);
            }
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
