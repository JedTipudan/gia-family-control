package com.gia.familycontrol.controller;

import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.model.Command;
import com.gia.familycontrol.service.CommandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommandController {

    private final CommandService commandService;

    @PostMapping("/send-command")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<Command> sendCommand(Principal principal,
                                                @Valid @RequestBody CommandDto.SendCommandRequest request) {
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
