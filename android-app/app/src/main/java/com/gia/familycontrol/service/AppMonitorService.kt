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
    
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("AppMonitorService", "Received refresh broadcast")
            loadBlockedAppsFromPrefs()
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AppMonitorService", "=== Service onCreate ===")
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            
            // Register broadcast receiver
            val filter = IntentFilter("com.gia.familycontrol.REFRESH_BLOCKED_APPS")
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
            var refreshCounter = 0
            var lastBlockedApp: String? = null
            var lastBlockTime = 0L
            
            while (isActive) {
                // Refresh from API every 5 seconds
                if (refreshCounter % 17 == 0) { // 17 * 0.3s = ~5s
                    loadBlockedAppsFromApi()
                }
                refreshCounter++
                
                val foregroundApp = getForegroundApp()
                val now = System.currentTimeMillis()
                
                // Only log when app changes
                if (foregroundApp != lastForegroundApp && foregroundApp != null && foregroundApp != packageName) {
                    Log.d("AppMonitorService", "📱 Current app: $foregroundApp")
                    lastForegroundApp = foregroundApp
                }
                
                if (foregroundApp != null && foregroundApp != packageName) {
                    // Check if it's a blocked app - ALWAYS block, no debouncing
                    if (foregroundApp in blockedPackages) {
                        // Block immediately every time, even if same app
                        if (foregroundApp != lastBlockedApp || now - lastBlockTime > 500) {
                            Log.d("AppMonitorService", "⛔ BLOCKED APP DETECTED: $foregroundApp - FORCE CLOSING")
                            forceCloseApp(foregroundApp)
                            lastBlockedApp = foregroundApp
                            lastBlockTime = now
                        }
                    } else {
                        // Reset tracking when user switches to allowed app
                        if (lastBlockedApp != null && foregroundApp != lastBlockedApp) {
                            lastBlockedApp = null
                        }
                    }
                    
                    // Check if it's a game and notify parent (only once)
                    if (isGameApp(foregroundApp) && foregroundApp !in notifiedGames) {
                        notifiedGames.add(foregroundApp)
                        notifyParentAboutGame(foregroundApp)
                    }
                }
                delay(300L) // Check every 0.3 seconds
            }
        }
    }
    
    private fun forceCloseApp(packageName: String) {
        try {
            Log.d("AppMonitorService", "🚫 Force closing: $packageName")
            
            // IMMEDIATELY send to home screen - no delay
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            }
            startActivity(homeIntent)
            
            Log.d("AppMonitorService", "✅ Sent to home, blocked app closed: $packageName")
        } catch (e: Exception) {
            Log.e("AppMonitorService", "❌ Failed to close blocked app", e)
        }
    }

    private fun getForegroundApp(): String? {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 3000, now)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
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
        try {
            unregisterReceiver(refreshReceiver)
        } catch (e: Exception) {}
        monitorJob?.cancel()
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
