package com.gia.familycontrol.ui.child

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
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
        tvMessage.text = "Your parent has granted you $minutes minutes.\nDevice will lock automatically when time is up."

        val remaining = untilMs - System.currentTimeMillis()
        if (remaining <= 0) { lockAndFinish(); return }

        // Schedule alarm to lock at expiry — fires even if overlay is dismissed
        scheduleLockAlarm(untilMs)

        timer = object : CountDownTimer(remaining, 1000) {
            override fun onTick(ms: Long) {
                val totalSecs = ms / 1000
                tvTimer.text = "%02d:%02d".format(totalSecs / 60, totalSecs % 60)
            }
            override fun onFinish() {
                tvTimer.text = "00:00"
                lockAndFinish()
            }
        }.start()

        // OK button — just dismiss the overlay, alarm still fires at expiry
        findViewById<android.widget.Button>(R.id.btnTempOk).setOnClickListener {
            finish() // timer keeps running via alarm + AppMonitorService
        }
    }

    private fun scheduleLockAlarm(untilMs: Long) {
        val intent = Intent(this, TempExpiredReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            this, 9001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(AlarmManager::class.java)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, untilMs, pi)
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, untilMs, pi)
                }
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, untilMs, pi)
            }
        } catch (_: Exception) {
            am.set(AlarmManager.RTC_WAKEUP, untilMs, pi)
        }
    }

    private fun lockAndFinish() {
        SecureAuthManager.revokeTemporaryAccess(this)
        getSharedPreferences("gia_lock", MODE_PRIVATE)
            .edit().putBoolean("is_locked", true).apply()
        try {
            val dpm = getSystemService(DevicePolicyManager::class.java)
            val admin = ComponentName(this, GiaDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(admin)) dpm.lockNow()
        } catch (_: Exception) {}
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
        fun launch(context: Context, minutes: Int) {
            context.startActivity(Intent(context, TempAccessOverlayActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("minutes", minutes)
            })
        }
    }
}

// Broadcast receiver that fires when temp access alarm expires
class TempExpiredReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Only lock if temp access is actually expired (not if parent granted more time)
        if (SecureAuthManager.isTemporaryAccessActive(context)) return

        SecureAuthManager.revokeTemporaryAccess(context)
        context.getSharedPreferences("gia_lock", Context.MODE_PRIVATE)
            .edit().putBoolean("is_locked", true).apply()

        // Lock via Device Admin
        try {
            val dpm = context.getSystemService(DevicePolicyManager::class.java)
            val admin = ComponentName(context, GiaDeviceAdminReceiver::class.java)
            if (dpm.isAdminActive(admin)) dpm.lockNow()
        } catch (_: Exception) {}

        // Show lock screen
        try {
            context.startActivity(Intent(context, LockScreenActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        } catch (_: Exception) {}
    }
}
