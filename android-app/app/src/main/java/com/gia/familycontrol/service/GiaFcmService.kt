package com.gia.familycontrol.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gia.familycontrol.GiaApplication
import com.gia.familycontrol.R
import com.gia.familycontrol.ui.parent.ParentDashboardActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gia.familycontrol.model.DeviceStatusUpdate
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.ui.child.LockScreenActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GiaFcmService : FirebaseMessagingService() {

    private val api by lazy { RetrofitClient.create(this) }

    override fun onMessageReceived(message: RemoteMessage) {
        val command = message.data["command"] ?: return

        when (command) {
            "LOCK" -> lockDevice()
            "UNLOCK" -> unlockDevice()
            "UNPAIR" -> unpairDevice()
            "CHILD_PAIRED" -> handleChildPaired(message.data)
            "BLOCK_APP" -> {
                val pkg = message.data["packageName"] ?: return
                updateAppBlock(pkg, true)
            }
            "UNBLOCK_APP" -> {
                val pkg = message.data["packageName"] ?: return
                updateAppBlock(pkg, false)
            }
            "GAME_ALERT" -> {
                val appName = message.data["appName"] ?: "Unknown Game"
                showGameNotification(appName, "opened")
            }
            "GAME_INSTALLED" -> {
                val appName = message.data["appName"] ?: "Unknown Game"
                showGameNotification(appName, "installed")
            }
            "NETWORK_CONNECTED" -> {
                val connectionType = message.data["connectionType"] ?: "Internet"
                showNetworkNotification(true, connectionType)
            }
            "NETWORK_DISCONNECTED" -> {
                showNetworkNotification(false, "")
            }
            "EMERGENCY" -> handleEmergency(message.data["message"])
            "SOS" -> handleSosAlert(message.data)
        }
    }
    
    private fun handleSosAlert(data: Map<String, String>) {
        val childName = data["childName"] ?: "Your child"
        val location = data["location"] ?: "Unknown location"
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // Create high priority channel for SOS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sos_alerts",
                "SOS Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency alerts from your child"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create intent to open parent dashboard
        val intent = Intent(this, ParentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("show_location", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build urgent notification
        val notification = NotificationCompat.Builder(this, "sos_alerts")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🆘 EMERGENCY SOS ALERT")
            .setContentText("$childName needs help!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("🆘 EMERGENCY SOS ALERT\n\n$childName has sent an SOS alert!\n\nLocation: $location\n\nTap to view their location immediately."))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM))
            .setFullScreenIntent(pendingIntent, true)
            .build()
        
        notificationManager.notify(7777, notification)
        
        // Play alarm sound
        try {
            val ringtone = android.media.RingtoneManager.getRingtone(
                this,
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
            )
            ringtone.play()
            
            // Stop after 10 seconds
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                ringtone.stop()
            }, 10000)
        } catch (e: Exception) {
            android.util.Log.e("GiaFcmService", "Failed to play alarm", e)
        }
        
        // Vibrate
        try {
            val vibrator = getSystemService(android.os.Vibrator::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createWaveform(
                        longArrayOf(0, 1000, 500, 1000, 500, 1000),
                        -1
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000), -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("GiaFcmService", "Failed to vibrate", e)
        }
        }
    }

    private fun lockDevice() {
        android.util.Log.d("GiaFcmService", "LOCK command received")
        
        // Save lock state
        getSharedPreferences("gia_lock", MODE_PRIVATE)
            .edit().putBoolean("is_locked", true).apply()
        
        android.util.Log.d("GiaFcmService", "Lock state saved")
        
        // Immediately lock device using Device Admin
        try {
            val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
            val adminComponent = android.content.ComponentName(this, com.gia.familycontrol.admin.GiaDeviceAdminReceiver::class.java)
            
            if (dpm.isAdminActive(adminComponent)) {
                dpm.lockNow()
                android.util.Log.d("GiaFcmService", "Device locked using Device Admin")
            } else {
                android.util.Log.e("GiaFcmService", "Device Admin not active")
            }
        } catch (e: Exception) {
            android.util.Log.e("GiaFcmService", "Failed to lock device", e)
        }
        
        // Show lock overlay
        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        
        try {
            startActivity(lockIntent)
        } catch (e: Exception) {
            android.util.Log.e("GiaFcmService", "Failed to show lock screen", e)
        }
        
        // Ensure LockMonitorService is running
        val serviceIntent = Intent(this, LockMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        
        // Show notification
        showLockNotification()
    }
    
    private fun showLockNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "device_lock",
                "Device Lock",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when device is locked by parent"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(this, "device_lock")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔒 Device Locked")
            .setContentText("This device is locked by your parent")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("This device has been locked by your parent. You cannot use it until they unlock it."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        notificationManager.notify(8888, notification)
    }

    private fun unlockDevice() {
        // Clear lock state
        getSharedPreferences("gia_lock", MODE_PRIVATE)
            .edit().putBoolean("is_locked", false).apply()
        LockScreenActivity.dismiss()
        
        // Stop LockMonitorService
        stopService(Intent(this, LockMonitorService::class.java))
        
        // Cancel lock notification
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.cancel(8888)
    }

    private fun updateAppBlock(packageName: String, block: Boolean) {
        android.util.Log.d("GiaFcmService", "App block update: $packageName = $block")
        
        // Update SharedPreferences for immediate effect
        val prefs = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked", mutableSetOf())!!.toMutableSet()
        if (block) {
            blocked.add(packageName)
            android.util.Log.d("GiaFcmService", "Added $packageName to blocked list")
        } else {
            blocked.remove(packageName)
            android.util.Log.d("GiaFcmService", "Removed $packageName from blocked list")
        }
        prefs.edit().putStringSet("blocked", blocked).apply()
        
        // Notify AppMonitorService to refresh immediately
        val intent = Intent("com.gia.familycontrol.REFRESH_BLOCKED_APPS")
        sendBroadcast(intent)
        
        // If blocking, check if app is currently running and close it
        if (block) {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            android.util.Log.d("GiaFcmService", "Sent to home screen to close $packageName")
        }
    }

    private fun unpairDevice() {
        // Clear pairing data
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        prefs.edit()
            .remove("device_id")
            .apply()
        
        // Stop all services
        stopService(Intent(this, LocationTrackingService::class.java))
        stopService(Intent(this, AppMonitorService::class.java))
        
        // Show notification
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, GiaApplication.CHANNEL_COMMANDS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Device Unpaired")
            .setContentText("Your parent has unpaired this device. You can now uninstall the app.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(9999, notification)
    }

    private fun showGameNotification(appName: String, action: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "game_alerts",
                "Game Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when child opens or installs games"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open parent dashboard
        val intent = Intent(this, ParentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (action == "installed") "📥 Game Installed" else "🎮 Game Opened"
        val text = if (action == "installed") 
            "Your child installed: $appName" 
        else 
            "Your child opened: $appName"
        val bigText = if (action == "installed")
            "Your child just installed the game: $appName. Tap to manage apps."
        else
            "Your child just opened the game: $appName. Tap to view dashboard."

        // Build notification
        val notification = NotificationCompat.Builder(this, "game_alerts")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun showNetworkNotification(isConnected: Boolean, connectionType: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "network_alerts",
                "Network Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when child connects/disconnects from internet"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, ParentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isConnected) "📶 Child Connected" else "📵 Child Disconnected"
        val text = if (isConnected) 
            "Your child connected to $connectionType" 
        else 
            "Your child disconnected from internet"

        val notification = NotificationCompat.Builder(this, "network_alerts")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun handleEmergency(message: String?) {
        // Show high-priority notification to parent
    }
    
    private fun handleChildPaired(data: Map<String, String>) {
        try {
            val childDeviceId = data["childDeviceId"]?.toLongOrNull()
            val deviceName = data["deviceName"] ?: "Child Device"
            val deviceModel = data["deviceModel"] ?: ""
            
            android.util.Log.d("GiaFcmService", "CHILD_PAIRED received: deviceId=$childDeviceId, name=$deviceName")
            
            if (childDeviceId == null) {
                android.util.Log.e("GiaFcmService", "Invalid child device ID")
                return
            }
            
            // Save child device ID
            getSharedPreferences("gia_prefs", MODE_PRIVATE)
                .edit()
                .putLong("child_device_id", childDeviceId)
                .apply()
            
            android.util.Log.d("GiaFcmService", "Child device ID saved: $childDeviceId")
            
            // Show notification
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    "pairing_alerts",
                    "Pairing Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when child device pairs"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            val intent = Intent(this, ParentDashboardActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(this, "pairing_alerts")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("✅ Child Device Paired")
                .setContentText("$deviceName ($deviceModel) is now connected")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$deviceName ($deviceModel) has been successfully paired with your account. Tap to view dashboard."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 500, 200, 500))
                .build()
            
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            android.util.Log.d("GiaFcmService", "Pairing notification shown")
        } catch (e: Exception) {
            android.util.Log.e("GiaFcmService", "Error handling CHILD_PAIRED", e)
        }
    }

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.updateDeviceStatus(
                    DeviceStatusUpdate(
                        batteryLevel = null,
                        isOnline = true,
                        fcmToken = token
                    )
                )
            } catch (e: Exception) { /* retry on next launch */ }
        }
    }
}
