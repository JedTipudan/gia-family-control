package com.gia.parentcontrol.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.parentcontrol.databinding.ActivityLoginBinding
import com.gia.parentcontrol.model.LoginRequest
import com.gia.parentcontrol.network.RetrofitClient
import com.gia.parentcontrol.ui.dashboard.ParentDashboardActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val api by lazy { RetrofitClient.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnLogin.setOnClickListener { doLogin() }
        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun doLogin() {
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (email.isEmpty() || password.isEmpty()) { showError("Fill in all fields"); return }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    if (auth.role != "PARENT") {
                        setLoading(false)
                        showError("This app is for parents only. Use the child app.")
                        return@launch
                    }
                    // Save to SharedPreferences only — no DataStore
                    getSharedPreferences("parent_prefs", MODE_PRIVATE).edit()
                        .putString("jwt_token_plain", auth.token)
                        .putString("role", auth.role)
                        .putLong("user_id", auth.userId)
                        .putString("full_name", auth.fullName)
                        .putString("email", email)
                        .apply()
                    startActivity(Intent(this@LoginActivity, ParentDashboardActivity::class.java))
                    finishAffinity()
                } else {
                    setLoading(false)
                    showError("Invalid email or password")
                }
            } catch (e: Exception) {
                setLoading(false)
                showError("Connection error: ${e.message}")
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvError.text = msg
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Signing in…" else "Sign In"
        binding.tvError.visibility = View.GONE
    }
}
