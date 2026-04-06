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
        
        // Get parent's device
        Device parentDevice = deviceRepository.findByUserId(parent.getId())
                .orElseThrow(() -> {
                    log.error("Parent device not found for user ID: {}", parent.getId());
                    return new RuntimeException("Parent device not found. Parent must login to their device first.");
                });
        
        log.info("Parent device found: ID {}, FCM token: {}", 
                parentDevice.getId(), 
                parentDevice.getFcmToken() != null ? "SET" : "NULL");
        
        // Validate parent FCM token
        if (parentDevice.getFcmToken() == null || parentDevice.getFcmToken().trim().isEmpty()) {
            throw new RuntimeException("Parent device is not registered for notifications. Parent must login to their app first.");
        }
        
        // Save SOS command to database
        Command command = new Command();
        command.setSenderId(child.getId());
        command.setTargetDeviceId(parentDevice.getId());
        command.setCommandType(Command.CommandType.SOS);
        command.setStatus(Command.Status.DELIVERED);
        commandRepository.save(command);
        
        log.info("SOS command saved to database: ID {}", command.getId());
        
        // Send urgent FCM notification to parent
        log.info("Sending FCM SOS alert to parent device...");
        log.info("Child Name: {}", child.getFullName());
        log.info("Location: {}", request.getMetadata());
        
        try {
            fcmService.sendSosAlert(
                parentDevice.getFcmToken(), 
                child.getFullName(), 
                request.getMetadata()
            );
            log.info("✅ SOS alert sent successfully via FCM");
        } catch (Exception e) {
            log.error("❌ Failed to send FCM SOS alert", e);
            throw new RuntimeException("Failed to send SOS alert to parent: " + e.getMessage());
        }
        
        log.info("=== SOS COMMAND COMPLETED SUCCESSFULLY ===");
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
            default -> {
                // Build metadata map — include packageName and metadata if present
                java.util.Map<String, String> data = new java.util.HashMap<>();
                if (request.getPackageName() != null) data.put("packageName", request.getPackageName());
                if (request.getMetadata()    != null) data.put("metadata",    request.getMetadata());
                // For GRANT_TEMP_ACCESS the minutes come in metadata field
                if ("GRANT_TEMP_ACCESS".equals(request.getCommandType()) && request.getMetadata() != null) {
                    data.put("minutes", request.getMetadata());
                }
                fcmService.sendCommand(device.getFcmToken(), command.getCommandType().name(),
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
