package com.gia.familycontrol.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class StatusBarBlockerService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (overlayView == null) addOverlay()
        return START_STICKY // restart if killed
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    private fun addOverlay() {
        if (!android.provider.Settings.canDrawOverlays(this)) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ERROR

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            getStatusBarHeight(),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0; y = 0
        }

        overlayView = View(this).apply { setBackgroundColor(0x00000000) }
        try { windowManager?.addView(overlayView, params) } catch (_: Exception) {}
    }

    private fun getStatusBarHeight(): Int {
        val density = resources.displayMetrics.density
        return (200 * density).toInt()
    }

    override fun onDestroy() {
        try { overlayView?.let { windowManager?.removeView(it) } } catch (_: Exception) {}
        overlayView = null
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            if (!android.provider.Settings.canDrawOverlays(context)) return
            // Use regular startService — no foreground notification needed
            context.startService(Intent(context, StatusBarBlockerService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, StatusBarBlockerService::class.java))
        }
    }
}
