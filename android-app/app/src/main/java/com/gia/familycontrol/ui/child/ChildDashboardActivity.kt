package com.gia.familycontrol.ui.child

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
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
import com.gia.familycontrol.network.JWT_TOKEN_KEY
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.network.dataStore
import com.gia.familycontrol.service.AppMonitorService
import com.gia.familycontrol.service.DeviceStatusService
import com.gia.familycontrol.service.LocationTrackingService
import com.gia.familycontrol.service.LockMonitorService
import com.google.android.material.navigation.NavigationView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.first
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
        binding.btnStartMonitoring.setOnClickListener { startMonitoringServices() }
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
        
        // Check if device was just paired (e.g., via QR scanner)
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val deviceId = prefs.getLong("device_id", -1L)
        
        if (deviceId != -1L) {
            // Device is paired, update UI
            binding.tvStatus.text = "✅ Paired with parent successfully!"
            binding.btnPair.text = "Paired ✓"
            binding.btnPair.isEnabled = false
            binding.btnScanQR.visibility = android.view.View.GONE
            binding.btnStartMonitoring.visibility = android.view.View.VISIBLE
            Log.d("ChildDashboard", "Device is paired. ID: $deviceId")
        }
        
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

    private fun startMonitoringServices() {
        Log.d("ChildDashboard", "User clicked Start Monitoring")
        
        // Check Usage Stats permission FIRST
        if (!hasUsageStatsPermission()) {
            showUsageStatsPrompt()
            return
        }
        
        // Check if accessibility service is enabled
        if (!isAccessibilityServiceEnabled()) {
            showAccessibilityPrompt()
            return
        }
        
        binding.btnStartMonitoring.isEnabled = false
        binding.btnStartMonitoring.text = "Starting..."
        
        // Sync installed apps to backend
        syncInstalledApps()
        
        startTrackingServices()
        
        binding.btnStartMonitoring.text = "✅ Monitoring Active"
        binding.tvStatus.text = "✅ Monitoring active! Your parent can now track and lock your device."
        
        Toast.makeText(this, "✅ Monitoring started! Services running in background.", Toast.LENGTH_LONG).show()
    }
    
    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
    
    private fun showUsageStatsPrompt() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Permission Required")
            .setMessage("To monitor and block apps, you must grant Usage Access permission.\n\nThis is REQUIRED for app blocking to work.\n\nGo to Settings → Apps → Special app access → Usage access → Gia Family Control → Enable")
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "Please enable Usage Access manually in Settings", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }
    
    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/${com.gia.familycontrol.service.GiaAccessibilityService::class.java.name}"
        val enabledServices = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return enabledServices?.contains(service) == true
    }
    
    private fun showAccessibilityPrompt() {
        AlertDialog.Builder(this)
            .setTitle("Enable Accessibility Service")
            .setMessage("To enable remote lock functionality, you must enable the Gia Family Control accessibility service.\n\nThis allows the app to lock your device when your parent sends a lock command.\n\nGo to Settings → Accessibility → Gia Family Control → Enable")
            .setPositiveButton("Open Settings") { _, _ ->
                try {
                    startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (e: Exception) {
                    Toast.makeText(this, "Please enable accessibility service manually", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Skip") { _, _ ->
                Toast.makeText(this, "⚠️ Remote lock will not work without accessibility service", Toast.LENGTH_LONG).show()
                // Start services anyway
                binding.btnStartMonitoring.isEnabled = false
                binding.btnStartMonitoring.text = "Starting..."
                syncInstalledApps()
                startTrackingServices()
                binding.btnStartMonitoring.text = "✅ Monitoring Active"
                binding.tvStatus.text = "✅ Monitoring active (lock disabled)"
            }
            .setCancelable(false)
            .show()
    }
    
    private fun syncInstalledApps() {
        lifecycleScope.launch {
            try {
                val pm = packageManager
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 } // Only user apps
                    .map { info ->
                        com.gia.familycontrol.model.AppInfo(
                            packageName = info.packageName,
                            appName = pm.getApplicationLabel(info).toString(),
                            isSystem = false
                        )
                    }
                
                api.syncApps(installedApps)
                Log.d("ChildDashboard", "Synced ${installedApps.size} apps to backend")
            } catch (e: Exception) {
                Log.e("ChildDashboard", "Failed to sync apps", e)
            }
        }
    }
    
    private fun startTrackingServices() {
        Log.d("ChildDashboard", "Starting services...")
        
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
        
        try {
            val statusIntent = Intent(this, DeviceStatusService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(statusIntent)
            } else {
                startService(statusIntent)
            }
            Log.d("ChildDashboard", "Device status service started")
        } catch (e: Exception) {
            Log.e("ChildDashboard", "Failed to start device status service", e)
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

        Log.d("ChildDashboard", "Starting pairing with code: $code")
        
        binding.btnPair.isEnabled = false
        binding.btnPair.text = "Pairing..."

        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            Log.d("ChildDashboard", "Got FCM token")
            lifecycleScope.launch {
                try {
                    val response = api.pairDevice(PairRequest(
                        pairCode = code,
                        deviceName = Build.MODEL,
                        deviceModel = Build.MANUFACTURER,
                        androidVersion = Build.VERSION.RELEASE,
                        fcmToken = fcmToken
                    ))
                    
                    Log.d("ChildDashboard", "Pairing response: ${response.code()}")
                    
                    if (response.isSuccessful && response.body() != null) {
                        val device = response.body()!!
                        Log.d("ChildDashboard", "Pairing SUCCESS! Device ID: ${device.id}")
                        
                        // Save device ID
                        getSharedPreferences("gia_prefs", MODE_PRIVATE).edit()
                            .putLong("device_id", device.id)
                            .apply()
                        
                        // Update UI
                        binding.tvStatus.text = "✅ Paired successfully!"
                        binding.btnPair.text = "Paired ✓"
                        binding.btnPair.isEnabled = false
                        binding.btnScanQR.visibility = android.view.View.GONE
                        binding.btnStartMonitoring.visibility = android.view.View.VISIBLE
                        
                        Toast.makeText(this@ChildDashboardActivity, 
                            "✅ Paired! Click 'Start Monitoring' to begin.", 
                            Toast.LENGTH_LONG).show()
                        
                        Log.d("ChildDashboard", "Pairing complete. User can now start monitoring.")
                        
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("ChildDashboard", "Pairing FAILED: ${response.code()} - $errorBody")
                        
                        binding.btnPair.isEnabled = true
                        binding.btnPair.text = "Pair with Parent"
                        
                        val errorMsg = when (response.code()) {
                            400 -> "Invalid pair code or already used"
                            404 -> "Pair code not found. Make sure it's from a PARENT account."
                            else -> "Pairing failed (${response.code()}). Try again."
                        }
                        Toast.makeText(this@ChildDashboardActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("ChildDashboard", "Pairing EXCEPTION", e)
                    binding.btnPair.isEnabled = true
                    binding.btnPair.text = "Pair with Parent"
                    Toast.makeText(this@ChildDashboardActivity, 
                        "Connection error: ${e.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
        }.addOnFailureListener { e ->
            Log.e("ChildDashboard", "FCM token FAILED", e)
            binding.btnPair.isEnabled = true
            binding.btnPair.text = "Pair with Parent"
            Toast.makeText(this, "Failed to get device token: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSos() {
        Log.d("ChildDashboard", "=== SOS BUTTON CLICKED ===")
        
        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val deviceId = prefs.getLong("device_id", -1L)
        val email = prefs.getString("email", null)
        
        Log.d("ChildDashboard", "Device ID: $deviceId")
        Log.d("ChildDashboard", "Email: $email")
        
        if (deviceId == -1L) {
            Log.e("ChildDashboard", "No device ID - not paired")
            AlertDialog.Builder(this)
                .setTitle("⚠️ Not Paired")
                .setMessage("You must pair with your parent before you can send SOS alerts.\n\nPlease enter your parent's pair code and click 'Pair with Parent'.")
                .setPositiveButton("OK") { _, _ ->
                    binding.etPairCode.requestFocus()
                }
                .show()
            return
        }
        
        // Check if JWT token exists
        lifecycleScope.launch {
            val token = dataStore.data.first()[JWT_TOKEN_KEY]
            Log.d("ChildDashboard", "JWT Token exists: ${token != null}")
            if (token != null) {
                Log.d("ChildDashboard", "Token preview: ${token.substring(0, minOf(30, token.length))}...")
            } else {
                Log.e("ChildDashboard", "NO JWT TOKEN FOUND!")
                AlertDialog.Builder(this@ChildDashboardActivity)
                    .setTitle("❌ Authentication Error")
                    .setMessage("You are not logged in properly.\n\nPlease logout and login again.")
                    .setPositiveButton("Logout") { _, _ ->
                        prefs.edit().clear().apply()
                        startActivity(Intent(this@ChildDashboardActivity, LoginActivity::class.java))
                        finishAffinity()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
                return@launch
            }
        }
        
        // Show loading
        val progressDialog = AlertDialog.Builder(this)
            .setTitle("🆘 Sending SOS...")
            .setMessage("Alerting your parent...")
            .setCancelable(false)
            .create()
        progressDialog.show()
        
        lifecycleScope.launch {
            try {
                // Get current location if available
                var locationStr = "Location unavailable"
                try {
                    val locationManager = getSystemService(android.location.LocationManager::class.java)
                    val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                        ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                    
                    if (location != null) {
                        locationStr = "Lat: ${location.latitude}, Lng: ${location.longitude}"
                    }
                    Log.d("ChildDashboard", "Location: $locationStr")
                } catch (e: Exception) {
                    Log.e("ChildDashboard", "Failed to get location for SOS", e)
                }
                
                Log.d("ChildDashboard", "Sending SOS command to backend...")
                val response = api.sendCommand(SendCommandRequest(
                    targetDeviceId = deviceId,
                    commandType = "SOS",
                    metadata = locationStr
                ))
                
                progressDialog.dismiss()
                
                Log.d("ChildDashboard", "SOS response: ${response.code()} - ${response.message()}")
                
                if (response.isSuccessful) {
                    Log.d("ChildDashboard", "✅ SOS sent successfully!")
                    
                    // Show success dialog
                    AlertDialog.Builder(this@ChildDashboardActivity)
                        .setTitle("🆘 SOS Alert Sent!")
                        .setMessage("✅ Your parent has been notified with an emergency alert!\n\nYour location: $locationStr\n\nHelp is on the way!")
                        .setPositiveButton("OK", null)
                        .show()
                    
                    Toast.makeText(this@ChildDashboardActivity, "🆘 SOS ALERT SENT TO PARENT!", Toast.LENGTH_LONG).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("ChildDashboard", "SOS failed: ${response.code()} - $errorBody")
                    
                    val errorMessage = when (response.code()) {
                        403 -> "Authentication failed (403 Forbidden).\n\nYour login session may have expired.\n\nPlease logout and login again."
                        404 -> "Parent device not found.\n\nPlease ask your parent to login to their app."
                        500 -> "Server error.\n\nPlease try again in a moment."
                        else -> "Failed to send SOS: ${response.message()}\n\nError code: ${response.code()}\n\nDetails: $errorBody"
                    }
                    
                    AlertDialog.Builder(this@ChildDashboardActivity)
                        .setTitle("❌ SOS Failed")
                        .setMessage(errorMessage)
                        .setPositiveButton(if (response.code() == 403) "Logout & Re-login" else "OK") { _, _ ->
                            if (response.code() == 403) {
                                prefs.edit().clear().apply()
                                startActivity(Intent(this@ChildDashboardActivity, LoginActivity::class.java))
                                finishAffinity()
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e("ChildDashboard", "Failed to send SOS", e)
                
                AlertDialog.Builder(this@ChildDashboardActivity)
                    .setTitle("❌ Connection Error")
                    .setMessage("Failed to send SOS alert:\n\n${e.message}\n\nPlease check your internet connection and try again.")
                    .setPositiveButton("Retry") { _, _ -> sendSos() }
                    .setNegativeButton("Cancel", null)
                    .show()
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
            R.id.nav_debug -> {
                startActivity(Intent(this, DebugActivity::class.java))
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
