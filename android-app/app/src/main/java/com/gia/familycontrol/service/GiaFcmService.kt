package com.gia.familycontrol.service

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.gia.familycontrol.network.DeviceStatusUpdate
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
            "BLOCK_APP" -> {
                val pkg = message.data["packageName"] ?: return
                updateAppBlock(pkg, true)
            }
            "UNBLOCK_APP" -> {
                val pkg = message.data["packageName"] ?: return
                updateAppBlock(pkg, false)
            }
            "EMERGENCY" -> handleEmergency(message.data["message"])
        }
    }

    private fun lockDevice() {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        startActivity(intent)
    }

    private fun unlockDevice() {
        LockScreenActivity.dismiss()
    }

    private fun updateAppBlock(packageName: String, block: Boolean) {
        val prefs = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
        val blocked = prefs.getStringSet("blocked", mutableSetOf())!!.toMutableSet()
        if (block) blocked.add(packageName) else blocked.remove(packageName)
        prefs.edit().putStringSet("blocked", blocked).apply()
    }

    private fun handleEmergency(message: String?) {
        // Show high-priority notification to parent
    }

    override fun onNewToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                api.updateDeviceStatus(
                    com.gia.familycontrol.model.DeviceStatusUpdate(
                        batteryLevel = null,
                        isOnline = true,
                        fcmToken = token
                    )
                )
            } catch (e: Exception) { /* retry on next launch */ }
        }
    }
}
