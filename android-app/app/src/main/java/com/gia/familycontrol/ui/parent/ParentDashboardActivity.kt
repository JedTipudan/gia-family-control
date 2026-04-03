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

class ParentDashboardActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityParentDashboardBinding
    private var map: GoogleMap? = null
    private val api by lazy { RetrofitClient.create(this) }
    private var childDeviceId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        childDeviceId = getSharedPreferences("gia_prefs", MODE_PRIVATE).getLong("child_device_id", -1L)

        try {
            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
            mapFragment?.getMapAsync(this)
        } catch (e: Exception) { /* map not available */ }

        binding.btnLock.setOnClickListener { sendCommand("LOCK") }
        binding.btnUnlock.setOnClickListener { sendCommand("UNLOCK") }
        binding.btnApps.setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        startLocationPolling()
    }

    private fun startLocationPolling() {
        if (childDeviceId == -1L) return
        lifecycleScope.launch {
            while (isActive) {
                try {
                    val response = api.getLatestLocation(childDeviceId)
                    if (response.isSuccessful) {
                        response.body()?.let { loc ->
                            val pos = LatLng(loc.latitude, loc.longitude)
                            map?.clear()
                            map?.addMarker(MarkerOptions().position(pos).title("Child Location"))
                            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f))
                        }
                    }
                } catch (e: Exception) { /* ignore */ }
                delay(8000L)
            }
        }
    }

    private fun sendCommand(type: String) {
        if (childDeviceId == -1L) {
            Toast.makeText(this, "No child device linked yet", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val response = api.sendCommand(SendCommandRequest(childDeviceId, type))
                if (response.isSuccessful) {
                    Toast.makeText(this@ParentDashboardActivity,
                        if (type == "LOCK") "🔒 Device locked" else "🔓 Device unlocked",
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ParentDashboardActivity, "Failed to send command", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
