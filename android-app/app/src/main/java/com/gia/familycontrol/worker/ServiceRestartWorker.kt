package com.gia.familycontrol.worker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gia.familycontrol.service.AppMonitorService
import com.gia.familycontrol.service.DeviceStatusService
import com.gia.familycontrol.service.LocationTrackingService
import com.gia.familycontrol.service.LockMonitorService

class ServiceRestartWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("ServiceRestartWorker", "Checking and restarting services...")
        
        val prefs = applicationContext.getSharedPreferences("gia_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getLong("device_id", -1L)
        
        if (deviceId == -1L) {
            Log.d("ServiceRestartWorker", "Device not paired, skipping")
            return Result.success()
        }
        
        try {
            startService(LocationTrackingService::class.java)
            startService(AppMonitorService::class.java)
            startService(DeviceStatusService::class.java)
            
            val lockPrefs = applicationContext.getSharedPreferences("gia_lock", Context.MODE_PRIVATE)
            if (lockPrefs.getBoolean("is_locked", false)) {
                startService(LockMonitorService::class.java)
            }
            
            Log.d("ServiceRestartWorker", "All services restarted")
            return Result.success()
        } catch (e: Exception) {
            Log.e("ServiceRestartWorker", "Failed to restart services", e)
            return Result.retry()
        }
    }
    
    private fun startService(serviceClass: Class<*>) {
        val intent = Intent(applicationContext, serviceClass)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.startForegroundService(intent)
        } else {
            applicationContext.startService(intent)
        }
    }
}
