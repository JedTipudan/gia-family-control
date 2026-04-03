package com.gia.familycontrol.service

import android.app.Notification
import android.app.usage.UsageStatsManager
import android.content.Intent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.GiaApplication
import com.gia.familycontrol.R
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.ui.child.AppBlockOverlayActivity
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

class AppMonitorService : LifecycleService() {

    private val api by lazy { RetrofitClient.create(this) }
    private var blockedPackages = mutableSetOf<String>()
    private var monitorJob: Job? = null
    private var lastBlockedApp: String? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
        loadBlockedApps()
        startMonitoring()
    }

    private fun loadBlockedApps() {
        lifecycleScope.launch {
            try {
                val deviceId = getDeviceId() ?: return@launch
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
                if (foregroundApp != null && foregroundApp in blockedPackages
                    && foregroundApp != lastBlockedApp) {
                    lastBlockedApp = foregroundApp
                    showBlockOverlay(foregroundApp)
                } else if (foregroundApp !in blockedPackages) {
                    lastBlockedApp = null
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

    fun updateBlockedApps(packages: Set<String>) {
        blockedPackages = packages.toMutableSet()
    }

    private fun getDeviceId(): Long? {
        val prefs = getSharedPreferences("gia_device", MODE_PRIVATE)
        val id = prefs.getLong("device_id", -1L)
        return if (id == -1L) null else id
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, GiaApplication.CHANNEL_LOCATION)
            .setContentTitle("Gia Family Control")
            .setContentText("App monitoring active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDestroy() {
        monitorJob?.cancel()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1002
    }
}
