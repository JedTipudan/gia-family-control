package com.gia.familycontrol.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.gia.familycontrol.GiaApplication
import com.gia.familycontrol.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gia.familycontrol.model.DeviceStatusUpdate
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.ui.child.LockScreenActivity
import com.gia.familycontrol.util.AppHideManager
import com.gia.familycontrol.util.SecureAuthManager
import com.gia.familycontrol.util.ActionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GiaFcmService : FirebaseMessagingService() {

    private val api by lazy { RetrofitClient.create(this) }

    // Safe pending intent — opens the launcher (child app entry point)
    private fun getMainPendingIntent(): PendingIntent {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val command = message.data["command"] ?: return

        when (command) {
            "LOCK"    -> lockDevice()
            "UNLOCK"  -> unlockDevice()
            "UNPAIR"  -> unpairDevice()
            "CHILD_PAIRED" -> handleChildPaired(message.data)
            "BLOCK_APP" -> {
                val pkg = message.data["packageName"] ?: return
                updateAppBlock(pkg, true)
            }
            "UNBLOCK_APP" -> {
                val pkg = message.data["packageName"] ?: return
                updateAppBlock(pkg, false)
            }
            "HIDE_APP" -> {
                val pkg = message.data["packageName"] ?: return
                val prefs = getSharedPreferences("gia_hidden_apps", MODE_PRIVATE)
                val hidden = prefs.getStringSet("hidden", mutableSetOf())!!.toMutableSet()
                hidden.add(pkg)
                prefs.edit().putStringSet("hidden", hidden).apply()
                AppHideManager.hideApp(this, pkg)
                ActionLogger.log(this, "HIDE_APP", pkg)
            }
            "UNHIDE_APP" -> {
                val pkg = message.data["packageName"] ?: return
                val prefs = getSharedPreferences("gia_hidden_apps", MODE_PRIVATE)
                val hidden = prefs.getStringSet("hidden", mutableSetOf())!!.toMutableSet()
                hidden.remove(pkg)
                prefs.edit().putStringSet("hidden", hidden).apply()
                AppHideManager.unhideApp(this, pkg)
                ActionLogger.log(this, "UNHIDE_APP", pkg)
            }
            "GRANT_TEMP_ACCESS" -> {
                val minutes = message.data["minutes"]?.toIntOrNull()
                    ?: message.data["metadata"]?.toIntOrNull()
                    ?: 30
                SecureAuthManager.grantTemporaryAccess(this, minutes)
                ActionLogger.log(this, "GRANT_TEMP_ACCESS", "${minutes}min")
                com.gia.familycontrol.ui.child.TempAccessOverlayActivity.launch(this, minutes)
            }
            "REVOKE_TEMP_ACCESS" -> {
                SecureAuthManager.revokeTemporaryAccess(this)
                ActionLogger.log(this, "REVOKE_TEMP_ACCESS")
            }
            "SET_PIN" -> {
                val pin = message.data["pin"]
                    ?: message.data["metadata"]
                    ?: message.data["packageName"]
                    ?: return
                SecureAuthManager.setPin(this, pin)
                ActionLogger.log(this, "SET_PIN")
            }
            "ENABLE_LAUNCHER" -> {
                getSharedPreferences("gia_prefs", MODE_PRIVATE)
                    .edit().putBoolean("launcher_mode", true).apply()
                ActionLogger.log(this, "ENABLE_LAUNCHER")
            }
            "DISABLE_LAUNCHER" -> {
                getSharedPreferences("gia_prefs", MODE_PRIVATE)
                    .edit().putBoolean("launcher_mode", false).apply()
                ActionLogger.log(this, "DISABLE_LAUNCHER")
            }
            "GAME_ALERT"    -> showGameNotification(message.data["appName"] ?: "Unknown", "opened")
            "GAME_INSTALLED"-> showGameNotification(message.data["appName"] ?: "Unknown", "installed")
            "NETWORK_CONNECTED"    -> showNetworkNotification(true,  message.data["connectionType"] ?: "")
            "NETWORK_DISCONNECTED" -> showNetworkNotification(false, "")
            "EMERGENCY" -> { /* no-op */ }
            "SOS"       -> handleSosAlert(message.data)
        }
    }

    private fun handleSosAlert(data: Map<String, String>) {
        val childName = data["childName"] ?: "Your child"
        val location  = data["location"]  ?: "Unknown location"
        val nm = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(
                "sos_alerts", "SOS Emergency Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            })
        }

        val notification = NotificationCompat.Builder(this, "sos_alerts")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🆘 EMERGENCY SOS ALERT")
            .setContentText("$childName needs help!")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$childName sent an SOS!\n\nLocation: $location"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false).setOngoing(true)
            .setContentIntent(getMainPendingIntent())
            .setFullScreenIntent(getMainPendingIntent(), true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
            .build()

        nm.notify(7777, notification)

        try {
            val ringtone = android.media.RingtoneManager.getRingtone(this,
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM))
            ringtone.play()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ ringtone.stop() }, 10000)
        } catch (_: Exception) {}
    }

    private fun lockDevice() {
        ActionLogger.log(this, "LOCK")
        getSharedPreferences("gia_lock", MODE_PRIVATE).edit().putBoolean("is_locked", true).apply()
        try {
            val dpm = getSystemService(android.app.admin.DevicePolicyManager::class.java)
            val admin = android.content.ComponentName(this, com.gia.familycontrol.admin.GiaDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(admin)) dpm.lockNow()
        } catch (_: Exception) {}

        try {
            startActivity(Intent(this, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            })
        } catch (_: Exception) {}

        val svc = Intent(this, LockMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(svc) else startService(svc)

        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel("device_lock", "Device Lock", NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null) })
        nm.notify(8888, NotificationCompat.Builder(this, "device_lock")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔒 Device Locked")
            .setContentText("This device is locked by your parent")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true).setAutoCancel(false).build())
    }

    private fun unlockDevice() {
        ActionLogger.log(this, "UNLOCK")
        getSharedPreferences("gia_lock", MODE_PRIVATE).edit().putBoolean("is_locked", false).apply()
        LockScreenActivity.dismiss()
        stopService(Intent(this, LockMonitorService::class.java))
        getSystemService(NotificationManager::class.java).cancel(8888)
    }

    private fun updateAppBlock(packageName: String, block: Boolean) {
        val prefs   = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked", mutableSetOf())!!.toMutableSet()
        if (block) blocked.add(packageName) else blocked.remove(packageName)
        prefs.edit().putStringSet("blocked", blocked).apply()
        sendBroadcast(Intent("com.gia.familycontrol.REFRESH_BLOCKED_APPS"))
        if (block) {
            startActivity(Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            })
        }
    }

    private fun unpairDevice() {
        getSharedPreferences("gia_prefs", MODE_PRIVATE).edit().remove("device_id").apply()
        stopService(Intent(this, LocationTrackingService::class.java))
        stopService(Intent(this, AppMonitorService::class.java))
        getSystemService(NotificationManager::class.java).notify(9999,
            NotificationCompat.Builder(this, GiaApplication.CHANNEL_COMMANDS)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Device Unpaired")
                .setContentText("Your parent has unpaired this device.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true).build())
    }

    private fun showGameNotification(appName: String, action: String) {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel("game_alerts", "Game Alerts", NotificationManager.IMPORTANCE_HIGH))
        val title = if (action == "installed") "📥 Game Installed" else "🎮 Game Opened"
        val text  = if (action == "installed") "Your child installed: $appName" else "Your child opened: $appName"
        nm.notify(System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, "game_alerts")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title).setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(getMainPendingIntent()).build())
    }

    private fun showNetworkNotification(isConnected: Boolean, connectionType: String) {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel("network_alerts", "Network Alerts", NotificationManager.IMPORTANCE_DEFAULT))
        val title = if (isConnected) "📶 Child Connected" else "📵 Child Disconnected"
        val text  = if (isConnected) "Connected to $connectionType" else "Disconnected from internet"
        nm.notify(System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, "network_alerts")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title).setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(getMainPendingIntent()).build())
    }

    private fun handleChildPaired(data: Map<String, String>) {
        val childDeviceId = data["childDeviceId"]?.toLongOrNull() ?: return
        val deviceName    = data["deviceName"] ?: "Child Device"
        val deviceModel   = data["deviceModel"] ?: ""
        getSharedPreferences("gia_prefs", MODE_PRIVATE).edit().putLong("child_device_id", childDeviceId).apply()
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            nm.createNotificationChannel(NotificationChannel("pairing_alerts", "Pairing Alerts", NotificationManager.IMPORTANCE_HIGH))
        nm.notify(System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(this, "pairing_alerts")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("✅ Child Device Paired")
                .setContentText("$deviceName ($deviceModel) is now connected")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(getMainPendingIntent()).build())
    }

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.updateDeviceStatus(DeviceStatusUpdate(null, true, token, null))
            } catch (_: Exception) {}
        }
    }
}
