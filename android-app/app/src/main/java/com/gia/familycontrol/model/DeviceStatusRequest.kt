package com.gia.familycontrol.model

data class DeviceStatusRequest(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val isWifiConnected: Boolean,
    val isOnline: Boolean
)
