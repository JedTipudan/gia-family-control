package com.gia.familycontrol.service

import android.app.Notification
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.GiaApplication
import com.gia.familycontrol.R
import com.gia.familycontrol.model.SendCommandRequest
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.ui.child.AppBlockOverlayActivity
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AppMonitorService : LifecycleService() {

    private val api by lazy { RetrofitClient.create(this) }
    private var blockedPackages = mutableSetOf<String>()
    private var monitorJob: Job? = null
    private val notifiedGames = mutableSetOf<String>()
    private val gameKeywords = listOf("game", "play", "arcade", "puzzle", "racing", "action", "adventure")
    private var lastForegroundApp: String? = null
    private var consecutiveBlockCount = 0
    
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.gia.familycontrol.REFRESH_BLOCKED_APPS" -> {
                    Log.d("AppMonitorService", "📡 Received refresh broadcast")
                    loadBlockedAppsFromPrefs()
                }
                "com.gia.familycontrol.CHECK_APPS_NOW" -> {
                    Log.d("AppMonitorService", "⚡ Immediate check requested")
                    // Check immediately
                    lifecycleScope.launch {
                        val foregroundApp = getForegroundApp()
                        if (foregroundApp != null && foregroundApp in blockedPackages) {
                            Log.d("AppMonitorService", "⛔ BLOCKED: $foregroundApp")
                            forceCloseApp(foregroundApp)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AppMonitorService", "=== Service onCreate ===")
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            
            // Register broadcast receiver
            val filter = IntentFilter().apply {
                addAction("com.gia.familycontrol.REFRESH_BLOCKED_APPS")
                addAction("com.gia.familycontrol.CHECK_APPS_NOW")
            }
            registerReceiver(refreshReceiver, filter)
            
            loadBlockedAppsFromPrefs()
            loadBlockedAppsFromApi()
            startMonitoring()
            Log.d("AppMonitorService", "✅ Service created successfully")
        } catch (e: Exception) {
            Log.e("AppMonitorService", "❌ Failed to create service", e)
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun loadBlockedAppsFromPrefs() {
        val prefs = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked", mutableSetOf()) ?: mutableSetOf()
        blockedPackages = blocked.toMutableSet()
        Log.d("AppMonitorService", "📋 Loaded ${blockedPackages.size} blocked apps from prefs: $blockedPackages")
    }
    
    private fun loadBlockedAppsFromApi() {
        lifecycleScope.launch {
            try {
                val deviceId = fetchDeviceId() ?: return@launch
                Log.d("AppMonitorService", "🔄 Loading blocked apps from API for device: $deviceId")
                
                val response = api.getAppControls(deviceId)
                if (response.isSuccessful) {
                    val apiBlocked = response.body()
                        ?.filter { it.controlType == "BLOCKED" }
                        ?.map { it.packageName }
                        ?.toSet() ?: emptySet()
                    
                    Log.d("AppMonitorService", "📡 API returned ${apiBlocked.size} blocked apps: $apiBlocked")
                    
                    // Replace with API data (API is source of truth)
                    blockedPackages = apiBlocked.toMutableSet()
                    
                    // Update SharedPreferences
                    getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
                        .edit()
                        .putStringSet("blocked", blockedPackages)
                        .apply()
                    
                    Log.d("AppMonitorService", "✅ Updated blocked apps: $blockedPackages")
                } else {
                    Log.e("AppMonitorService", "❌ API call failed: ${response.code()}")
                }
            } catch (e: Exception) { 
                Log.e("AppMonitorService", "❌ Failed to load from API", e)
            }
        }
    }

    private fun startMonitoring() {
        monitorJob = lifecycleScope.launch {
            var apiRefreshCounter = 0
            
            while (isActive) {
                try {
                    // Refresh from API every 3 seconds (10 cycles * 0.3s)
                    if (apiRefreshCounter >= 10) {
                        loadBlockedAppsFromApi()
                        apiRefreshCounter = 0
                    }
                    apiRefreshCounter++
                    
                    val foregroundApp = getForegroundApp()
                    
                    // Log app changes
                    if (foregroundApp != lastForegroundApp && foregroundApp != null && foregroundApp != packageName) {
                        Log.d("AppMonitorService", "📱 App changed: $foregroundApp")
                        lastForegroundApp = foregroundApp
                        consecutiveBlockCount = 0 // Reset counter on app change
                    }
                    
                    if (foregroundApp != null && foregroundApp != packageName) {
                        // Check if blocked - ALWAYS block, no exceptions
                        if (foregroundApp in blockedPackages) {
                            consecutiveBlockCount++
                            Log.d("AppMonitorService", "⛔ BLOCKED APP DETECTED: $foregroundApp (attempt #$consecutiveBlockCount)")
                            forceCloseApp(foregroundApp)
                        }
                        
                        // Check if it's a game and notify parent (only once)
                        if (isGameApp(foregroundApp) && foregroundApp !in notifiedGames) {
                            notifiedGames.add(foregroundApp)
                            notifyParentAboutGame(foregroundApp)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AppMonitorService", "Error in monitoring loop", e)
                }
                
                delay(200L) // Check every 0.2 seconds for ultra-fast blocking
            }
        }
    }
    
    private fun forceCloseApp(packageName: String) {
        try {
            Log.d("AppMonitorService", "🚫 FORCE CLOSING: $packageName")
            
            // Method 1: Send to home screen
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
            }
            startActivity(homeIntent)
            
            Log.d("AppMonitorService", "✅ App blocked: $packageName")
        } catch (e: Exception) {
            Log.e("AppMonitorService", "❌ Failed to block app", e)
        }
    }

    private fun getForegroundApp(): String? {
        try {
            // Method 1: UsageStatsManager (requires Usage Stats permission)
            val usm = getSystemService(UsageStatsManager::class.java)
            if (usm != null) {
                val now = System.currentTimeMillis()
                val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5000, now)
                if (stats != null && stats.isNotEmpty()) {
                    val foregroundApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName
                    if (foregroundApp != null) {
                        return foregroundApp
                    }
                }
            }
            
            // Method 2: ActivityManager (fallback, deprecated but works)
            @Suppress("DEPRECATION")
            val am = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val tasks = am.getRunningTasks(1)
            if (tasks.isNotEmpty()) {
                return tasks[0].topActivity?.packageName
            }
        } catch (e: Exception) {
            Log.e("AppMonitorService", "Error getting foreground app", e)
        }
        return null
    }

    private fun isGameApp(packageName: String): Boolean {
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString().lowercase()
            
            // Check if app is in GAME category
            if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                return true
            }
            
            // Check if app name contains game keywords
            return gameKeywords.any { appName.contains(it) }
        } catch (e: Exception) {
            return false
        }
    }

    private fun notifyParentAboutGame(packageName: String) {
        lifecycleScope.launch {
            try {
                val deviceId = fetchDeviceId() ?: return@launch
                val pm = packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                
                // Send alert to backend which will notify parent via FCM
                api.sendCommand(SendCommandRequest(
                    targetDeviceId = deviceId,
                    commandType = "GAME_ALERT",
                    metadata = "Child opened game: $appName ($packageName)"
                ))
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    private fun fetchDeviceId(): Long? {
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val id = prefs.getLong("device_id", -1L)
        return if (id == -1L) null else id
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, GiaApplication.CHANNEL_LOCATION)
            .setContentTitle("Gia Family Control")
            .setContentText("App monitoring active - ${blockedPackages.size} apps blocked")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

    override fun onDestroy() {
        Log.d("AppMonitorService", "Service destroyed, scheduling restart")
        try {
            unregisterReceiver(refreshReceiver)
        } catch (e: Exception) {}
        monitorJob?.cancel()
        
        // Restart service
        val restartIntent = Intent(applicationContext, AppMonitorService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 2, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
        
        super.onDestroy()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("AppMonitorService", "Task removed, restarting service")
        
        val restartIntent = Intent(applicationContext, AppMonitorService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 2, restartIntent, 
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1002
    }
}
