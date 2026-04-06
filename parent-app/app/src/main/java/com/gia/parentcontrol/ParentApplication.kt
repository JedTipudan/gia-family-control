package com.gia.parentcontrol

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ParentApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ALERTS, "Parental Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts from child device" })
            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_SOS, "SOS Emergency", NotificationManager.IMPORTANCE_MAX
            ).apply {
                description = "Emergency SOS alerts"
                enableVibration(true)
            })
        }
    }

    companion object {
        const val CHANNEL_ALERTS = "parent_alerts"
        const val CHANNEL_SOS = "parent_sos"
    }
}
