package com.gia.familycontrol.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver

/**
 * Manages app hiding via Device Owner API (setApplicationHidden).
 * Completely separate from the blocked-apps auto-exit system.
 * Hidden apps are invisible in the launcher; blocked apps use auto-exit.
 */
object AppHideManager {

    private const val TAG = "AppHideManager"
    private const val PREFS = "gia_hidden_apps"
    private const val KEY_HIDDEN = "hidden"

    // Apps that must NEVER be hidden — system-critical
    private val PROTECTED_PACKAGES = setOf(
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",       // Samsung
        "com.miui.home",                       // Xiaomi
        "com.huawei.android.launcher",         // Huawei
        "com.android.settings",
        "com.android.systemui",
        "com.android.phone",
        "com.android.dialer",
        "com.google.android.dialer"
    )

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun hideApp(context: Context, packageName: String): Boolean {
        if (packageName in PROTECTED_PACKAGES) {
            Log.w(TAG, "Refused to hide protected package: $packageName")
            return false
        }
        if (!isDeviceOwner(context)) {
            Log.w(TAG, "Not device owner — cannot hide apps")
            return false
        }
        return try {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            val admin = ComponentName(context, GiaDeviceAdminReceiver::class.java)
            val result = dpm.setApplicationHidden(admin, packageName, true)
            if (result) persistHidden(context, packageName, true)
            Log.d(TAG, "hideApp $packageName = $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide $packageName", e)
            false
        }
    }

    fun unhideApp(context: Context, packageName: String): Boolean {
        if (!isDeviceOwner(context)) return false
        return try {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            val admin = ComponentName(context, GiaDeviceAdminReceiver::class.java)
            val result = dpm.setApplicationHidden(admin, packageName, false)
            if (result) persistHidden(context, packageName, false)
            Log.d(TAG, "unhideApp $packageName = $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unhide $packageName", e)
            false
        }
    }

    fun isHidden(context: Context, packageName: String): Boolean =
        getHiddenPackages(context).contains(packageName)

    fun getHiddenPackages(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    /** Called on BOOT_COMPLETED to reapply hidden state (Device Owner persists it, but we sync). */
    fun reapplyOnBoot(context: Context) {
        if (!isDeviceOwner(context)) return
        val hidden = getHiddenPackages(context)
        Log.d(TAG, "Reapplying ${hidden.size} hidden apps on boot")
        hidden.forEach { pkg -> hideApp(context, pkg) }
    }

    private fun persistHidden(context: Context, packageName: String, hide: Boolean) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_HIDDEN, mutableSetOf())!!.toMutableSet()
        if (hide) current.add(packageName) else current.remove(packageName)
        prefs.edit().putStringSet(KEY_HIDDEN, current).apply()
    }
}
