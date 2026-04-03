package com.gia.familycontrol.ui.child

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.gia.familycontrol.databinding.ActivityLockScreenBinding
import kotlinx.coroutines.*

class LockScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLockScreenBinding
    private var monitorJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if still locked
        if (!isLocked()) {
            finish()
            return
        }

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
        
        // Start monitoring to bring back to front if user tries to leave
        startMonitoring()
    }

    private fun isLocked(): Boolean {
        return getSharedPreferences("gia_lock", MODE_PRIVATE)
            .getBoolean("is_locked", false)
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

    override fun onDestroy() {
        monitorJob?.cancel()
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
