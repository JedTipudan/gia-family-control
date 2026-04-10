package com.gia.parentcontrol.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.parentcontrol.databinding.ActivityRegisterBinding
import com.gia.parentcontrol.model.RegisterRequest
import com.gia.parentcontrol.network.RetrofitClient
import com.gia.parentcontrol.ui.dashboard.ParentDashboardActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val api by lazy { RetrofitClient.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnRegister.setOnClickListener { doRegister() }
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun doRegister() {
        val name     = binding.etFullName.text.toString().trim()
        val email    = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Fill in all fields"); return
        }
        if (password.length < 6) { showError("Password must be at least 6 characters"); return }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val response = api.register(RegisterRequest(email, password, name, "PARENT"))
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    getSharedPreferences("parent_prefs", MODE_PRIVATE).edit()
                        .putString("jwt_token_plain", auth.token)
                        .putString("role", auth.role)
                        .putLong("user_id", auth.userId)
                        .putString("full_name", auth.fullName)
                        .apply()
                    // Fetch pair code immediately after register
                    try {
                        val profileResp = api.getUserProfile(auth.userId)
                        profileResp.body()?.pairCode?.let { code ->
                            getSharedPreferences("parent_prefs", MODE_PRIVATE)
                                .edit().putString("pair_code", code).apply()
                        }
                    } catch (_: Exception) {}
                    startActivity(Intent(this@RegisterActivity, ParentDashboardActivity::class.java))
                    finishAffinity()
                } else {
                    setLoading(false)
                    showError(if (response.code() == 400) "Email already registered" else "Registration failed")
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
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) "Creating account…" else "Create Account"
        binding.tvError.visibility = View.GONE
    }
}
