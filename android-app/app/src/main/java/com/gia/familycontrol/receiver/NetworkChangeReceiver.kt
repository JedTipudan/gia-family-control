package com.gia.familycontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.gia.familycontrol.model.SendCommandRequest
import com.gia.familycontrol.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NetworkChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ConnectivityManager.CONNECTIVITY_ACTION) {
            val isConnected = isNetworkConnected(context)
            val connectionType = getConnectionType(context)
            
            notifyParentAboutConnection(context, isConnected, connectionType)
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
            val network = cm.activeNetwork ?: return "None"
            val capabilities = cm.getNetworkCapabilities(network) ?: return "None"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Data"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unknown"
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "Mobile Data"
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                else -> "Unknown"
            }
        }
    }

    private fun notifyParentAboutConnection(context: Context, isConnected: Boolean, connectionType: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
                val deviceId = prefs.getLong("device_id", -1L)
                if (deviceId == -1L) return@launch

                val status = if (isConnected) "connected to $connectionType" else "disconnected from internet"
                
                val api = RetrofitClient.create(context)
                api.sendCommand(SendCommandRequest(
                    targetDeviceId = deviceId,
                    commandType = if (isConnected) "NETWORK_CONNECTED" else "NETWORK_DISCONNECTED",
                    metadata = "Child device $status"
                ))
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
