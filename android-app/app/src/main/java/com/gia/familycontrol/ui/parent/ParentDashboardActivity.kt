package com.gia.familycontrol.ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.R
import com.gia.familycontrol.auth.LoginActivity
import com.gia.familycontrol.databinding.ActivityParentDashboardBinding
import com.gia.familycontrol.model.SendCommandRequest
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.util.QRCodeGenerator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ParentDashboardActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityParentDashboardBinding
    private var map: GoogleMap? = null
    private val api by lazy { RetrofitClient.create(this) }
    private var childDeviceId: Long = -1L
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityParentDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setupToolbar()
        setupNavigationDrawer()
        loadUserData()
        setupMap()
        setupButtons()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
    }

    private fun setupNavigationDrawer() {
        binding.navigationView.setNavigationItemSelectedListener(this)
        
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Load dark mode preference
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        binding.navigationView.menu.findItem(R.id.nav_dark_mode).isChecked = isDarkMode
    }

    private fun loadUserData() {
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        childDeviceId = prefs.getLong("child_device_id", -1L)
        val name = prefs.getString("full_name", "Parent") ?: "Parent"
        val email = prefs.getString("email", "") ?: ""
        val pairCode = prefs.getString("pair_code", "") ?: ""

        // Update navigation header
        val headerView = binding.navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.navHeaderName).text = name
        headerView.findViewById<TextView>(R.id.navHeaderEmail).text = email

        binding.tvChildName.text = if (childDeviceId != -1L) "Child Device" else "No Device Paired"
        
        if (pairCode.isNotEmpty()) {
            binding.tvPairCode.text = "Pair Code: $pairCode"
            binding.tvPairCode.setOnClickListener { showQRCodeDialog(pairCode) }
        } else {
            loadPairCode()
        }
    }

    private fun loadPairCode() {
        val userId = getSharedPreferences("gia_prefs", MODE_PRIVATE).getLong("user_id", -1L)
        if (userId == -1L) return
        lifecycleScope.launch {
            try {
                val response = api.getUserProfile(userId)
                if (response.isSuccessful) {
                    response.body()?.pairCode?.let { code ->
                        getSharedPreferences("gia_prefs", MODE_PRIVATE)
                            .edit().putString("pair_code", code).apply()
                        binding.tvPairCode.text = "Pair Code: $code"
                        binding.tvPairCode.setOnClickListener { showQRCodeDialog(code) }
                    }
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun setupMap() {
        try {
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        } catch (e: Exception) {
            // Map not available
        }
    }

    private fun setupButtons() {
        binding.btnLock.setOnClickListener { sendCommand("LOCK") }
        binding.btnUnlock.setOnClickListener { sendCommand("UNLOCK") }
        binding.btnApps.setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (childDeviceId != -1L) startLocationPolling()
    }

    private fun startLocationPolling() {
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    val response = api.getLatestLocation(childDeviceId)
                    if (response.isSuccessful) {
                        response.body()?.let { loc ->
                            val pos = LatLng(loc.latitude, loc.longitude)
                            map?.clear()
                            map?.addMarker(MarkerOptions().position(pos).title("📍 Child"))
                            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                            binding.statusIndicator.setBackgroundResource(R.drawable.status_online)
                        }
                    }
                } catch (e: Exception) {
                    binding.statusIndicator.setBackgroundResource(R.drawable.status_offline)
                }
                delay(8000L)
            }
        }
    }

    private fun sendCommand(type: String) {
        if (childDeviceId == -1L) {
            Toast.makeText(this, "No child device linked yet.\nShare your pair code with your child.", Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            try {
                val response = api.sendCommand(SendCommandRequest(childDeviceId, type))
                if (response.isSuccessful) {
                    val msg = if (type == "LOCK") "🔒 Device locked" else "🔓 Device unlocked"
                    Toast.makeText(this@ParentDashboardActivity, msg, Toast.LENGTH_SHORT).show()
                    binding.tvLockStatus.text = if (type == "LOCK") "Locked" else "Unlocked"
                    binding.tvLockIcon.text = if (type == "LOCK") "🔒" else "🔓"
                }
            } catch (e: Exception) {
                Toast.makeText(this@ParentDashboardActivity, "Failed to send command", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                // Already on dashboard
            }
            R.id.nav_apps -> {
                startActivity(Intent(this, AppManagerActivity::class.java))
            }
            R.id.nav_location -> {
                Toast.makeText(this, "Location History - Coming Soon", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_dark_mode -> {
                toggleDarkMode(item)
            }
            R.id.nav_change_password -> {
                showChangePasswordDialog()
            }
            R.id.nav_notifications -> {
                Toast.makeText(this, "Notification Settings - Coming Soon", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_logout -> {
                logout()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun toggleDarkMode(item: MenuItem) {
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val currentMode = prefs.getBoolean("dark_mode", false)
        val newMode = !currentMode
        
        prefs.edit().putBoolean("dark_mode", newMode).apply()
        item.isChecked = newMode
        
        if (newMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun showChangePasswordDialog() {
        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setMessage("This feature will allow you to change your password securely.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                getSharedPreferences("gia_prefs", MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showQRCodeDialog(pairCode: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_qr_code, null)
        val ivQRCode = dialogView.findViewById<ImageView>(R.id.ivQRCode)
        val tvPairCode = dialogView.findViewById<TextView>(R.id.tvPairCode)
        
        tvPairCode.text = pairCode
        val qrBitmap = QRCodeGenerator.generateQRCode(pairCode, 512, 512)
        ivQRCode.setImageBitmap(qrBitmap)
        
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnClose)
            .setOnClickListener { dialog.dismiss() }
        
        dialog.show()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        super.onDestroy()
    }
}
