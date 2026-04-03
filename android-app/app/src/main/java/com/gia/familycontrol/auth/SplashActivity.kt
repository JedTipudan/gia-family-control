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

        val dest = when (role) {
            "PARENT" -> ParentDashboardActivity::class.java
            "CHILD" -> ChildDashboardActivity::class.java
            else -> LoginActivity::class.java
        }

        startActivity(Intent(this, dest))
        finish()
    }
}
