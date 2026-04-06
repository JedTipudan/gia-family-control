package com.gia.parentcontrol.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gia.parentcontrol.ParentApplication
import com.gia.parentcontrol.R
import com.gia.parentcontrol.model.DeviceStatusUpdate
import com.gia.parentcontrol.network.RetrofitClient
import com.gia.parentcontrol.ui.dashboard.ParentDashboardActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ParentFcmService : FirebaseMessagingService() {

    private val api by lazy { RetrofitClient.create(this) }

    override fun onMessageReceived(message: RemoteMessage) {
        val command = message.data["command"] ?: return
        when (command) {
            "CHILD_PAIRED" -> handleChildPaired(message.data)
            "SOS"          -> handleSos(message.data)
            "GAME_ALERT"   -> showAlert("🎮 Game Opened", "Child opened: ${message.data["appName"]}")
            "GAME_INSTALLED" -> showAlert("📥 Game Installed", "Child installed: ${message.data["appName"]}")
            "NETWORK_CONNECTED" -> showAlert("📶 Child Online", "Connected via ${message.data["connectionType"]}")
            "NETWORK_DISCONNECTED" -> showAlert("📵 Child Offline", "Child disconnected from internet")
        }
    }

    private fun handleChildPaired(data: Map<String, String>) {
        val deviceId = data["childDeviceId"]?.toLongOrNull() ?: return
        val name = data["deviceName"] ?: "Child Device"
        getSharedPreferences("parent_prefs", MODE_PRIVATE)
            .edit().putLong("child_device_id", deviceId).apply()
        showAlert("✅ Child Device Paired", "$name is now connected")
    }

    private fun handleSos(data: Map<String, String>) {
        val childName = data["childName"] ?: "Your child"
        val location = data["location"] ?: "Unknown"
        val nm = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = android.app.NotificationChannel(
                "sos_channel", "SOS Emergency", NotificationManager.IMPORTANCE_MAX
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setBypassDnd(true)
            }
            nm.createNotificationChannel(ch)
        }

        val intent = Intent(this, ParentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(this, "sos_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🆘 EMERGENCY SOS")
            .setContentText("$childName needs help!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$childName sent an SOS!\nLocation: $location"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        nm.notify(7777, notif)
    }

    private fun showAlert(title: String, text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        val intent = Intent(this, ParentDashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notif = NotificationCompat.Builder(this, ParentApplication.CHANNEL_ALERTS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.updateDeviceStatus(DeviceStatusUpdate(null, true, token, null))
            } catch (_: Exception) {}
        }
    }
}
