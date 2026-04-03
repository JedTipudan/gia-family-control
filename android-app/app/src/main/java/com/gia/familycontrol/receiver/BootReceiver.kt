package com.gia.familycontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.gia.familycontrol.service.AppMonitorService
import com.gia.familycontrol.service.LocationTrackingService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
            val isChild = prefs.getString("role", null) == "CHILD"
            if (isChild) {
                context.startForegroundService(Intent(context, LocationTrackingService::class.java))
                context.startForegroundService(Intent(context, AppMonitorService::class.java))
            }
        }
    }
}
