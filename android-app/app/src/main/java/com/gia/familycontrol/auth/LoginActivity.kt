package com.gia.familycontrol.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.R
import com.gia.familycontrol.databinding.ActivityLoginBinding
import com.gia.familycontrol.model.LoginRequest
import com.gia.familycontrol.network.JWT_TOKEN_KEY
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.network.dataStore
import com.gia.familycontrol.ui.child.ChildLauncherActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val api by lazy { RetrofitClient.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate elements
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        binding.root.startAnimation(fadeIn)

        binding.btnLogin.setOnClickListener { doLogin() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            overridePendingTransition(R.anim.slide_up, R.anim.fade_in)
        }
    }

    private fun doLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    if (auth.role != "CHILD") {
                        setLoading(false)
                        showError("This is the child app. Please use the Gia Parent Control app to sign in as a parent.")
                        return@launch
                    }
                    dataStore.edit { it[JWT_TOKEN_KEY] = auth.token }
                    getSharedPreferences("gia_prefs", MODE_PRIVATE).edit()
                        .putString("role", auth.role)
                        .putLong("user_id", auth.userId)
                        .putString("full_name", auth.fullName)
                        .putString("email", email)
                        .apply()
                    startActivity(Intent(this@LoginActivity, ChildLauncherActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_in)
                    finishAffinity()
                } else {
                    setLoading(false)
                    showError("Invalid email or password")
                }
            } catch (e: Exception) {
                setLoading(false)
                showError("Connection error. Check your internet.")
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.errorBox.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Signing in…" else "Sign In"
        binding.errorBox.visibility = View.GONE
    }
}
