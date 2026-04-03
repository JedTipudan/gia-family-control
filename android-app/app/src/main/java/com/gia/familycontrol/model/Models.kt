package com.gia.familycontrol.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(val email: String, val password: String,
                           val fullName: String, val role: String)

data class AuthResponse(
    val token: String,
    val role: String,
    val userId: Long,
    val fullName: String
)

data class PairRequest(
    val pairCode: String,
    val deviceName: String?,
    val deviceModel: String?,
    val androidVersion: String?,
    val fcmToken: String?
)

data class DeviceResponse(
    val id: Long,
    val deviceName: String?,
    val isLocked: Boolean,
    val batteryLevel: Int
)

data class DeviceStatusUpdate(
    val batteryLevel: Int?,
    val isOnline: Boolean?,
    val fcmToken: String?
)

data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val speed: Float?,
    val batteryLevel: Int?
)

data class LocationResponse(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val speed: Float?,
    val recordedAt: String
)

data class SendCommandRequest(
    val targetDeviceId: Long,
    val commandType: String,
    val packageName: String? = null
)

data class CommandResponse(
    val id: Long,
    val commandType: String,
    val status: String
)

data class AppControlRequest(
    val deviceId: Long,
    val packageName: String,
    val controlType: String,
    val scheduleStart: String? = null,
    val scheduleEnd: String? = null,
    val scheduleDays: String? = null
)

data class AppControlResponse(
    val id: Long,
    val packageName: String,
    val controlType: String
)

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    var isBlocked: Boolean = false
)

data class UserProfileResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String,
    val pairCode: String?
)
