package com.gia.familycontrol.service;

import com.gia.familycontrol.dto.CommandDto;
import com.gia.familycontrol.firebase.FcmService;
import com.gia.familycontrol.model.Device;
import com.gia.familycontrol.model.Geofence;
import com.gia.familycontrol.model.Location;
import com.gia.familycontrol.model.User;
import com.gia.familycontrol.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final GeofenceRepository geofenceRepository;
    private final FcmService fcmService;

    public Location updateLocation(String email, CommandDto.LocationUpdateRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Device device = deviceRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Device not found"));

        Location location = new Location();
        location.setDeviceId(device.getId());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setAccuracy(request.getAccuracy());
        location.setSpeed(request.getSpeed());
        location.setRecordedAt(LocalDateTime.now());
        locationRepository.save(location);

        device.setLastSeen(LocalDateTime.now());
        if (request.getBatteryLevel() != null) device.setBatteryLevel(request.getBatteryLevel());
        deviceRepository.save(device);

        checkGeofences(device, request.getLatitude(), request.getLongitude(), user.getParentId());
        return location;
    }

    public List<Location> getLocationHistory(Long deviceId, int limit) {
        return locationRepository.findByDeviceIdOrderByRecordedAtDesc(
                deviceId, PageRequest.of(0, limit));
    }

    public Location getLatestLocation(Long deviceId) {
        return locationRepository.findTop1ByDeviceIdOrderByRecordedAtDesc(deviceId)
                .stream().findFirst().orElse(null);
    }

    private void checkGeofences(Device device, double lat, double lng, Long parentId) {
        if (parentId == null) return;
        List<Geofence> geofences = geofenceRepository.findByDeviceIdAndIsActiveTrue(device.getId());
        for (Geofence fence : geofences) {
            double distance = haversineDistance(lat, lng, fence.getLatitude(), fence.getLongitude());
            if (distance > fence.getRadiusM()) {
                userRepository.findById(parentId).ifPresent(parent -> {
                    deviceRepository.findByUserId(parentId).ifPresent(parentDevice -> {
                        if (parentDevice.getFcmToken() != null) {
                            fcmService.sendCommand(parentDevice.getFcmToken(), "GEOFENCE_BREACH",
                                    Map.of("fenceName", fence.getName(), "deviceId", String.valueOf(device.getId())));
                        }
                    });
                });
            }
        }
    }

    private double haversineDistance(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
