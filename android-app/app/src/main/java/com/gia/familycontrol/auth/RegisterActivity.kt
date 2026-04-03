package com.gia.familycontrol.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.databinding.ActivityRegisterBinding
import com.gia.familycontrol.model.RegisterRequest
import com.gia.familycontrol.network.JWT_TOKEN_KEY
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.network.dataStore
import com.gia.familycontrol.ui.child.ChildDashboardActivity
import com.gia.familycontrol.ui.parent.ParentDashboardActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val api by lazy { RetrofitClient.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnRegister.setOnClickListener { doRegister() }
    }

    private fun doRegister() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val name = binding.etFullName.text.toString().trim()
        val role = if (binding.rbParent.isChecked) "PARENT" else "CHILD"

        lifecycleScope.launch {
            try {
                val response = api.register(RegisterRequest(email, password, name, role))
                if (response.isSuccessful) {
                    val auth = response.body()!!
                    dataStore.edit { it[JWT_TOKEN_KEY] = auth.token }
                    getSharedPreferences("gia_prefs", MODE_PRIVATE).edit()
                        .putString("role", auth.role)
                        .putLong("user_id", auth.userId)
                        .apply()

                    val dest = if (auth.role == "PARENT") ParentDashboardActivity::class.java
                               else ChildDashboardActivity::class.java
                    startActivity(Intent(this@RegisterActivity, dest))
                    finish()
                } else {
                    Toast.makeText(this@RegisterActivity, "Registration failed", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RegisterActivity, "Connection error", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
