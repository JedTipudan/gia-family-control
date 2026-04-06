package com.gia.parentcontrol.ui.apps

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gia.parentcontrol.databinding.ActivityAppManagerBinding
import com.gia.parentcontrol.model.AppControlRequest
import com.gia.parentcontrol.model.ManagedApp
import com.gia.parentcontrol.model.SendCommandRequest
import com.gia.parentcontrol.network.RetrofitClient
import kotlinx.coroutines.launch

class AppManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppManagerBinding
    private val api by lazy { RetrofitClient.create(this) }
    private var childDeviceId: Long = -1L
    private val apps = mutableListOf<ManagedApp>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        childDeviceId = getSharedPreferences("parent_prefs", MODE_PRIVATE).getLong("child_device_id", -1L)
        if (childDeviceId == -1L) {
            Toast.makeText(this, "No child device paired", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        loadApps()
    }

    private fun loadApps() {
        lifecycleScope.launch {
            try {
                val appsResp = api.getInstalledApps(childDeviceId)
                val ctrlResp = api.getAppControls(childDeviceId)
                if (!appsResp.isSuccessful) {
                    Toast.makeText(this@AppManagerActivity,
                        "Child must start monitoring first", Toast.LENGTH_LONG).show()
                    finish(); return@launch
                }
                val blocked = ctrlResp.body()
                    ?.filter { it.controlType == "BLOCKED" }?.map { it.packageName }?.toSet() ?: emptySet()
                val hidden = ctrlResp.body()
                    ?.filter { it.controlType == "HIDDEN" }?.map { it.packageName }?.toSet() ?: emptySet()

                apps.clear()
                appsResp.body()?.forEach { app ->
                    apps.add(ManagedApp(
                        packageName = app.packageName,
                        appName = app.appName,
                        isSystem = app.isSystem,
                        isBlocked = app.packageName in blocked,
                        isHidden = app.packageName in hidden
                    ))
                }
                binding.rvApps.layoutManager = LinearLayoutManager(this@AppManagerActivity)
                binding.rvApps.adapter = AppListAdapter(
                    apps,
                    onToggleBlock = { app, block -> toggleBlock(app, block) },
                    onToggleHide = { app, hide -> toggleHide(app, hide) }
                )
            } catch (e: Exception) {
                Toast.makeText(this@AppManagerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun toggleBlock(app: ManagedApp, block: Boolean) {
        lifecycleScope.launch {
            try {
                val resp = api.setAppControl(AppControlRequest(
                    childDeviceId, app.packageName, if (block) "BLOCKED" else "ALLOWED"
                ))
                if (resp.isSuccessful) {
                    app.isBlocked = block
                    binding.rvApps.adapter?.notifyDataSetChanged()
                    Toast.makeText(this@AppManagerActivity,
                        if (block) "🚫 ${app.appName} blocked" else "✅ ${app.appName} allowed",
                        Toast.LENGTH_SHORT).show()
                } else {
                    binding.rvApps.adapter?.notifyDataSetChanged()
                }
            } catch (_: Exception) { binding.rvApps.adapter?.notifyDataSetChanged() }
        }
    }

    private fun toggleHide(app: ManagedApp, hide: Boolean) {
        lifecycleScope.launch {
            try {
                val resp = api.setAppControl(AppControlRequest(
                    childDeviceId, app.packageName, if (hide) "HIDDEN" else "VISIBLE"
                ))
                if (resp.isSuccessful) {
                    app.isHidden = hide
                    binding.rvApps.adapter?.notifyDataSetChanged()
                    // Send FCM to child to actually hide/unhide via Device Owner
                    api.sendCommand(SendCommandRequest(
                        targetDeviceId = childDeviceId,
                        commandType = if (hide) "HIDE_APP" else "UNHIDE_APP",
                        packageName = app.packageName
                    ))
                    Toast.makeText(this@AppManagerActivity,
                        if (hide) "👁 ${app.appName} hidden" else "👁 ${app.appName} visible",
                        Toast.LENGTH_SHORT).show()
                } else {
                    binding.rvApps.adapter?.notifyDataSetChanged()
                }
            } catch (_: Exception) { binding.rvApps.adapter?.notifyDataSetChanged() }
        }
    }
}
