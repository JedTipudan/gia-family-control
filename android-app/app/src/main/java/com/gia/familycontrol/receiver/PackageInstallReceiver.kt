package com.gia.familycontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.model.SendCommandRequest
import com.gia.familycontrol.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageInstallReceiver : BroadcastReceiver() {

    private val gameKeywords = listOf("game", "play", "arcade", "puzzle", "racing", "action", "adventure")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            val packageName = intent.data?.schemeSpecificPart ?: return
            
            // Check if it's a game
            if (isGameApp(context, packageName)) {
                notifyParentAboutNewGame(context, packageName)
            }
        }
    }

    private fun isGameApp(context: Context, packageName: String): Boolean {
        try {
            val pm = context.packageManager
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

    private fun notifyParentAboutNewGame(context: Context, packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
                val deviceId = prefs.getLong("device_id", -1L)
                if (deviceId == -1L) return@launch

                val pm = context.packageManager
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                
                val api = RetrofitClient.create(context)
                api.sendCommand(SendCommandRequest(
                    targetDeviceId = deviceId,
                    commandType = "GAME_INSTALLED",
                    metadata = "Child installed game: $appName ($packageName)"
                ))
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
