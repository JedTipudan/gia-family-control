package com.gia.familycontrol.service

import android.app.Notification
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
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
    private var lastBlockedApp: String? = null
    private val notifiedGames = mutableSetOf<String>()
    private val gameKeywords = listOf("game", "play", "arcade", "puzzle", "racing", "action", "adventure")

    override fun onCreate() {
        super.onCreate()
        Log.d("AppMonitorService", "=== Service onCreate ===")
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            loadBlockedApps()
            startMonitoring()
            Log.d("AppMonitorService", "✅ Service created successfully")
        } catch (e: Exception) {
            Log.e("AppMonitorService", "❌ Failed to create service", e)
            stopSelf()
        }
    }

    private fun loadBlockedApps() {
        lifecycleScope.launch {
            try {
                val deviceId = fetchDeviceId() ?: return@launch
                val response = api.getAppControls(deviceId)
                if (response.isSuccessful) {
                    blockedPackages = response.body()
                        ?.filter { it.controlType == "BLOCKED" }
                        ?.map { it.packageName }
                        ?.toMutableSet() ?: mutableSetOf()
                }
            } catch (e: Exception) { /* use cached list */ }
        }
    }

    private fun startMonitoring() {
        monitorJob = lifecycleScope.launch {
            while (isActive) {
                val foregroundApp = getForegroundApp()
                if (foregroundApp != null) {
                    // Check if it's a blocked app
                    if (foregroundApp in blockedPackages && foregroundApp != lastBlockedApp) {
                        lastBlockedApp = foregroundApp
                        showBlockOverlay(foregroundApp)
                    } else if (foregroundApp !in blockedPackages) {
                        lastBlockedApp = null
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

    private fun getForegroundApp(): String? {
        val usm = getSystemService(UsageStatsManager::class.java)
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
