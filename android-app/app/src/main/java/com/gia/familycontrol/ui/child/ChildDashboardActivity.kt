package com.gia.familycontrol.ui.child

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.admin.GiaDeviceAdminReceiver
import com.gia.familycontrol.databinding.ActivityChildDashboardBinding
import com.gia.familycontrol.model.PairRequest
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

        requestDeviceAdmin()
        startServices()

        binding.btnPair.setOnClickListener { pairWithParent() }
        binding.btnSos.setOnClickListener { sendSos() }
    }

    private fun startServices() {
        startForegroundService(Intent(this, LocationTrackingService::class.java))
        startForegroundService(Intent(this, AppMonitorService::class.java))
    }

    private fun pairWithParent() {
        val code = binding.etPairCode.text.toString().trim()
        if (code.isEmpty()) return

        FirebaseMessaging.getInstance().token.addOnSuccessListener { fcmToken ->
            lifecycleScope.launch {
                try {
                    val response = api.pairDevice(PairRequest(
                        pairCode = code,
                        deviceName = android.os.Build.MODEL,
                        deviceModel = android.os.Build.MANUFACTURER,
                        androidVersion = android.os.Build.VERSION.RELEASE,
                        fcmToken = fcmToken
                    ))
                    if (response.isSuccessful) {
                        val device = response.body()!!
                        getSharedPreferences("gia_prefs", MODE_PRIVATE).edit()
                            .putLong("device_id", device.id).apply()
                        Toast.makeText(this@ChildDashboardActivity, "Paired successfully!", Toast.LENGTH_SHORT).show()
                        binding.tvStatus.text = "Paired with parent ✓"
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ChildDashboardActivity, "Pairing failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendSos() {
        lifecycleScope.launch {
            try {
                val deviceId = getSharedPreferences("gia_prefs", MODE_PRIVATE).getLong("device_id", -1L)
                if (deviceId != -1L) {
                    api.sendCommand(com.gia.familycontrol.model.SendCommandRequest(deviceId, "SOS"))
                    Toast.makeText(this@ChildDashboardActivity, "SOS sent to parent!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    private fun requestDeviceAdmin() {
        val dpm = getSystemService(DevicePolicyManager::class.java)
        val admin = ComponentName(this, GiaDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(admin)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Required for parental control features")
            }
            startActivity(intent)
        }
    }
}
