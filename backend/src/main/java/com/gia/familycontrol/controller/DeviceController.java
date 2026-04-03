package com.gia.familycontrol.controller;

import com.gia.familycontrol.dto.AuthDto;
import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.model.Device;
import com.gia.familycontrol.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/pair-device")
    public ResponseEntity<Device> pairDevice(Principal principal,
                                              @Valid @RequestBody AuthDto.PairRequest request) {
        return ResponseEntity.ok(deviceService.pairDevice(principal.getName(), request));
    }

    @PutMapping("/device/status")
    public ResponseEntity<Void> updateStatus(Principal principal,
                                              @RequestBody CommandDto.DeviceStatusUpdate update) {
        deviceService.updateStatus(principal.getName(), update);
        return ResponseEntity.ok().build();
    }
}
