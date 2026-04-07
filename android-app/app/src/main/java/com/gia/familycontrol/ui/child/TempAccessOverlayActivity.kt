package com.gia.familycontrol.ui.child

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gia.familycontrol.R
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver
import com.gia.familycontrol.util.SecureAuthManager

class TempAccessOverlayActivity : AppCompatActivity() {

    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temp_access_overlay)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val minutes  = intent.getIntExtra("minutes", 30)
        val untilMs  = System.currentTimeMillis() + minutes * 60_000L
        val tvTitle   = findViewById<TextView>(R.id.tvTempTitle)
        val tvTimer   = findViewById<TextView>(R.id.tvTempTimer)
        val tvMessage = findViewById<TextView>(R.id.tvTempMessage)

        tvTitle.text   = "⏱ Temporary Access"
        tvMessage.text = "Your parent has granted you $minutes minutes of free access.\nDevice will lock automatically when time is up."

        val remaining = untilMs - System.currentTimeMillis()
        if (remaining <= 0) { lockAndFinish(); return }

        timer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(ms: Long) {
                val totalSecs = ms / 1000
                val mins = totalSecs / 60
                val secs = totalSecs % 60
                tvTimer.text = "%02d:%02d".format(mins, secs)
            }
            override fun onFinish() {
                tvTimer.text = "00:00"
                lockAndFinish()
            }
        }.start()

        // OK button — dismiss overlay, timer keeps running via AppMonitorService
        findViewById<android.widget.Button>(R.id.btnTempOk).setOnClickListener {
            finish()
        }
    }

    private fun lockAndFinish() {
        // Revoke temp access
        SecureAuthManager.revokeTemporaryAccess(this)

        // Lock the device
        getSharedPreferences("gia_lock", MODE_PRIVATE)
            .edit().putBoolean("is_locked", true).apply()

        try {
            val dpm = getSystemService(DevicePolicyManager::class.java)
            val admin = ComponentName(this, GiaDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(admin)) dpm.lockNow()
        } catch (_: Exception) {}

        // Show lock screen
        try {
            startActivity(Intent(this, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        } catch (_: Exception) {}

        finish()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    companion object {
        fun launch(context: android.content.Context, minutes: Int) {
            val intent = Intent(context, TempAccessOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("minutes", minutes)
            }
            context.startActivity(intent)
        }
    }
}
