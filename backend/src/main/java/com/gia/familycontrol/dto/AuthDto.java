package com.gia.familycontrol.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @Email @NotBlank
        private String email;
        @NotBlank @Size(min = 6)
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @Email @NotBlank
        private String email;
        @NotBlank @Size(min = 6)
        private String password;
        @NotBlank
        private String fullName;
        @NotBlank
        private String role; // PARENT or CHILD
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String role;
        private Long userId;
        private String fullName;

        public AuthResponse(String token, String role, Long userId, String fullName) {
            this.token = token;
            this.role = role;
            this.userId = userId;
            this.fullName = fullName;
        }
    }

    @Data
    public static class PairRequest {
        @NotBlank
        private String pairCode;
        private String deviceName;
        private String deviceModel;
        private String androidVersion;
        private String fcmToken;
    }
}
