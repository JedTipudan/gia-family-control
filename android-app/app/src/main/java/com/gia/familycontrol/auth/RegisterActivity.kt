package com.gia.familycontrol.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
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
        binding.tvLogin.setOnClickListener { finish() }
    }

    private fun doRegister() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val role = if (binding.rbParent.isChecked) "PARENT" else "CHILD"

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }
        if (password.length < 6) {
            showError("Password must be at least 6 characters")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                val response = api.register(RegisterRequest(email, password, name, role))
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
                    startActivity(Intent(this@RegisterActivity, dest))
                    finishAffinity()
                } else {
                    setLoading(false)
                    val code = response.code()
                    showError(if (code == 400) "Email already registered or invalid data" else "Registration failed. Try again.")
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
        binding.btnRegister.isEnabled = !loading
        binding.btnRegister.text = if (loading) "Creating account..." else "Create Account"
        binding.tvError.visibility = View.GONE
    }
}
