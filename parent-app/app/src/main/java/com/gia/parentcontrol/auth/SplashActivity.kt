package com.gia.parentcontrol.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.parentcontrol.ui.dashboard.ParentDashboardActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            delay(1500)
            val prefs = getSharedPreferences("parent_prefs", MODE_PRIVATE)
            val token = prefs.getString("jwt_token_plain", null)
            val dest = if (token != null) ParentDashboardActivity::class.java
                       else LoginActivity::class.java
            startActivity(Intent(this@SplashActivity, dest))
            finish()
        }
    }
}
