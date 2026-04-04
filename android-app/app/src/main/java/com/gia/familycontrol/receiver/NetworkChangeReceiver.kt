package com.gia.familycontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.gia.familycontrol.model.DeviceStatusUpdate
import com.gia.familycontrol.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val isConnected = isNetworkConnected(context)
            val connectionType = getConnectionType(context)
            
            android.util.Log.d("NetworkReceiver", "Network changed: Connected=$isConnected, Type=$connectionType")
            
            // Update device status immediately
            updateDeviceStatus(context, isConnected, connectionType)
        }
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }

    private fun getConnectionType(context: Context): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return "OFFLINE"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "OFFLINE"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE_DATA"
                else -> "OFFLINE"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WIFI"
                ConnectivityManager.TYPE_MOBILE -> "MOBILE_DATA"
                else -> "OFFLINE"
            }
        }
    }

    private fun updateDeviceStatus(context: Context, isConnected: Boolean, connectionType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
                val deviceId = prefs.getLong("device_id", -1L)
                if (deviceId == -1L) {
                    android.util.Log.w("NetworkReceiver", "Device not paired, skipping status update")
                    return@launch
                }

                val api = RetrofitClient.create(context)
                val battery = getBatteryLevel(context)
                
                api.updateDeviceStatus(DeviceStatusUpdate(
                    batteryLevel = battery,
                    isOnline = isConnected,
                    fcmToken = null,
                    connectionType = if (isConnected) connectionType else "OFFLINE"
                ))
                
                android.util.Log.d("NetworkReceiver", "Device status updated: Online=$isConnected, Type=$connectionType, Battery=$battery%")
            } catch (e: Exception) {
                android.util.Log.e("NetworkReceiver", "Failed to update device status", e)
            }
        }
    }
    
    private fun getBatteryLevel(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }
}
