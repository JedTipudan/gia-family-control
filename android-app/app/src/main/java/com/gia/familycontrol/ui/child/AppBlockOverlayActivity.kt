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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityAppBlockOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val blockedPackage = intent.getStringExtra("blocked_package") ?: "this app"
        binding.tvBlockedApp.text = "\"$blockedPackage\" is blocked"
        binding.tvMessage.text = "This app has been restricted by your parent."

        // Go to home screen instead of blocked app
        binding.btnGoHome.setOnClickListener {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
            finish()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                // Send to home, not back to blocked app
                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(homeIntent)
                finish()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
