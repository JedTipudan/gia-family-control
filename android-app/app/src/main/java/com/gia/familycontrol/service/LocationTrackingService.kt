package com.gia.familycontrol.service

import android.app.Notification
import android.app.PendingIntent
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
        Log.d("LocationService", "Service created")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupLocationCallback()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("LocationService", "Service started")
        startLocationUpdates()
        return START_STICKY
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

    private fun buildNotification(): Notification {
        val intent = PendingIntent.getActivity(this, 0,
            Intent(this, ChildDashboardActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, GiaApplication.CHANNEL_LOCATION)
            .setContentTitle("Gia Family Control")
            .setContentText("Sending location every 5 seconds")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(intent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        Log.d("LocationService", "Service destroyed")
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
