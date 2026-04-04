package com.gia.familycontrol.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.GiaApplication
import com.gia.familycontrol.R
import com.gia.familycontrol.model.DeviceStatusRequest
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.ui.child.ChildDashboardActivity
import kotlinx.coroutines.launch

class DeviceStatusService : LifecycleService() {

    private val api by lazy { RetrofitClient.create(this) }
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 10000L // 10 seconds

    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            updateDeviceStatus()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("DeviceStatusService", "Service created")
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("DeviceStatusService", "Service started")
        handler.post(statusUpdateRunnable)
        return START_STICKY
    }

    private fun updateDeviceStatus() {
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val deviceId = prefs.getLong("device_id", -1L)
        
        if (deviceId == -1L) {
            Log.w("DeviceStatusService", "Device not paired, skipping status update")
            return
        }

        val batteryLevel = getBatteryLevel()
        val isCharging = isCharging()
        val isWifiConnected = isWifiConnected()
        val isOnline = isOnline()

        Log.d("DeviceStatusService", "Battery: $batteryLevel%, Charging: $isCharging, WiFi: $isWifiConnected, Online: $isOnline")

        lifecycleScope.launch {
            try {
                val response = api.updateDeviceStatus(DeviceStatusRequest(
                    batteryLevel = batteryLevel,
                    isCharging = isCharging,
                    isWifiConnected = isWifiConnected,
                    isOnline = isOnline
                ))
                if (response.isSuccessful) {
                    Log.d("DeviceStatusService", "Status updated successfully")
                } else {
                    Log.e("DeviceStatusService", "Status update failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("DeviceStatusService", "Failed to update status", e)
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(BatteryManager::class.java)
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val bm = getSystemService(BatteryManager::class.java)
        return bm.isCharging
    }

    private fun isWifiConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun buildNotification(): Notification {
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, ChildDashboardActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, GiaApplication.CHANNEL_LOCATION)
            .setContentTitle("Gia Family Control")
            .setContentText("Device monitoring active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(intent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        Log.d("DeviceStatusService", "Service destroyed")
        handler.removeCallbacks(statusUpdateRunnable)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartIntent = Intent(applicationContext, DeviceStatusService::class.java)
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
        const val NOTIFICATION_ID = 1004
    }
}
