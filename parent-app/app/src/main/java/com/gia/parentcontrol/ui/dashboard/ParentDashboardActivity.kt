package com.gia.parentcontrol.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.parentcontrol.auth.LoginActivity
import com.gia.parentcontrol.databinding.ActivityParentDashboardBinding
import com.gia.parentcontrol.model.DeviceStatusUpdate
import com.gia.parentcontrol.model.SendCommandRequest
import com.gia.parentcontrol.network.RetrofitClient
import com.gia.parentcontrol.ui.apps.AppManagerActivity
import com.gia.parentcontrol.ui.apps.QRCodeActivity
import com.gia.parentcontrol.util.SecureAuthManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ParentDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityParentDashboardBinding
    private val api by lazy { RetrofitClient.create(this) }
    private var childDeviceId: Long = -1L
    private var map: GoogleMap? = null
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityParentDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)
            loadPrefs()
            setupButtons()
            setupMap()
            registerFcmToken()
            loadChildDevice()
        } catch (e: Exception) {
            android.util.Log.e("ParentDashboard", "onCreate crash", e)
            // Show error toast and go back to login
            android.widget.Toast.makeText(this, "Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun loadPrefs() {
        val prefs = getSharedPreferences("parent_prefs", MODE_PRIVATE)
        childDeviceId = prefs.getLong("child_device_id", -1L)
        val name = prefs.getString("full_name", "Parent") ?: "Parent"
        binding.tvParentName.text = "Hi, $name"
        val pairCode = prefs.getString("pair_code", null)
        if (pairCode != null) binding.tvPairCode.text = "Pair Code: $pairCode"
    }

    private fun setupButtons() {
        binding.btnLock.setOnClickListener { sendCommand("LOCK") }
        binding.btnUnlock.setOnClickListener { sendCommand("UNLOCK") }
        binding.btnManageApps.setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }
        binding.btnShowQr.setOnClickListener {
            startActivity(Intent(this, QRCodeActivity::class.java))
        }
        binding.btnTempAccess.setOnClickListener { showTempAccessDialog() }
        binding.btnSetPin.setOnClickListener { showSetPinDialog() }
        binding.btnUnpair.setOnClickListener { confirmUnpair() }
        binding.btnLogout.setOnClickListener { logout() }
        binding.btnRefresh.setOnClickListener { loadChildDevice() }
        binding.btnShowQrBanner.setOnClickListener {
            startActivity(Intent(this, QRCodeActivity::class.java))
        }
        binding.btnBlockNotifications.setOnClickListener {
            if (childDeviceId == -1L) { Toast.makeText(this, "No child device paired", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    api.sendCommand(SendCommandRequest(childDeviceId, "BLOCK_NOTIFICATIONS"))
                    Toast.makeText(this@ParentDashboardActivity, "🔕 Notifications blocked", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) { Toast.makeText(this@ParentDashboardActivity, "Failed", Toast.LENGTH_SHORT).show() }
            }
        }
        binding.btnAllowNotifications.setOnClickListener {
            if (childDeviceId == -1L) { Toast.makeText(this, "No child device paired", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            lifecycleScope.launch {
                try {
                    api.sendCommand(SendCommandRequest(childDeviceId, "ALLOW_NOTIFICATIONS"))
                    Toast.makeText(this@ParentDashboardActivity, "🔔 Notifications allowed", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) { Toast.makeText(this@ParentDashboardActivity, "Failed", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun setupMap() {
        try {
            val mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(com.gia.parentcontrol.R.id.map, mapFragment)
                .commitAllowingStateLoss()
            mapFragment.getMapAsync(this)
        } catch (e: Exception) {
            android.util.Log.e("ParentDashboard", "Map init failed", e)
            // Map failed to load — app continues without map
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        try {
            map = googleMap
            if (childDeviceId != -1L) startPolling()
        } catch (e: Exception) {
            android.util.Log.e("ParentDashboard", "onMapReady crash", e)
        }
    }

    override fun onResume() {
        super.onResume()
        loadChildDevice()
    }

    override fun onPause() {
        super.onPause()
        pollingJob?.cancel()
    }

    private fun registerFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            lifecycleScope.launch {
                try { api.updateDeviceStatus(DeviceStatusUpdate(100, true, token, null)) } catch (_: Exception) {}
            }
        }
    }

    private fun loadChildDevice() {
        if (!::binding.isInitialized || isFinishing || isDestroyed) return
        lifecycleScope.launch {
            try {
                val response = api.getChildDevices()
                if (!::binding.isInitialized) return@launch
                if (response.isSuccessful) {
                    val device = response.body()?.firstOrNull()
                    if (device != null) {
                        childDeviceId = device.id
                        getSharedPreferences("parent_prefs", MODE_PRIVATE)
                            .edit().putLong("child_device_id", childDeviceId).apply()
                        updateDeviceUI(device)
                        if (map != null) startPolling()
                    } else {
                        binding.tvChildName.text = "No child device paired"
                        binding.tvChildStatus.text = "● Offline"
                        binding.cardPairing.visibility = android.view.View.VISIBLE
                    }
                }
                val userId = getSharedPreferences("parent_prefs", MODE_PRIVATE).getLong("user_id", -1L)
                if (userId != -1L) {
                    val profileResp = api.getUserProfile(userId)
                    profileResp.body()?.pairCode?.let { code ->
                        getSharedPreferences("parent_prefs", MODE_PRIVATE)
                            .edit().putString("pair_code", code).apply()
                        if (::binding.isInitialized) binding.tvPairCode.text = "Pair Code: $code"
                    }
                }
            } catch (e: Exception) {
                if (::binding.isInitialized) binding.tvChildStatus.text = "Connection error"
            }
        }
    }

    private fun updateDeviceUI(device: com.gia.parentcontrol.model.DeviceResponse) {
        binding.cardPairing.visibility = android.view.View.GONE
        binding.tvChildName.text = device.deviceName ?: "Child Device"
        binding.tvBattery.text = "🔋 ${device.batteryLevel}%"
        binding.tvLockState.text = if (device.isLocked) "🔒" else "🔓"

        if (device.isOnline) {
            binding.tvChildStatus.text = "● Online"
            binding.tvChildStatus.setTextColor(getColor(android.R.color.holo_green_dark))
            binding.tvConnection.text = when (device.connectionType) {
                "WIFI" -> "📶 WiFi"
                "MOBILE_DATA" -> "📱 Mobile"
                else -> "🟢 Online"
            }
        } else {
            binding.tvChildStatus.text = "● Offline"
            binding.tvChildStatus.setTextColor(getColor(android.R.color.holo_red_dark))
            binding.tvConnection.text = "Offline"
        }

        device.lastSeen?.let { binding.tvLastSeen.text = formatLastSeen(it) }
    }

    private fun formatLastSeen(lastSeenStr: String): String {
        return try {
            val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
            val lastSeenTime = java.time.LocalDateTime.parse(lastSeenStr, formatter)
            val duration = java.time.Duration.between(lastSeenTime, java.time.LocalDateTime.now())
            when {
                duration.toSeconds() < 60 -> "${duration.toSeconds()}s ago"
                duration.toMinutes() < 60 -> "${duration.toMinutes()}m ago"
                duration.toHours() < 24 -> "${duration.toHours()}h ago"
                else -> "${duration.toDays()}d ago"
            }
        } catch (_: Exception) { lastSeenStr }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    val devResp = api.getChildDevices()
                    devResp.body()?.firstOrNull()?.let { updateDeviceUI(it) }

                    val locResp = api.getLatestLocation(childDeviceId)
                    locResp.body()?.let { loc ->
                        val pos = LatLng(loc.latitude, loc.longitude)
                        map?.clear()
                        map?.addMarker(MarkerOptions().position(pos).title("📍 Child"))
                        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                    }
                } catch (_: Exception) {}
                delay(5000L)
            }
        }
    }

    private fun sendCommand(type: String) {
        if (childDeviceId == -1L) {
            Toast.makeText(this, "No child device paired", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val resp = api.sendCommand(SendCommandRequest(childDeviceId, type))
                if (resp.isSuccessful) {
                    Toast.makeText(this@ParentDashboardActivity, "✅ $type sent", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@ParentDashboardActivity, "Failed: ${resp.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ParentDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showTempAccessDialog() {
        if (childDeviceId == -1L) { Toast.makeText(this, "No child device paired", Toast.LENGTH_SHORT).show(); return }

        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 8)
        }

        // Preset buttons row
        val presets = listOf(5, 10, 15, 30, 60)
        val btnRow = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }
        presets.forEach { min ->
            val btn = com.google.android.material.button.MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = if (min < 60) "${min}m" else "1h"
                textSize = 13f
                isAllCaps = false
                val lp = android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(4, 0, 4, 0)
                }
                layoutParams = lp
                setOnClickListener {
                    grantTempAccess(min)
                    (parent as? android.view.ViewGroup)?.let {
                        val dialog = it.tag
                        if (dialog is android.app.Dialog) dialog.dismiss()
                    }
                }
            }
            btnRow.addView(btn)
        }
        layout.addView(btnRow)

        // Divider label
        val divLabel = android.widget.TextView(this).apply {
            text = "or enter custom minutes"
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 20, 0, 8)
            setTextColor(android.graphics.Color.GRAY)
        }
        layout.addView(divLabel)

        // Custom input
        val input = android.widget.EditText(this).apply {
            hint = "e.g. 45"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(input)

        val dialog = AlertDialog.Builder(this)
            .setTitle("⏱ Temporary Access")
            .setView(layout)
            .setPositiveButton("Grant") { _, _ ->
                val custom = input.text.toString().toIntOrNull()
                if (custom != null && custom > 0) grantTempAccess(custom)
                else Toast.makeText(this, "Enter a valid number of minutes", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        // Tag dialog on btnRow so preset buttons can dismiss it
        btnRow.tag = dialog
        dialog.show()
    }

    private fun grantTempAccess(minutes: Int) {
        lifecycleScope.launch {
            try {
                api.sendCommand(SendCommandRequest(
                    targetDeviceId = childDeviceId,
                    commandType = "GRANT_TEMP_ACCESS",
                    metadata = minutes.toString()
                ))
                Toast.makeText(this@ParentDashboardActivity, "✅ Access granted: $minutes min", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(this@ParentDashboardActivity, "Failed to grant access", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSetPinDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter new 4-digit PIN"
        }
        AlertDialog.Builder(this)
            .setTitle("Set Parent PIN")
            .setMessage("This PIN protects launcher exit on the child device.")
            .setView(input)
            .setPositiveButton("Set PIN") { _, _ ->
                val pin = input.text.toString()
                if (pin.length < 4) {
                    Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                } else {
                    SecureAuthManager.setPin(this, pin)
                    if (childDeviceId != -1L) {
                        lifecycleScope.launch {
                            try {
                                api.sendCommand(SendCommandRequest(
                                    targetDeviceId = childDeviceId,
                                    commandType = "SET_PIN",
                                    metadata = pin
                                ))
                            } catch (_: Exception) {}
                        }
                    }
                    Toast.makeText(this, "✅ PIN set", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun confirmUnpair() {
        if (childDeviceId == -1L) { Toast.makeText(this, "No child device paired", Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(this)
            .setTitle("Unpair Device")
            .setMessage("This will remove parental controls from the child device.")
            .setPositiveButton("Unpair") { _, _ ->
                lifecycleScope.launch {
                    try {
                        api.unpairDevice(childDeviceId)
                        getSharedPreferences("parent_prefs", MODE_PRIVATE)
                            .edit().remove("child_device_id").apply()
                        childDeviceId = -1L
                        binding.tvChildName.text = "No child device paired"
                        binding.tvChildStatus.text = "● Offline"
                        Toast.makeText(this@ParentDashboardActivity, "Device unpaired", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@ParentDashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun logout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure?")
            .setPositiveButton("Yes") { _, _ ->
                getSharedPreferences("parent_prefs", MODE_PRIVATE).edit().clear().apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
            }
            .setNegativeButton("Cancel", null).show()
    }
}
