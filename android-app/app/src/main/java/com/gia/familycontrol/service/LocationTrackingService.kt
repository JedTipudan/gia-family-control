package com.gia.familycontrol.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.gia.familycontrol.GiaApplication
import com.gia.familycontrol.R
import com.gia.familycontrol.model.LocationUpdateRequest
import com.gia.familycontrol.network.RetrofitClient
import com.gia.familycontrol.ui.child.ChildDashboardActivity
import com.google.android.gms.location.*
import kotlinx.coroutines.launch

class LocationTrackingService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val api by lazy { RetrofitClient.create(this) }

    override fun onCreate() {
        super.onCreate()
        Log.d("LocationService", "=== Service onCreate ===")
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            setupLocationCallback()
            startForeground(NOTIFICATION_ID, buildNotification())
            Log.d("LocationService", "✅ Service created successfully")
        } catch (e: Exception) {
            Log.e("LocationService", "❌ Failed to create service", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("LocationService", "=== Service onStartCommand ===")
        try {
            startLocationUpdates()
            Log.d("LocationService", "✅ Location updates started")
        } catch (e: Exception) {
            Log.e("LocationService", "❌ Failed to start location updates", e)
        }
        return START_STICKY // Service will restart if killed
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    Log.d("LocationService", "Location: ${location.latitude}, ${location.longitude}")
                    
                    // Check if device is paired
                    val prefs = getSharedPreferences("gia_prefs", MODE_PRIVATE)
                    val deviceId = prefs.getLong("device_id", -1L)
                    
                    if (deviceId == -1L) {
                        Log.w("LocationService", "Device not paired, skipping location update")
                        return
                    }
                    
                    lifecycleScope.launch {
                        try {
                            val response = api.updateLocation(LocationUpdateRequest(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                speed = location.speed,
                                batteryLevel = getBatteryLevel()
                            ))
                            if (response.isSuccessful) {
                                Log.d("LocationService", "Location sent successfully")
                            } else {
                                Log.e("LocationService", "Location send failed: ${response.code()} - ${response.message()}")
                            }
                            
                            // Update device status with battery and connection type
                            api.updateDeviceStatus(DeviceStatusUpdate(
                                batteryLevel = getBatteryLevel(),
                                isOnline = true,
                                fcmToken = null,
                                connectionType = getConnectionType()
                            ))
                        } catch (e: Exception) {
                            Log.e("LocationService", "Failed to send location", e)
                        }
                    }
                }
            }
        }
    }

    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateIntervalMillis(3000L)
            .setMaxUpdateDelayMillis(10000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            Log.d("LocationService", "Location updates started (every 5 seconds)")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Location permission denied", e)
            stopSelf()
        }
    }

    private fun getBatteryLevel(): Int {
        val bm = getSystemService(android.os.BatteryManager::class.java)
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun getConnectionType(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = cm.activeNetwork ?: return "OFFLINE"
        val capabilities = cm.getNetworkCapabilities(network) ?: return "OFFLINE"
        
        return when {
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE_DATA"
            else -> "OFFLINE"
        }
    }

    private fun buildNotification(): Notification {
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, ChildDashboardActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, GiaApplication.CHANNEL_LOCATION)
            .setContentTitle("Gia Family Control")
            .setContentText("Location tracking active - Tap to open")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(intent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    override fun onDestroy() {
        Log.d("LocationService", "Service destroyed")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }
    
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("LocationService", "Task removed, restarting service")
        
        // Restart the service
        val restartIntent = Intent(applicationContext, LocationTrackingService::class.java)
        val pendingIntent = PendingIntent.getService(
            applicationContext, 1, restartIntent, 
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.ELAPSED_REALTIME_WAKEUP,
            android.os.SystemClock.elapsedRealtime() + 1000,
            pendingIntent
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
