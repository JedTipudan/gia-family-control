package com.gia.parentcontrol.util

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import com.gia.parentcontrol.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object UpdateChecker {

    private const val GITHUB_API = "https://api.github.com/repos/JedTipudan/gia-family-control/releases/latest"
    private const val DOWNLOAD_URL = "https://github.com/JedTipudan/gia-family-control/releases/latest"
    private const val CHANNEL_ID = "update_channel"

    fun check(activity: Activity, currentVersionCode: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(GITHUB_API).readText()
                val obj = JSONObject(json)
                val tagName = obj.getString("tag_name")
                val latestCode = tagName.removePrefix("v").toIntOrNull() ?: return@launch
                val releaseNotes = obj.optString("body", "Bug fixes and improvements.")

                if (latestCode > currentVersionCode) {
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(activity, tagName, releaseNotes)
                    }
                    showUpdateNotification(activity, tagName)
                }
            } catch (_: Exception) {}
        }
    }

    private fun showUpdateDialog(activity: Activity, version: String, notes: String) {
        if (activity.isFinishing || activity.isDestroyed) return
        AlertDialog.Builder(activity)
            .setTitle("🆕 Update Available — $version")
            .setMessage("What's new:\n\n$notes\n\nUpdate now to get the latest features and fixes.")
            .setPositiveButton("Download Update") { _, _ ->
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL)))
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showUpdateNotification(context: Context, version: String) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "App Updates", NotificationManager.IMPORTANCE_DEFAULT
            ))
        }
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_URL)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        nm.notify(5555, NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🆕 Update Available — $version")
            .setContentText("Tap to download the latest version of Gia Family Control.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .build())
    }
}
