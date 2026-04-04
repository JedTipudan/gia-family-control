package com.gia.familycontrol.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.gia.familycontrol.GiaApplication
import com.gia.familycontrol.R
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver
import com.gia.familycontrol.ui.child.ChildDashboardActivity
import com.gia.familycontrol.ui.child.LockScreenActivity
import kotlinx.coroutines.*

class LockMonitorService : Service() {

    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName
    private var lastLockTime = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    // Screen unlocked, check if device should be locked
                    val lockPrefs = getSharedPreferences("gia_lock", MODE_PRIVATE)
                    val isLocked = lockPrefs.getBoolean("is_locked", false)
                    
                    if (isLocked) {
                        android.util.Log.d("LockMonitorService", "Screen unlocked but device should be locked")
                        // Show lock overlay
                        showLockOverlay()
                        
                        // Lock again after 2 seconds
                        scope.launch {
                            delay(2000)
                            if (lockPrefs.getBoolean("is_locked", false)) {
                                lockDeviceNow()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("LockMonitorService", "=== Service onCreate ===")
        try {
            dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            adminComponent = ComponentName(this, GiaDeviceAdminReceiver::class.java)
            
            startForeground(NOTIFICATION_ID, buildNotification())
            
            // Register screen receiver
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenReceiver, filter)
            
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
                delay(1000) // Check every 1 second
                
                val lockPrefs = getSharedPreferences("gia_lock", MODE_PRIVATE)
                val isLocked = lockPrefs.getBoolean("is_locked", false)
                
                if (isLocked) {
                    // Continuously show lock screen and lock device
                    showLockOverlay()
                    lockDeviceNow()
                }
            }
        }
    }
    
    private fun lockDeviceNow() {
        val now = System.currentTimeMillis()
        if (now - lastLockTime < 2000) return // Don't lock more than once per 2 seconds
        lastLockTime = now
        
        try {
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                android.util.Log.d("LockMonitorService", "Device locked")
            }
        } catch (e: Exception) {
            android.util.Log.e("LockMonitorService", "Failed to lock device", e)
        }
    }
    
    private fun showLockOverlay() {
        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        try {
            startActivity(lockIntent)
        } catch (e: Exception) {
            android.util.Log.e("LockMonitorService", "Failed to show lock overlay", e)
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
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: Exception) {}
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
