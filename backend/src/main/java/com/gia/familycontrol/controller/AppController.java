package com.gia.familycontrol.controller;

import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.model.AppControl;
import com.gia.familycontrol.service.AppControlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/apps")
@RequiredArgsConstructor
public class AppController {

    private final AppControlService appControlService;

    @GetMapping("/controls/{deviceId}")
    public ResponseEntity<List<AppControl>> getControls(@PathVariable Long deviceId) {
        return ResponseEntity.ok(appControlService.getAppControls(deviceId));
    }

    @PostMapping("/control")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<AppControl> setControl(Principal principal,
                                                   @Valid @RequestBody CommandDto.AppControlRequest request) {
        return ResponseEntity.ok(appControlService.setAppControl(principal.getName(), request));
    }

    @DeleteMapping("/control/{deviceId}/{packageName}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<Void> removeControl(@PathVariable Long deviceId,
                                               @PathVariable String packageName) {
        appControlService.removeAppControl(deviceId, packageName);
        return ResponseEntity.ok().build();
    }
}
