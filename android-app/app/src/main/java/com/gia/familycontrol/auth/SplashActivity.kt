package com.gia.familycontrol.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.databinding.ActivitySplashBinding
import com.gia.familycontrol.ui.child.ChildLauncherActivity
import com.gia.familycontrol.util.UpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        lifecycleScope.launch {
            delay(1500)
            val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
            val role  = prefs.getString("role", null)
            val onboardingDone = prefs.getBoolean("onboarding_done", false)
            val dest = when {
                role == "CHILD" -> ChildLauncherActivity::class.java
                !onboardingDone -> OnboardingActivity::class.java
                else -> LoginActivity::class.java
            }
            startActivity(Intent(this@SplashActivity, dest))
            finish()
            // Check for updates in background (versionCode from build.gradle)
            UpdateChecker.check(this@SplashActivity, 2)
        }
    }
}
