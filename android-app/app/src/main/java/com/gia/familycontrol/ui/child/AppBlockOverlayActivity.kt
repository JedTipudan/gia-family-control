package com.gia.familycontrol.ui.child

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.gia.familycontrol.databinding.ActivityAppBlockOverlayBinding

class AppBlockOverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppBlockOverlayBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )
        binding = ActivityAppBlockOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val blockedPkg = intent.getStringExtra("blocked_package") ?: "this app"
        // Show app name if possible
        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(blockedPkg, 0)
            ).toString()
        } catch (_: Exception) { blockedPkg }

        binding.tvBlockedApp.text = "\"$appName\" is blocked"
        binding.tvMessage.text = "This app has been restricted by your parent."

        binding.btnGoHome.setOnClickListener { goToLauncher() }
    }

    private fun goToLauncher() {
        // Go to Gia launcher (which IS the home screen)
        val home = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(home)
        finish()
    }

    // Block back — child cannot go back to blocked app
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            goToLauncher()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // If user somehow gets back to this, keep showing it
    override fun onResume() {
        super.onResume()
        // Re-check if still blocked
        val blockedPkgs = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
            .getStringSet("blocked", emptySet()) ?: emptySet()
        val blockedPkg = intent.getStringExtra("blocked_package") ?: return
        if (blockedPkg !in blockedPkgs) {
            // No longer blocked, let them through
            finish()
        }
    }
}
