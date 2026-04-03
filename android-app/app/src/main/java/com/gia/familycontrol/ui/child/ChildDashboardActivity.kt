package com.gia.familycontrol.ui.child

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver
import com.gia.familycontrol.databinding.ActivityChildDashboardBinding
import com.gia.familycontrol.model.PairRequest
import com.gia.familycontrol.model.SendCommandRequest
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.service.AppMonitorService
import com.gia.familycontrol.service.LocationTrackingService
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class ChildDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildDashboardBinding
    private val api by lazy { RetrofitClient.create(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        val name = prefs.getString("full_name", "Child")
        binding.tvStatus.text = "Welcome, $name"

        binding.btnPair.setOnClickListener { pairWithParent() }
        binding.btnSos.setOnClickListener { sendSos() }

        requestPermissionsIfNeeded()
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
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        } else {
            startTrackingServices()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            // Don't finish activity, just start services if permissions granted
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startTrackingServices()
                Toast.makeText(this, "Permissions granted. Services started.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Some permissions denied. App may not work properly.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startTrackingServices() {
        try {
            startForegroundService(Intent(this, LocationTrackingService::class.java))
        } catch (e: Exception) { /* ignore if fails */ }
        try {
            startForegroundService(Intent(this, AppMonitorService::class.java))
        } catch (e: Exception) { /* ignore if fails */ }
    }

    private fun pairWithParent() {
        val code = binding.etPairCode.text.toString().trim()
        if (code.isEmpty()) {
            Toast.makeText(this, "Enter parent pair code", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this@ChildDashboardActivity, "Paired successfully!", Toast.LENGTH_SHORT).show()
                    } else {
                        binding.btnPair.isEnabled = true
                        binding.btnPair.text = "Pair with Parent"
                        Toast.makeText(this@ChildDashboardActivity, "Invalid pair code", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    binding.btnPair.isEnabled = true
                    binding.btnPair.text = "Pair with Parent"
                    Toast.makeText(this@ChildDashboardActivity, "Connection error", Toast.LENGTH_SHORT).show()
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
}
