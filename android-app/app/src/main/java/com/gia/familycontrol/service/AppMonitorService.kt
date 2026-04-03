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
        Log.d("AppMonitorService", "Loaded ${blockedPackages.size} blocked apps from prefs: $blockedPackages")
    }
    
    private fun loadBlockedAppsFromApi() {
        lifecycleScope.launch {
            try {
                val deviceId = fetchDeviceId() ?: return@launch
                val response = api.getAppControls(deviceId)
                if (response.isSuccessful) {
                    val apiBlocked = response.body()
                        ?.filter { it.controlType == "BLOCKED" }
                        ?.map { it.packageName }
                        ?.toMutableSet() ?: mutableSetOf()
                    
                    // Merge with SharedPreferences
                    blockedPackages.addAll(apiBlocked)
                    
                    // Update SharedPreferences to sync
                    getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
                        .edit()
                        .putStringSet("blocked", blockedPackages)
                        .apply()
                    
                    Log.d("AppMonitorService", "Synced with API. Total blocked: ${blockedPackages.size}")
                }
            } catch (e: Exception) { 
                Log.e("AppMonitorService", "Failed to load from API", e)
            }
        }
    }

    private fun startMonitoring() {
        monitorJob = lifecycleScope.launch {
            var refreshCounter = 0
            while (isActive) {
                // Refresh from API every 30 seconds (reduced from 10 for better performance)
                if (refreshCounter % 30 == 0) {
                    loadBlockedAppsFromApi()
                }
                refreshCounter++
                
                val foregroundApp = getForegroundApp()
                if (foregroundApp != null && foregroundApp != packageName) {
                    // Check if it's a blocked app
                    if (foregroundApp in blockedPackages) {
                        Log.d("AppMonitorService", "Blocked app detected: $foregroundApp - Force closing")
                        forceCloseApp(foregroundApp)
                    }
                    
                    // Check if it's a game and notify parent
                    if (isGameApp(foregroundApp) && foregroundApp !in notifiedGames) {
                        notifiedGames.add(foregroundApp)
                        notifyParentAboutGame(foregroundApp)
                    }
                }
                delay(1000L)
            }
        }
    }
    
    private fun forceCloseApp(packageName: String) {
        try {
            // Show block overlay
            val intent = Intent(this, AppBlockOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("blocked_package", packageName)
            }
            startActivity(intent)
            
            // Send to home screen to close the blocked app
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            
            Log.d("AppMonitorService", "Blocked app closed: $packageName")
        } catch (e: Exception) {
            Log.e("AppMonitorService", "Failed to close blocked app", e)
        }
    }

    private fun getForegroundApp(): String? {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return null
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 5000, now)
        return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
    }

    private fun showBlockOverlay(packageName: String) {
        val intent = Intent(this, AppBlockOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("blocked_package", packageName)
        }
        startActivity(intent)
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

    fun updateBlockedApps(packages: Set<String>) {
        blockedPackages = packages.toMutableSet()
    }

    private fun fetchDeviceId(): Long? {
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val id = prefs.getLong("device_id", -1L)
        return if (id == -1L) null else id
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, GiaApplication.CHANNEL_LOCATION)
            .setContentTitle("Gia Family Control")
            .setContentText("App monitoring active")
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
        android.util.Log.d("AppMonitorService", "Task removed, restarting service")
        
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
