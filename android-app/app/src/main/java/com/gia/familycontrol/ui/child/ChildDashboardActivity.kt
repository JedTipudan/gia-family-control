package com.gia.familycontrol.ui.child

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.R
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver
import com.gia.familycontrol.auth.LoginActivity
import com.gia.familycontrol.databinding.ActivityChildDashboardBinding
import com.gia.familycontrol.model.PairRequest
import com.gia.familycontrol.model.SendCommandRequest
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.service.AppMonitorService
import com.gia.familycontrol.service.LocationTrackingService
import com.gia.familycontrol.service.LockMonitorService
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class ChildDashboardActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityChildDashboardBinding
    private val api by lazy { RetrofitClient.create(this) }
    private var permissionRequestInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if device is locked
        val lockPrefs = getSharedPreferences("gia_lock", MODE_PRIVATE)
        if (lockPrefs.getBoolean("is_locked", false)) {
            val lockIntent = Intent(this, LockScreenActivity::class.java)
            startActivity(lockIntent)
        }
        
        try {
            binding = ActivityChildDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
        } catch (e: Exception) {
            Log.e("ChildDashboard", "Error inflating layout", e)
            finish()
            return
        }

        setupToolbar()
        setupNavigationDrawer()
        loadUserData()
        setupButtons()
        enableDeviceAdmin()
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
        val name = prefs.getString("full_name", "Child") ?: "Child"
        
        binding.tvWelcome.text = "Welcome, $name!"
        
        // Update navigation header
        val headerView = binding.navigationView.getHeaderView(0)
        headerView.findViewById<TextView>(R.id.navHeaderName).text = name
    }

    private fun setupButtons() {
        binding.btnPair.setOnClickListener { pairWithParent() }
        binding.btnScanQR.setOnClickListener { 
            startActivity(Intent(this, QRScannerActivity::class.java))
        }
        binding.btnSos.setOnClickListener { sendSos() }
    }

    private fun enableDeviceAdmin() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val adminComponent = ComponentName(this, GiaDeviceAdminReceiver::class.java)
        
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Enable device admin to prevent uninstallation and ensure child safety")
            }
            try {
                startActivityForResult(intent, 200)
            } catch (e: Exception) {
                Log.e("ChildDashboard", "Failed to request device admin", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!permissionRequestInProgress) {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val hasLocation = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (!hasLocation) {
            requestPermissionsIfNeeded()
        }
    }

    private fun requestPermissionsIfNeeded() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionRequestInProgress = true
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, 
        permissions: Array<out String>, 
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionRequestInProgress = false
        
        if (requestCode == 100) {
            val hasLocation = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || 
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (hasLocation) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startTrackingServices() {
        try {
            val locationIntent = Intent(this, LocationTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(locationIntent)
            } else {
                startService(locationIntent)
            }
            Log.d("ChildDashboard", "Location service started")
        } catch (e: Exception) {
            Log.e("ChildDashboard", "Failed to start location service", e)
        }
        
        try {
            val appMonitorIntent = Intent(this, AppMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(appMonitorIntent)
            } else {
                startService(appMonitorIntent)
            }
            Log.d("ChildDashboard", "App monitor service started")
        } catch (e: Exception) {
            Log.e("ChildDashboard", "Failed to start app monitor service", e)
        }
        
        try {
            val lockMonitorIntent = Intent(this, LockMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(lockMonitorIntent)
            } else {
                startService(lockMonitorIntent)
            }
            Log.d("ChildDashboard", "Lock monitor service started")
        } catch (e: Exception) {
            Log.e("ChildDashboard", "Failed to start lock monitor service", e)
        }
    }

    private fun pairWithParent() {
        val code = binding.etPairCode.text.toString().trim()
        if (code.isEmpty()) {
            Toast.makeText(this, "Enter parent pair code", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!code.startsWith("GIA-")) {
            Toast.makeText(this, "Invalid pair code format", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnPair.isEnabled = false
        binding.btnPair.text = "Pairing..."

        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            lifecycleScope.launch {
                try {
                    val response = api.pairDevice(PairRequest(
                        pairCode = code,
                        deviceName = Build.MODEL,
                        deviceModel = Build.MANUFACTURER,
                        androidVersion = Build.VERSION.RELEASE,
                        fcmToken = fcmToken
                    ))
                    if (response.isSuccessful && response.body() != null) {
                        val device = response.body()!!
                        getSharedPreferences("gia_prefs", MODE_PRIVATE).edit()
                            .putLong("device_id", device.id).apply()
                        binding.tvStatus.text = "✅ Paired with parent successfully!"
                        binding.btnPair.text = "Paired ✓"
                        
                        startTrackingServices()
                        
                        Toast.makeText(this@ChildDashboardActivity, "Paired successfully! Monitoring started.", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.btnPair.isEnabled = true
                        binding.btnPair.text = "Pair with Parent"
                        val errorMsg = when (response.code()) {
                            400 -> "Invalid pair code or already used"
                            404 -> "Pair code not found. Make sure it's from a PARENT account."
                            else -> "Pairing failed. Try again."
                        }
                        Toast.makeText(this@ChildDashboardActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    binding.btnPair.isEnabled = true
                    binding.btnPair.text = "Pair with Parent"
                    Toast.makeText(this@ChildDashboardActivity, "Connection error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.addOnFailureListener {
            binding.btnPair.isEnabled = true
            binding.btnPair.text = "Pair with Parent"
            Toast.makeText(this, "Failed to get device token", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSos() {
        lifecycleScope.launch {
            try {
                val deviceId = getSharedPreferences("gia_prefs", MODE_PRIVATE).getLong("device_id", -1L)
                if (deviceId != -1L) {
                    api.sendCommand(SendCommandRequest(deviceId, "SOS"))
                    Toast.makeText(this@ChildDashboardActivity, "🆘 SOS sent to parent!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@ChildDashboardActivity, "Pair with parent first", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChildDashboardActivity, "Failed to send SOS", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                // Already on dashboard
            }
            R.id.nav_pair -> {
                binding.etPairCode.requestFocus()
            }
            R.id.nav_sos -> {
                sendSos()
            }
            R.id.nav_dark_mode -> {
                toggleDarkMode(item)
            }
            R.id.nav_permissions -> {
                requestPermissionsIfNeeded()
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

    private fun logout() {
        // Check if paired - if paired, prevent logout
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val deviceId = prefs.getLong("device_id", -1L)
        
        if (deviceId != -1L) {
            AlertDialog.Builder(this)
                .setTitle("Cannot Logout")
                .setMessage("This device is paired with a parent account. Only your parent can unpair this device.\n\nChild accounts stay logged in permanently for safety monitoring.")
                .setPositiveButton("OK", null)
                .show()
            return
        }
        
        // Not paired yet, allow logout
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                prefs.edit().clear().apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            // For child accounts, minimize app instead of closing
            // This keeps services running
            moveTaskToBack(true)
        }
    }
}
