package com.gia.familycontrol.service

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver
import com.gia.familycontrol.ui.child.LockScreenActivity

class GiaAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isLocking = false

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Check lock state whenever any event happens
        checkAndLock()
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("GiaAccessibility", "Accessibility service connected")
        startLockMonitoring()
    }

    private fun startLockMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                checkAndLock()
                handler.postDelayed(this, 500) // Check every 0.5 seconds
            }
        })
    }

    private fun checkAndLock() {
        if (isLocking) return
        
        val lockPrefs = getSharedPreferences("gia_lock", Context.MODE_PRIVATE)
        val isLocked = lockPrefs.getBoolean("is_locked", false)

        if (isLocked) {
            isLocking = true
            
            // Show overlay immediately
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("GiaAccessibility", "Failed to show lock screen", e)
            }
            
            // Lock device using Device Admin
            handler.postDelayed({
                lockDeviceNow()
                isLocking = false
            }, 1000)
        }
    }

    private fun lockDeviceNow() {
        try {
            val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(this, GiaDeviceAdminReceiver::class.java)

            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                Log.d("GiaAccessibility", "Device locked")
            }
        } catch (e: Exception) {
            Log.e("GiaAccessibility", "Failed to lock device", e)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
