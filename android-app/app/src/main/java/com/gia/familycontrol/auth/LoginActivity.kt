package com.gia.familycontrol.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.databinding.ActivityLoginBinding
import com.gia.familycontrol.model.LoginRequest
import com.gia.familycontrol.network.JWT_TOKEN_KEY
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.network.dataStore
import com.gia.familycontrol.ui.child.ChildDashboardActivity
import com.gia.familycontrol.ui.parent.ParentDashboardActivity
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
                    dataStore.edit { it[JWT_TOKEN_KEY] = auth.token }
                    getSharedPreferences("gia_prefs", MODE_PRIVATE).edit()
                        .putString("role", auth.role)
                        .putLong("user_id", auth.userId)
                        .putString("full_name", auth.fullName)
                        .apply()

                    val dest = if (auth.role == "PARENT") ParentDashboardActivity::class.java
                               else ChildDashboardActivity::class.java
                    startActivity(Intent(this@LoginActivity, dest))
                    finish()
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
        binding.tvError.visibility = View.VISIBLE
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.btnLogin.text = if (loading) "Signing in..." else "Sign In"
        binding.tvError.visibility = View.GONE
    }
}
