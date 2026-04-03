package com.gia.familycontrol.controller;

import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.model.Location;
import com.gia.familycontrol.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/update")
    public ResponseEntity<Location> updateLocation(Principal principal,
                                                    @Valid @RequestBody CommandDto.LocationUpdateRequest request) {
        return ResponseEntity.ok(locationService.updateLocation(principal.getName(), request));
    }

    @GetMapping("/{deviceId}/latest")
    public ResponseEntity<Location> getLatest(@PathVariable Long deviceId) {
        return ResponseEntity.ok(locationService.getLatestLocation(deviceId));
    }

    @GetMapping("/{deviceId}/history")
    public ResponseEntity<List<Location>> getHistory(@PathVariable Long deviceId,
                                                      @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(locationService.getLocationHistory(deviceId, limit));
    }
}
