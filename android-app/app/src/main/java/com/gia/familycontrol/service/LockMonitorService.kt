package com.gia.familycontrol.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gia.familycontrol.GiaApplication
import com.gia.familycontrol.R
import com.gia.familycontrol.ui.child.ChildDashboardActivity
import com.gia.familycontrol.ui.child.LockScreenActivity
import kotlinx.coroutines.*

class LockMonitorService : Service() {

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("LockMonitorService", "=== Service onCreate ===")
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
            startMonitoring()
            android.util.Log.d("LockMonitorService", "✅ Service created successfully")
        } catch (e: Exception) {
            android.util.Log.e("LockMonitorService", "❌ Failed to create service", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startMonitoring() {
        monitorJob = scope.launch {
            while (isActive) {
                delay(500) // Check every 0.5 seconds for faster response
                
                val lockPrefs = getSharedPreferences("gia_lock", MODE_PRIVATE)
                val isLocked = lockPrefs.getBoolean("is_locked", false)
                
                if (isLocked) {
                    android.util.Log.d("LockMonitorService", "Device should be locked, showing lock screen")
                    // Device should be locked, ensure lock screen is showing
                    val lockIntent = Intent(this@LockMonitorService, LockScreenActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    }
                    try {
                        startActivity(lockIntent)
                    } catch (e: Exception) {
                        android.util.Log.e("LockMonitorService", "Failed to show lock screen", e)
                    }
                }
            }
        }
    }

    private fun buildNotification(): Notification {
        val intent = PendingIntent.getActivity(
            this, 0,
            Intent(this, ChildDashboardActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, GiaApplication.CHANNEL_LOCATION)
            .setContentTitle("Gia Family Control")
            .setContentText("Lock monitoring active")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(intent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        android.util.Log.d("LockMonitorService", "Task removed, restarting service")
        
        val restartIntent = Intent(applicationContext, LockMonitorService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 3, restartIntent, 
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1003
    }
}
