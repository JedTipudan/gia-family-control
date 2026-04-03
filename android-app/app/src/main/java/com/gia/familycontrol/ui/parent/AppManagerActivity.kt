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
        android.util.Log.d("AppManager", "Loading apps for device: $childDeviceId")
        
        if (childDeviceId == -1L) {
            Toast.makeText(this, "No child device paired", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        lifecycleScope.launch {
            try {
                android.util.Log.d("AppManager", "Fetching installed apps from backend...")
                // Load installed apps from backend
                val appsResponse = api.getInstalledApps(childDeviceId)
                android.util.Log.d("AppManager", "Apps response: ${appsResponse.code()}")
                
                val controlsResponse = api.getAppControls(childDeviceId)
                android.util.Log.d("AppManager", "Controls response: ${controlsResponse.code()}")
                
                if (!appsResponse.isSuccessful) {
                    val error = appsResponse.errorBody()?.string()
                    android.util.Log.e("AppManager", "Failed to load apps: $error")
                    Toast.makeText(this@AppManagerActivity, "Child must start monitoring first to sync apps", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }
                
                val blockedPackages = controlsResponse.body()
                    ?.filter { it.controlType == "BLOCKED" }
                    ?.map { it.packageName }?.toSet() ?: emptySet()

                val installedApps = appsResponse.body()?.map { app ->
                    InstalledApp(
                        packageName = app.packageName,
                        appName = app.appName,
                        isSystem = app.isSystem,
                        isBlocked = app.packageName in blockedPackages
                    )
                } ?: emptyList()
                
                android.util.Log.d("AppManager", "Loaded ${installedApps.size} apps")

                apps.clear()
                apps.addAll(installedApps)
                setupRecyclerView()
            } catch (e: Exception) {
                android.util.Log.e("AppManager", "Exception loading apps", e)
                Toast.makeText(this@AppManagerActivity, "Failed to load apps: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
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
                val response = api.setAppControl(AppControlRequest(
                    deviceId = childDeviceId,
                    packageName = app.packageName,
                    controlType = if (block) "BLOCKED" else "ALLOWED"
                ))
                
                if (response.isSuccessful) {
                    app.isBlocked = block
                    binding.rvApps.adapter?.notifyDataSetChanged()
                    Toast.makeText(this@AppManagerActivity, 
                        if (block) "${app.appName} blocked" else "${app.appName} allowed", 
                        Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@AppManagerActivity, "Failed to update", Toast.LENGTH_SHORT).show()
                    // Revert switch
                    binding.rvApps.adapter?.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AppManagerActivity, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                // Revert switch
                binding.rvApps.adapter?.notifyDataSetChanged()
            }
        }
    }
}
