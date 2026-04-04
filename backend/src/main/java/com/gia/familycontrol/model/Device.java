package com.gia.familycontrol.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "device_model")
    private String deviceModel;

    @Column(name = "android_version")
    private String androidVersion;

    @Column(name = "fcm_token", columnDefinition = "TEXT")
    private String fcmToken;

    @Column(name = "battery_level")
    private Integer batteryLevel = 0;
    
    @Column(name = "is_charging")
    private Boolean isCharging = false;
    
    @Column(name = "is_wifi_connected")
    private Boolean isWifiConnected = false;

    @Column(name = "is_online")
    private Boolean isOnline = false;

    @Column(name = "is_locked")
    private Boolean isLocked = false;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
