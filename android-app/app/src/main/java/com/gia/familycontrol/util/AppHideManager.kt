package com.gia.familycontrol.util

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver

object AppHideManager {

    private const val TAG = "AppHideManager"
    private const val PREFS = "gia_hidden_apps"
    private const val KEY_HIDDEN = "hidden"

    // Apps that are TRULY critical and must never be hidden (would break the device)
    private val PROTECTED_PACKAGES = setOf(
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.android.systemui",
        "com.android.phone"
    )

    // Apps automatically hidden from launcher when child protection is enabled
    // These prevent the child from uninstalling apps or changing device settings
    val CHILD_PROTECTION_PACKAGES = setOf(
        "com.android.settings",
        "com.samsung.android.settings",
        "com.miui.securitycenter",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.android.vending",
        "com.sec.android.app.samsungapps"
    )

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(DevicePolicyManager::class.java)
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    /** Hide a specific app from the launcher */
    fun hideApp(context: Context, packageName: String): Boolean {
        if (packageName in PROTECTED_PACKAGES) {
            Log.w(TAG, "Refused to hide protected package: $packageName")
            return false
        }
        // Always persist to prefs (launcher filters by prefs)
        persistHidden(context, packageName, true)
        // Also try Device Owner hide if available
        if (isDeviceOwner(context)) {
            try {
                val dpm = context.getSystemService(DevicePolicyManager::class.java)
                val admin = ComponentName(context, GiaDeviceAdminReceiver::class.java)
                dpm.setApplicationHidden(admin, packageName, true)
            } catch (e: Exception) {
                Log.w(TAG, "Device Owner hide failed for $packageName: ${e.message}")
            }
        }
        Log.d(TAG, "hideApp $packageName")
        return true
    }

    /** Unhide a specific app */
    fun unhideApp(context: Context, packageName: String): Boolean {
        persistHidden(context, packageName, false)
        if (isDeviceOwner(context)) {
            try {
                val dpm = context.getSystemService(DevicePolicyManager::class.java)
                val admin = ComponentName(context, GiaDeviceAdminReceiver::class.java)
                dpm.setApplicationHidden(admin, packageName, false)
            } catch (e: Exception) {
                Log.w(TAG, "Device Owner unhide failed for $packageName: ${e.message}")
            }
        }
        Log.d(TAG, "unhideApp $packageName")
        return true
    }

    /** Hide Settings + Package Installers so child cannot uninstall apps or change settings */
    fun applyChildProtection(context: Context) {
        Log.d(TAG, "Applying child protection - hiding Settings and Package Installer")
        CHILD_PROTECTION_PACKAGES.forEach { pkg ->
            // Only hide if the package is actually installed
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                hideApp(context, pkg)
                Log.d(TAG, "Child protection: hidden $pkg")
            } catch (_: Exception) {
                // Package not installed on this device, skip
            }
        }
    }

    /** Restore Settings visibility */
    fun removeChildProtection(context: Context) {
        Log.d(TAG, "Removing child protection - showing Settings")
        CHILD_PROTECTION_PACKAGES.forEach { pkg -> unhideApp(context, pkg) }
    }

    fun isHidden(context: Context, packageName: String): Boolean =
        getHiddenPackages(context).contains(packageName)

    fun getHiddenPackages(context: Context): Set<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet()

    fun reapplyOnBoot(context: Context) {
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
