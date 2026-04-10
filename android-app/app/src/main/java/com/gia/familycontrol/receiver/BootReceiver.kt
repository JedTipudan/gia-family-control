package com.gia.familycontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.gia.familycontrol.service.AppMonitorService
import com.gia.familycontrol.service.LocationTrackingService
import com.gia.familycontrol.service.LockMonitorService
import com.gia.familycontrol.service.StatusBarBlockerService
import com.gia.familycontrol.ui.child.LockScreenActivity
import com.gia.familycontrol.util.AppHideManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Received: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                startServicesIfNeeded(context)
            }
        }
    }
    
    private fun startServicesIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
        val role = prefs.getString("role", null)
        val deviceId = prefs.getLong("device_id", -1L)
        
        Log.d("BootReceiver", "Role: $role, DeviceId: $deviceId")
        
        // Start services for child devices that are paired
        if (role == "CHILD" && deviceId != -1L) {
            Log.d("BootReceiver", "Starting services after boot...")
            // Reapply hidden apps via Device Owner
            AppHideManager.reapplyOnBoot(context)

            // Reapply notification blocker if it was active
            val notifBlocked = prefs.getBoolean("notifications_blocked", false)
            if (notifBlocked && android.provider.Settings.canDrawOverlays(context)) {
                StatusBarBlockerService.start(context)
            }
            
            try {
                val locationIntent = Intent(context, LocationTrackingService::class.java)
                val appMonitorIntent = Intent(context, AppMonitorService::class.java)
                val lockMonitorIntent = Intent(context, LockMonitorService::class.java)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(locationIntent)
                    context.startForegroundService(appMonitorIntent)
                    context.startForegroundService(lockMonitorIntent)
                } else {
                    context.startService(locationIntent)
                    context.startService(appMonitorIntent)
                    context.startService(lockMonitorIntent)
                }
                
                Log.d("BootReceiver", "All services started successfully")
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to start services", e)
            }
            
            // Check if device is locked and lock it immediately
            val lockPrefs = context.getSharedPreferences("gia_lock", Context.MODE_PRIVATE)
            if (lockPrefs.getBoolean("is_locked", false)) {
                Log.d("BootReceiver", "Device is locked, locking now")
                try {
                    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
                    val adminComponent = android.content.ComponentName(context, com.gia.familycontrol.admin.GiaDeviceAdminReceiver::class.java)
                    
                    if (dpm.isAdminActive(adminComponent)) {
                        dpm.lockNow()
                        Log.d("BootReceiver", "Device locked using Device Admin")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to lock device", e)
                }
            }
        }
    }
}
