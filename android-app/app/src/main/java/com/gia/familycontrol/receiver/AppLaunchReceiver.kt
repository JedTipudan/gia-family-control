package com.gia.familycontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AppLaunchReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_ON -> {
                Log.d("AppLaunchReceiver", "Screen turned ON")
                checkBlockedApps(context)
            }
            Intent.ACTION_USER_PRESENT -> {
                Log.d("AppLaunchReceiver", "User unlocked device")
                checkBlockedApps(context)
            }
        }
    }
    
    private fun checkBlockedApps(context: Context) {
        // Trigger app monitor service to check immediately
        val intent = Intent("com.gia.familycontrol.CHECK_APPS_NOW")
        context.sendBroadcast(intent)
    }
}
