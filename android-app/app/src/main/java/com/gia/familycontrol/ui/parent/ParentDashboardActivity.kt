package com.gia.familycontrol.ui.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.databinding.ActivityParentDashboardBinding
import com.gia.familycontrol.model.SendCommandRequest
import com.gia.familycontrol.network.RetrofitClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import com.gia.familycontrol.R
import com.gia.familycontrol.auth.LoginActivity
import kotlinx.coroutines.Job

class ParentDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

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

        val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
        childDeviceId = prefs.getLong("child_device_id", -1L)
        val name = prefs.getString("full_name", "Parent") ?: "Parent"
        val pairCode = prefs.getString("pair_code", "") ?: ""

        binding.tvParentName.text = "Welcome, $name"
        if (pairCode.isNotEmpty()) {
            binding.tvPairCode.text = "Code: $pairCode"
        } else {
            loadPairCode()
        }

        try {
            val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        } catch (e: Exception) {
            // Map not available, continue without it
        }

        binding.btnLock.setOnClickListener { sendCommand("LOCK") }
        binding.btnUnlock.setOnClickListener { sendCommand("UNLOCK") }
        binding.btnApps.setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
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
                        binding.tvPairCode.text = "Code: $code"
                    }
                }
            } catch (e: Exception) { /* ignore */ }
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
                            binding.tvOnlineStatus.text = "🟢 Online"
                        }
                    }
                } catch (e: Exception) {
                    binding.tvOnlineStatus.text = "⚫ Offline"
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
                    val msg = if (type == "LOCK") "🔒 Device locked successfully" else "🔓 Device unlocked successfully"
                    Toast.makeText(this@ParentDashboardActivity, msg, Toast.LENGTH_SHORT).show()
                    binding.tvLockStatus.text = if (type == "LOCK") "🔒 Locked" else "🔓 Unlocked"
                }
            } catch (e: Exception) {
                Toast.makeText(this@ParentDashboardActivity, "Failed to send command", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        pollingJob?.cancel()
        super.onDestroy()
    }
}
