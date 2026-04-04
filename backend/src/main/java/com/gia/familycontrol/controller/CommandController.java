package com.gia.familycontrol.controller;

import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.model.Command;
import com.gia.familycontrol.service.CommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class CommandController {

    private final CommandService commandService;

    @PostMapping("/send-command")
    public ResponseEntity<Command> sendCommand(Principal principal,
                                                @Valid @RequestBody CommandDto.SendCommandRequest request) {
        // Check if principal is null
        if (principal == null) {
            log.error("Principal is null - user not authenticated");
            return ResponseEntity.status(403).build();
        }
        
        log.info("Send command request from: {}, type: {}", principal.getName(), request.getCommandType());
        
        // Check if it's an SOS command from child
        if ("SOS".equals(request.getCommandType())) {
            return ResponseEntity.ok(commandService.sendSosCommand(principal.getName(), request));
        }
        // Regular commands require PARENT role
        return ResponseEntity.ok(commandService.sendCommand(principal.getName(), request));
    }

    @GetMapping("/commands/{deviceId}")
    public ResponseEntity<List<Command>> getHistory(@PathVariable Long deviceId) {
        return ResponseEntity.ok(commandService.getCommandHistory(deviceId));
    }

    @PutMapping("/commands/{commandId}/executed")
    public ResponseEntity<Void> markExecuted(@PathVariable Long commandId) {
        commandService.markExecuted(commandId);
        return ResponseEntity.ok().build();
    }
}
