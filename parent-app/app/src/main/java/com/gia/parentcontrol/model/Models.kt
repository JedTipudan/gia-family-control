package com.gia.parentcontrol.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)

data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String = "PARENT"
)

data class AuthResponse(
    val token: String,
    val role: String,
    val userId: Long,
    val fullName: String
)

data class DeviceResponse(
    val id: Long,
    val deviceName: String?,
    val isLocked: Boolean,
    val batteryLevel: Int,
    val isOnline: Boolean,
    val connectionType: String?,
    val lastSeen: String?
)

data class DeviceStatusUpdate(
    val batteryLevel: Int?,
    val isOnline: Boolean?,
    val fcmToken: String?,
    val connectionType: String?
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
    val packageName: String? = null,
    val metadata: String? = null
)

data class CommandResponse(val id: Long, val commandType: String, val status: String)

data class AppControlRequest(
    val deviceId: Long,
    val packageName: String,
    val controlType: String
)

data class AppControlResponse(
    val id: Long,
    val packageName: String,
    val controlType: String
)

data class ScheduledLock(
    val id: Long = 0,
    val deviceId: Long,
    val label: String = "Bedtime",
    val lockTime: String,
    val unlockTime: String,
    val days: String = "",
    val isActive: Boolean = true
)

data class AppResponse(
    val id: Long,
    val packageName: String,
    val appName: String,
    val isSystem: Boolean
)

data class UserProfileResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String,
    val pairCode: String?
)

/** UI model combining installed app info with its current control state */
data class ManagedApp(
    val packageName: String,
    val appName: String,
    val isSystem: Boolean,
    var isBlocked: Boolean = false,
    var isHidden: Boolean = false
)
