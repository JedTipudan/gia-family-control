package com.gia.familycontrol.ui.parent

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gia.familycontrol.databinding.ActivityAppManagerBinding
import com.gia.familycontrol.model.AppControlRequest
import com.gia.familycontrol.model.InstalledApp
import com.gia.familycontrol.network.RetrofitClient
import kotlinx.coroutines.launch

class AppManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppManagerBinding
    private val api by lazy { RetrofitClient.create(this) }
    private var childDeviceId: Long = -1L
    private val apps = mutableListOf<InstalledApp>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        childDeviceId = getSharedPreferences("gia_prefs", MODE_PRIVATE).getLong("child_device_id", -1L)
        loadAppsAndControls()
    }

    private fun loadAppsAndControls() {
        Toast.makeText(this, "App Manager feature coming soon. Child must sync apps first.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun setupRecyclerView() {
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = AppListAdapter(apps) { app, block ->
            toggleAppBlock(app, block)
        }
    }

    private fun toggleAppBlock(app: InstalledApp, block: Boolean) {
        lifecycleScope.launch {
            try {
                api.setAppControl(AppControlRequest(
                    deviceId = childDeviceId,
                    packageName = app.packageName,
                    controlType = if (block) "BLOCKED" else "ALLOWED"
                ))
                app.isBlocked = block
                binding.rvApps.adapter?.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(this@AppManagerActivity, "Failed to update", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
