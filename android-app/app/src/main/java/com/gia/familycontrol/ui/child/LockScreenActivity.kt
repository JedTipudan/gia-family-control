package com.gia.familycontrol.ui.child

import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver
import com.gia.familycontrol.databinding.ActivityLockScreenBinding
import com.gia.familycontrol.service.StatusBarBlockerService
import kotlinx.coroutines.*

class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding
    private var monitorJob: Job? = null
    private lateinit var dpm: DevicePolicyManager
    private lateinit var adminComponent: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if still locked
        if (!isLocked()) {
            finish()
            return
        }

        dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        adminComponent = ComponentName(this, GiaDeviceAdminReceiver::class.java)

        // Make fullscreen and block everything
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivityLockScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        instance = this

        binding.tvMessage.text = "Device locked by parent"
        binding.tvSubMessage.text = "Contact your parent to unlock"

        // Always block notification panel when device is locked
        StatusBarBlockerService.start(this)
        
        // Start Lock Task Mode (Kiosk Mode)
        startLockTaskMode()
        
        // Start monitoring to bring back to front if user tries to leave
        startMonitoring()
    }

    private fun isLocked(): Boolean {
        // Not locked if device is not paired
        val deviceId = getSharedPreferences("gia_prefs", MODE_PRIVATE)
            .getLong("device_id", -1L)
        if (deviceId == -1L) return false
        val locked = getSharedPreferences("gia_lock", MODE_PRIVATE)
            .getBoolean("is_locked", false)
        if (!locked) return false
        if (com.gia.familycontrol.util.SecureAuthManager.isTemporaryAccessActive(this)) return false
        return true
    }

    private fun startLockTaskMode() {
        try {
            if (dpm.isDeviceOwnerApp(packageName)) {
                // Device Owner - can use lock task mode without user interaction
                dpm.setLockTaskPackages(adminComponent, arrayOf(packageName))
                startLockTask()
                Log.d("LockScreen", "Lock task mode started (Device Owner)")
            } else if (dpm.isAdminActive(adminComponent)) {
                // Device Admin - try to start lock task (may require user approval on some devices)
                try {
                    startLockTask()
                    Log.d("LockScreen", "Lock task mode started (Device Admin)")
                } catch (e: Exception) {
                    Log.w("LockScreen", "Lock task mode not available, using fallback", e)
                }
            } else {
                Log.w("LockScreen", "Device admin not active, using fallback lock")
            }
        } catch (e: Exception) {
            Log.e("LockScreen", "Failed to start lock task mode", e)
        }
    }

    // Block all hardware back/home key presses
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return true // Block ALL keys
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        // Block all key events including home button
        return true
    }

    private fun startMonitoring() {
        monitorJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(300)
                // Check if still locked
                if (!isLocked()) {
                    stopLockTaskModeIfNeeded()
                    finish()
                    return@launch
                }
                // Bring to front if not on top
                if (!isTaskRoot) {
                    moveTaskToFront()
                }
            }
        }
    }

    private fun moveTaskToFront() {
        val intent = Intent(this, LockScreenActivity::class.java).apply {
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or
                    android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (!isLocked()) {
            stopLockTaskModeIfNeeded()
            finish()
            return
        }
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
    }

    override fun onPause() {
        super.onPause()
        // Immediately bring back to front if still locked
        if (isLocked()) {
            moveTaskToFront()
        }
    }

    override fun onStop() {
        super.onStop()
        // Restart if still locked
        if (isLocked()) {
            moveTaskToFront()
        }
    }

    override fun onBackPressed() {
        // Do nothing — prevent exit
    }

    private fun stopLockTaskModeIfNeeded() {
        try {
            if (dpm.isDeviceOwnerApp(packageName) || dpm.isAdminActive(adminComponent)) {
                stopLockTask()
                Log.d("LockScreen", "Lock task mode stopped")
            }
        } catch (e: Exception) {
            Log.e("LockScreen", "Failed to stop lock task mode", e)
        }
        // Stop status bar blocker only if notifications are not explicitly blocked by parent
        val notifBlocked = getSharedPreferences("gia_prefs", MODE_PRIVATE)
            .getBoolean("notifications_blocked", false)
        if (!notifBlocked) {
            StatusBarBlockerService.stop(this)
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        stopLockTaskModeIfNeeded()
        if (instance == this) instance = null
        super.onDestroy()
    }

    companion object {
        private var instance: LockScreenActivity? = null

        fun dismiss() {
            instance?.finish()
            instance = null
        }
    }
}
