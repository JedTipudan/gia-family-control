package com.gia.familycontrol.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.databinding.ActivitySplashBinding
import com.gia.familycontrol.ui.child.ChildDashboardActivity
import com.gia.familycontrol.ui.parent.ParentDashboardActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            delay(2000)
            checkAuth()
        }
    }

    private fun checkAuth() {
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val role = prefs.getString("role", null)

        // Child stays logged in permanently
        // Parent requires active session
        val dest = when {
            role == "CHILD" -> ChildDashboardActivity::class.java
            role == "PARENT" && isParentSessionActive() -> ParentDashboardActivity::class.java
            else -> LoginActivity::class.java
        }

        startActivity(Intent(this, dest))
        finish()
    }
    
    private fun isParentSessionActive(): Boolean {
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val lastActive = prefs.getLong("parent_last_active", 0L)
        val now = System.currentTimeMillis()
        val timeout = 30 * 60 * 1000L // 30 minutes
        
        return (now - lastActive) < timeout
    }
}
