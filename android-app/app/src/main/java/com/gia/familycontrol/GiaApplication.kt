package com.gia.familycontrol

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

class GiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initDarkMode()
        createNotificationChannels()
    }

    private fun initDarkMode() {
        val prefs = getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_LOCATION, "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Device is being monitored by parent" })

            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_COMMANDS, "Parental Commands",
                NotificationManager.IMPORTANCE_HIGH
            ))

            manager.createNotificationChannel(NotificationChannel(
                CHANNEL_ALERTS, "Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ))
        }
    }

    companion object {
        const val CHANNEL_LOCATION = "gia_location"
        const val CHANNEL_COMMANDS = "gia_commands"
        const val CHANNEL_ALERTS = "gia_alerts"
    }
}
