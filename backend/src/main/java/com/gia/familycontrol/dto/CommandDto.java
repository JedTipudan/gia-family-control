package com.gia.familycontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class CommandDto {

    @Data
    public static class SendCommandRequest {
        @NotNull
        private Long targetDeviceId;
        @NotBlank
        private String commandType;
        private String packageName; // for app block/unblock
    }

    @Data
    public static class LocationUpdateRequest {
        @NotNull
        private Double latitude;
        @NotNull
        private Double longitude;
        private Float accuracy;
        private Float speed;
        private Integer batteryLevel;
    }

    @Data
    public static class AppControlRequest {
        @NotNull
        private Long deviceId;
        @NotBlank
        private String packageName;
        @NotBlank
        private String controlType; // BLOCKED, ALLOWED, SCHEDULED
        private String scheduleStart;
        private String scheduleEnd;
        private String scheduleDays;
    }

    @Data
    public static class DeviceStatusUpdate {
        private Integer batteryLevel;
        private Boolean isOnline;
        private String fcmToken;
    }
}
