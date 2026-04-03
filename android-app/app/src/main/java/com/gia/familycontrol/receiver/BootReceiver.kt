package com.gia.familycontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gia.familycontrol.service.AppMonitorService
import com.gia.familycontrol.service.LocationTrackingService
import com.gia.familycontrol.ui.child.LockScreenActivity

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
            val isChild = prefs.getString("role", null) == "CHILD"
            if (isChild) {
                context.startForegroundService(Intent(context, LocationTrackingService::class.java))
                context.startForegroundService(Intent(context, AppMonitorService::class.java))
                
                // Check if device is locked and show lock screen
                val lockPrefs = context.getSharedPreferences("gia_lock", Context.MODE_PRIVATE)
                if (lockPrefs.getBoolean("is_locked", false)) {
                    val lockIntent = Intent(context, LockScreenActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(lockIntent)
                }
            }
        }
    }
}
