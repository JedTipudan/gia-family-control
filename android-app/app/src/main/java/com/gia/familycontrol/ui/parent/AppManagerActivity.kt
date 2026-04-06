package com.gia.familycontrol.ui.parent

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gia.familycontrol.databinding.ActivityAppManagerBinding
import com.gia.familycontrol.model.AppControlRequest
import com.gia.familycontrol.model.InstalledApp
import com.gia.familycontrol.model.SendCommandRequest
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.util.SecureAuthManager
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
        setupPinButton()
        setupTempAccessButton()
    }

    private fun setupPinButton() {
        // Allow parent to set/change PIN from app manager
        binding.root.findViewById<android.view.View?>(
            resources.getIdentifier("btnSetPin", "id", packageName)
        )?.setOnClickListener { showSetPinDialog() }
    }

    private fun setupTempAccessButton() {
        binding.root.findViewById<android.view.View?>(
            resources.getIdentifier("btnTempAccess", "id", packageName)
        )?.setOnClickListener { showTempAccessDialog() }
    }

    private fun showSetPinDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Enter new 4-digit PIN"
        }
        AlertDialog.Builder(this)
            .setTitle("Set Parent PIN")
            .setMessage("This PIN protects launcher exit and secure settings.")
            .setView(input)
            .setPositiveButton("Set PIN") { _, _ ->
                val pin = input.text.toString()
                if (pin.length < 4) {
                    Toast.makeText(this, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                } else {
                    SecureAuthManager.setPin(this, pin)
                    // Also push PIN to child device via FCM
                    sendCommandToChild("SET_PIN", pin = pin)
                    Toast.makeText(this, "✅ PIN set successfully", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTempAccessDialog() {
        val options = arrayOf("15 minutes", "30 minutes", "1 hour", "2 hours")
        val minutes = intArrayOf(15, 30, 60, 120)
        AlertDialog.Builder(this)
            .setTitle("Grant Temporary Access")
            .setItems(options) { _, which ->
                sendCommandToChild("GRANT_TEMP_ACCESS", minutes = minutes[which])
                Toast.makeText(this, "✅ Temporary access granted: ${options[which]}", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun sendCommandToChild(command: String, pin: String? = null, minutes: Int? = null) {
        if (childDeviceId == -1L) return
        lifecycleScope.launch {
            try {
                api.sendCommand(SendCommandRequest(
                    targetDeviceId = childDeviceId,
                    commandType = command,
                    packageName = pin,
                    metadata = minutes?.toString()
                ))
            } catch (e: Exception) {
                android.util.Log.e("AppManager", "Failed to send $command", e)
            }
        }
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
                val appsResponse = api.getInstalledApps(childDeviceId)
                val controlsResponse = api.getAppControls(childDeviceId)

                if (!appsResponse.isSuccessful) {
                    Toast.makeText(this@AppManagerActivity, "Child must start monitoring first to sync apps", Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }

                val blockedPackages = controlsResponse.body()
                    ?.filter { it.controlType == "BLOCKED" }
                    ?.map { it.packageName }?.toSet() ?: emptySet()

                val hiddenPackages = controlsResponse.body()
                    ?.filter { it.controlType == "HIDDEN" }
                    ?.map { it.packageName }?.toSet() ?: emptySet()

                val installedApps = appsResponse.body()?.map { app ->
                    InstalledApp(
                        packageName = app.packageName,
                        appName = app.appName,
                        isSystem = app.isSystem,
                        isBlocked = app.packageName in blockedPackages,
                        isHidden = app.packageName in hiddenPackages
                    )
                } ?: emptyList()

                apps.clear()
                apps.addAll(installedApps)
                setupRecyclerView()
            } catch (e: Exception) {
                Toast.makeText(this@AppManagerActivity, "Failed to load apps: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = AppListAdapter(
            apps,
            onToggleBlock = { app, block -> toggleAppBlock(app, block) },
            onToggleHide = { app, hide -> toggleAppHide(app, hide) }
        )
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
                        if (block) "✅ ${app.appName} blocked" else "✅ ${app.appName} allowed",
                        Toast.LENGTH_SHORT).show()
                } else {
                    binding.rvApps.adapter?.notifyDataSetChanged()
                    Toast.makeText(this@AppManagerActivity, "Failed: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.rvApps.adapter?.notifyDataSetChanged()
                Toast.makeText(this@AppManagerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun toggleAppHide(app: InstalledApp, hide: Boolean) {
        lifecycleScope.launch {
            try {
                // Record hide state in backend (controlType = HIDDEN)
                val response = api.setAppControl(AppControlRequest(
                    deviceId = childDeviceId,
                    packageName = app.packageName,
                    controlType = if (hide) "HIDDEN" else "VISIBLE"
                ))
                if (response.isSuccessful) {
                    app.isHidden = hide
                    binding.rvApps.adapter?.notifyDataSetChanged()
                    // Send FCM command to child device to actually hide/unhide
                    val command = if (hide) "HIDE_APP" else "UNHIDE_APP"
                    api.sendCommand(SendCommandRequest(
                        targetDeviceId = childDeviceId,
                        commandType = command,
                        packageName = app.packageName
                    ))
                    Toast.makeText(this@AppManagerActivity,
                        if (hide) "👁 ${app.appName} hidden" else "👁 ${app.appName} visible",
                        Toast.LENGTH_SHORT).show()
                } else {
                    binding.rvApps.adapter?.notifyDataSetChanged()
                    Toast.makeText(this@AppManagerActivity, "Failed: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                binding.rvApps.adapter?.notifyDataSetChanged()
                Toast.makeText(this@AppManagerActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
