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
        lifecycleScope.launch {
            try {
                val controlsResponse = api.getAppControls(childDeviceId)
                val blockedPackages = controlsResponse.body()
                    ?.filter { it.controlType == "BLOCKED" }
                    ?.map { it.packageName }?.toSet() ?: emptySet()

                val pm = packageManager
                val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { !it.flags.and(android.content.pm.ApplicationInfo.FLAG_SYSTEM).equals(0).not() }
                    .map { info ->
                        InstalledApp(
                            packageName = info.packageName,
                            appName = pm.getApplicationLabel(info).toString(),
                            isSystem = (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                            isBlocked = info.packageName in blockedPackages
                        )
                    }

                apps.clear()
                apps.addAll(installedApps)
                setupRecyclerView()
            } catch (e: Exception) {
                Toast.makeText(this@AppManagerActivity, "Failed to load apps", Toast.LENGTH_SHORT).show()
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
