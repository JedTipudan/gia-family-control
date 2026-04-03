package com.gia.familycontrol

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.gia.familycontrol.service.AppMonitorService
import com.gia.familycontrol.service.LocationTrackingService
import com.gia.familycontrol.service.LockMonitorService
import com.gia.familycontrol.ui.child.LockScreenActivity

class GiaApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initDarkMode()
        createNotificationChannels()
        startServicesIfNeeded()
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
    
    private fun startServicesIfNeeded() {
        val prefs = getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
        val role = prefs.getString("role", null)
        val deviceId = prefs.getLong("device_id", -1L)
        
        Log.d("GiaApplication", "App started - Role: $role, DeviceId: $deviceId")
        
        // Auto-start services for paired child devices
        if (role == "CHILD" && deviceId != -1L) {
            try {
                val locationIntent = Intent(this, LocationTrackingService::class.java)
                val appMonitorIntent = Intent(this, AppMonitorService::class.java)
                val lockMonitorIntent = Intent(this, LockMonitorService::class.java)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(locationIntent)
                    startForegroundService(appMonitorIntent)
                    startForegroundService(lockMonitorIntent)
                } else {
                    startService(locationIntent)
                    startService(appMonitorIntent)
                    startService(lockMonitorIntent)
                }
                
                Log.d("GiaApplication", "Services started automatically")
            } catch (e: Exception) {
                Log.e("GiaApplication", "Failed to start services", e)
            }
            
            // Check if device is locked
            val lockPrefs = getSharedPreferences("gia_lock", Context.MODE_PRIVATE)
            if (lockPrefs.getBoolean("is_locked", false)) {
                Log.d("GiaApplication", "Device is locked, showing lock screen")
                val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(lockIntent)
            }
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
