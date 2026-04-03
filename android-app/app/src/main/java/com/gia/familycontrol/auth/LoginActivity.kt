package com.gia.familycontrol.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.datastore.preferences.core.edit
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
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val auth = response.body()!!
                    dataStore.edit { it[JWT_TOKEN_KEY] = auth.token }
                    getSharedPreferences("gia_prefs", MODE_PRIVATE).edit()
                        .putString("role", auth.role)
                        .putLong("user_id", auth.userId)
                        .apply()

                    val dest = if (auth.role == "PARENT") ParentDashboardActivity::class.java
                               else ChildDashboardActivity::class.java
                    startActivity(Intent(this@LoginActivity, dest))
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
