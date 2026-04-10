package com.gia.familycontrol.controller;

import com.gia.familycontrol.dto.AuthDto;
import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.model.Device;
import com.gia.familycontrol.service.DeviceService;
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
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/pair-device")
    public ResponseEntity<?> pairDevice(Principal principal,
                                              @Valid @RequestBody AuthDto.PairRequest request) {
        try {
            return ResponseEntity.ok(deviceService.pairDevice(principal.getName(), request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/child-devices")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<List<Device>> getChildDevices(Principal principal) {
        return ResponseEntity.ok(deviceService.getChildDevices(principal.getName()));
    }

    @PostMapping("/unpair-device/{deviceId}")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<Void> unpairDevice(Principal principal, @PathVariable Long deviceId) {
        deviceService.unpairDevice(principal.getName(), deviceId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/device/status")
    public ResponseEntity<Void> updateStatus(Principal principal,
                                              @RequestBody CommandDto.DeviceStatusUpdate update) {
        deviceService.updateStatus(principal.getName(), update);
        return ResponseEntity.ok().build();
    }
}
