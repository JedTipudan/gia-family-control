package com.gia.familycontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.gia.familycontrol.ui.child.LockScreenActivity

class LockCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("LockCheckReceiver", "Checking lock state...")
        
        val lockPrefs = context.getSharedPreferences("gia_lock", Context.MODE_PRIVATE)
        val isLocked = lockPrefs.getBoolean("is_locked", false)
        
        if (isLocked) {
            Log.d("LockCheckReceiver", "Device is locked, showing lock screen")
            val lockIntent = Intent(context, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            context.startActivity(lockIntent)
        }
    }
}
