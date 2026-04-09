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
    private var lastForegroundApp: String? = null
    private var consecutiveBlockCount = 0

    // Packages blocked from foreground (Settings, notification panel apps)
    private val BLOCKED_SYSTEM_PACKAGES = setOf(
        "com.android.settings",
        "com.samsung.android.settings",
        "com.miui.securitycenter",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.samsung.android.packageinstaller"
    )

    // Notification panel / quick settings window titles to close
    private val BLOCKED_WINDOW_TITLES = setOf(
        "Volume panel", "Quick Settings", "Notification shade"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val prefs = getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
        val settingsHidden = prefs.getBoolean("settings_hidden", false)
        val notifBlocked   = prefs.getBoolean("notifications_blocked", false)
        val pkg = event.packageName?.toString() ?: ""

        // Block notification panel / quick settings pull-down (separate toggle)
        if (notifBlocked || settingsHidden) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
                val className = event.className?.toString() ?: ""
                if (pkg == "com.android.systemui" ||
                    className.contains("NotificationShade", ignoreCase = true) ||
                    className.contains("QuickSettings", ignoreCase = true) ||
                    className.contains("StatusBar", ignoreCase = true)) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                    } else {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                    return
                }
            }
        }

        if (settingsHidden) {
            // Block Settings app from opening
            if (pkg in BLOCKED_SYSTEM_PACKAGES) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }

            // Block long press on launcher (prevents uninstall context menu)
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                if (pkg.contains("launcher", ignoreCase = true) ||
                    pkg == "com.google.android.apps.nexuslauncher" ||
                    pkg == "com.sec.android.app.launcher" ||
                    pkg == "com.miui.home") {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    return
                }
            }
        }

        checkAndLock()
        checkBlockedApps()
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("GiaAccessibility", "=== Accessibility service connected ===")
        loadBlockedApps()
        startMonitoring()
    }

    private fun loadBlockedApps() {
        val prefs = getSharedPreferences("gia_blocked_apps", Context.MODE_PRIVATE)
        blockedPackages = prefs.getStringSet("blocked", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    private fun startMonitoring() {
        handler.post(object : Runnable {
            override fun run() {
                try {
                    loadBlockedApps()
                    checkAndLock()
                    checkBlockedApps()
                } catch (e: Exception) {
                    Log.e("GiaAccessibility", "Error in monitoring", e)
                }
                handler.postDelayed(this, 200)
            }
        })
    }

    private fun checkBlockedApps() {
        val foregroundApp = getForegroundApp()
        if (blockedPackages.isEmpty()) return
        if (foregroundApp != lastForegroundApp && foregroundApp != null) {
            lastForegroundApp = foregroundApp
            consecutiveBlockCount = 0
        }
        if (foregroundApp != null && foregroundApp in blockedPackages && foregroundApp != packageName) {
            consecutiveBlockCount++
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            try { startActivity(homeIntent) } catch (_: Exception) {}
        }
    }

    private fun getForegroundApp(): String? {
        try {
            val usm = getSystemService(UsageStatsManager::class.java)
            if (usm != null) {
                val now = System.currentTimeMillis()
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5000, now)
                if (!stats.isNullOrEmpty())
                    return stats.maxByOrNull { it.lastTimeUsed }?.packageName
            }
            @Suppress("DEPRECATION")
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            return am.getRunningTasks(1).firstOrNull()?.topActivity?.packageName
        } catch (_: Exception) {}
        return null
    }

    private fun checkAndLock() {
        if (isLocking) return
        val isLocked = getSharedPreferences("gia_lock", Context.MODE_PRIVATE)
            .getBoolean("is_locked", false)
        if (isLocked) {
            isLocking = true
            try {
                startActivity(Intent(this, LockScreenActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                })
            } catch (_: Exception) {}
            handler.postDelayed({
                try {
                    val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    val admin = ComponentName(this, GiaDeviceAdminReceiver::class.java)
                    if (dpm.isAdminActive(admin)) dpm.lockNow()
                } catch (_: Exception) {}
                isLocking = false
            }, 1000)
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
