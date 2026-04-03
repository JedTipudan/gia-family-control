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
        }
    }

    private fun lockDevice() {
        // Save lock state
        getSharedPreferences("gia_lock", MODE_PRIVATE)
            .edit().putBoolean("is_locked", true).apply()
        
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
        }
        startActivity(intent)
    }

    private fun unlockDevice() {
        // Clear lock state
        getSharedPreferences("gia_lock", MODE_PRIVATE)
            .edit().putBoolean("is_locked", false).apply()
        LockScreenActivity.dismiss()
    }

    private fun updateAppBlock(packageName: String, block: Boolean) {
        val prefs = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked", mutableSetOf())!!.toMutableSet()
        if (block) blocked.add(packageName) else blocked.remove(packageName)
        prefs.edit().putStringSet("blocked", blocked).apply()
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
        val childDeviceId = data["childDeviceId"]?.toLongOrNull() ?: return
        val deviceName = data["deviceName"] ?: "Child Device"
        val deviceModel = data["deviceModel"] ?: ""
        
        // Save child device ID
        getSharedPreferences("gia_prefs", MODE_PRIVATE)
            .edit()
            .putLong("child_device_id", childDeviceId)
            .apply()
        
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
                .bigText("$deviceName ($deviceModel) has been successfully paired with your account. You can now monitor and control this device."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
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
