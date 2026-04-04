package com.gia.familycontrol.ui.child

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.R
import com.gia.familycontrol.network.RetrofitClient
import kotlinx.coroutines.launch

class DebugActivity : AppCompatActivity() {
    
    private val api by lazy { RetrofitClient.create(this) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this).apply {
            textSize = 14f
            setPadding(32, 32, 32, 32)
        }
        setContentView(textView)
        
        lifecycleScope.launch {
            try {
                val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
                val deviceId = prefs.getLong("device_id", -1L)
                
                val blockedPrefs = getSharedPreferences("gia_blocked_apps", MODE_PRIVATE)
                val blockedLocal = blockedPrefs.getStringSet("blocked", emptySet()) ?: emptySet()
                
                val response = api.getAppControls(deviceId)
                val blockedApi = if (response.isSuccessful) {
                    response.body()?.filter { it.controlType == "BLOCKED" }?.map { it.packageName } ?: emptyList()
                } else {
                    listOf("API call failed: ${response.code()}")
                }
                
                textView.text = """
                    DEBUG INFO
                    ==========
                    
                    Device ID: $deviceId
                    
                    Blocked (SharedPreferences):
                    ${blockedLocal.joinToString("\n") { "- $it" }}
                    
                    Blocked (API):
                    ${blockedApi.joinToString("\n") { "- $it" }}
                """.trimIndent()
                
            } catch (e: Exception) {
                textView.text = "Error: ${e.message}\n${e.stackTraceToString()}"
            }
        }
    }
}
