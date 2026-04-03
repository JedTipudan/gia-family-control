package com.gia.familycontrol.service

import android.accessibilityservice.AccessibilityService
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageStatsManager
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
    private var blockedPackages = mutableSetOf<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Check lock state and blocked apps whenever any event happens
        checkAndLock()
        checkBlockedApps()
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("GiaAccessibility", "Accessibility service connected")
        loadBlockedApps()
        startMonitoring()
    }
    
    private fun loadBlockedApps() {
        val prefs = getSharedPreferences("gia_blocked_apps", Context.MODE_PRIVATE)
        blockedPackages = prefs.getStringSet("blocked", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        Log.d("GiaAccessibility", "Loaded ${blockedPackages.size} blocked apps")
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                loadBlockedApps() // Refresh blocked apps
                checkAndLock()
                checkBlockedApps()
                handler.postDelayed(this, 500) // Check every 0.5 seconds
            }
        })
    }
    
    private fun checkBlockedApps() {
        if (blockedPackages.isEmpty()) return
        
        val foregroundApp = getForegroundApp()
        if (foregroundApp != null && foregroundApp in blockedPackages && foregroundApp != packageName) {
            Log.d("GiaAccessibility", "Blocked app detected: $foregroundApp - Closing")
            
            // Send to home screen
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }
    }
    
    private fun getForegroundApp(): String? {
        try {
            val usm = getSystemService(UsageStatsManager::class.java) ?: return null
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5000, now)
            return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            return null
        }
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
