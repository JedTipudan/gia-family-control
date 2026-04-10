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
    private val allApps = mutableListOf<ManagedApp>()
    private var showSystem = false
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        childDeviceId = getSharedPreferences("parent_prefs", MODE_PRIVATE).getLong("child_device_id", -1L)
        if (childDeviceId == -1L) {
            Toast.makeText(this, "No child device paired", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        // Wire X button to just close this screen
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnToggleSystem.setOnClickListener {
            showSystem = !showSystem
            binding.btnToggleSystem.text = if (showSystem) "System: ON" else "System: OFF"
            applyFilter()
        }

        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                searchQuery = s.toString().trim()
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        loadApps()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finish()
    }

    private fun applyFilter() {
        val filtered = allApps.filter { app ->
            (showSystem || !app.isSystem) &&
            (searchQuery.isEmpty() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true))
        }
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = AppListAdapter(
            filtered,
            onToggleBlock = { app, block -> toggleBlock(app, block) },
            onToggleHide = { app, hide -> toggleHide(app, hide) }
        )
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

                allApps.clear()
                appsResp.body()
                    ?.sortedWith(compareBy({ it.isSystem }, { it.appName }))
                    ?.forEach { app ->
                        allApps.add(ManagedApp(
                            packageName = app.packageName,
                            appName = app.appName,
                            isSystem = app.isSystem,
                            isBlocked = app.packageName in blocked,
                            isHidden = app.packageName in hidden
                        ))
                    }
                applyFilter()
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
                    // Send FCM command to child
                    api.sendCommand(SendCommandRequest(
                        targetDeviceId = childDeviceId,
                        commandType = if (block) "BLOCK_APP" else "UNBLOCK_APP",
                        packageName = app.packageName
                    ))
                    binding.rvApps.adapter?.notifyDataSetChanged()
                    Toast.makeText(this@AppManagerActivity,
                        if (block) "🚫 ${app.appName} blocked" else "✅ ${app.appName} unblocked",
                        Toast.LENGTH_SHORT).show()
                } else {
                    binding.rvApps.adapter?.notifyDataSetChanged()
                    Toast.makeText(this@AppManagerActivity, "Failed: ${resp.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                binding.rvApps.adapter?.notifyDataSetChanged()
                Toast.makeText(this@AppManagerActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
