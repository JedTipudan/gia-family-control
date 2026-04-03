# Always-Running Services & Persistent Lock

## Features Implemented

### 🚀 Auto-Start Services (No User Interaction Required)

**Problem:** Services only start when user opens the app. If child never opens app after pairing, monitoring doesn't work.

**Solution:**
1. **GiaApplication.onCreate()** - Services start automatically when app process starts
2. **BootReceiver** - Services restart on device boot/reboot
3. **MY_PACKAGE_REPLACED** - Services restart when app is updated
4. **LockMonitorService** - Persistent service that ensures lock screen stays active

### 📱 Service Auto-Start Triggers

Services automatically start in these scenarios:

1. **App Installation** → GiaApplication.onCreate() runs
2. **Device Boot** → BootReceiver receives BOOT_COMPLETED
3. **Device Reboot** → BootReceiver receives BOOT_COMPLETED
4. **App Update** → BootReceiver receives MY_PACKAGE_REPLACED
5. **Quick Boot** → BootReceiver receives QUICKBOOT_POWERON
6. **Locked Boot** → BootReceiver receives LOCKED_BOOT_COMPLETED
7. **Child Pairs** → ChildDashboardActivity starts services
8. **Any App Process Start** → GiaApplication.onCreate() runs

### 🔒 Persistent Lock Across Restarts

**Scenario:** Parent locks device → Child restarts device → Device should remain locked

**Implementation:**
- Lock state saved in SharedPreferences (`gia_lock` → `is_locked`)
- BootReceiver checks lock state on boot
- GiaApplication checks lock state on app start
- LockMonitorService continuously monitors lock state
- Lock screen automatically appears if device is locked

**Flow:**
1. Parent sends LOCK command
2. Child device saves `is_locked = true`
3. Lock screen appears
4. Child restarts device
5. BootReceiver checks `is_locked` → true
6. Lock screen appears immediately after boot
7. LockMonitorService ensures lock screen stays active

### 🔄 LockMonitorService

**Purpose:** Persistent background service that monitors lock state and ensures lock screen stays active

**Features:**
- Runs as foreground service (can't be killed easily)
- Checks lock state every 2 seconds
- Automatically shows lock screen if device is locked
- Survives app kills and device restarts
- START_STICKY flag ensures Android restarts it if killed

**Code:**
```kotlin
private fun startMonitoring() {
    monitorJob = scope.launch {
        while (isActive) {
            delay(2000) // Check every 2 seconds
            
            val lockPrefs = getSharedPreferences("gia_lock", MODE_PRIVATE)
            if (lockPrefs.getBoolean("is_locked", false)) {
                // Device should be locked, ensure lock screen is showing
                val lockIntent = Intent(this@LockMonitorService, LockScreenActivity::class.java)
                startActivity(lockIntent)
            }
        }
    }
}
```

## Services Started Automatically

For **paired child devices** (role=CHILD, device_id != -1):

1. **LocationTrackingService** - Sends GPS location every 5-10 seconds
2. **AppMonitorService** - Monitors app usage and blocks restricted apps
3. **LockMonitorService** - Ensures lock screen stays active when locked

## Testing Guide

### Test Auto-Start on Install

1. **Install app on child device**
2. **Register as child and pair with parent**
3. **Check running services:**
   ```bash
   adb shell dumpsys activity services | grep gia
   ```
4. **Should see:**
   - LocationTrackingService
   - AppMonitorService
   - LockMonitorService

5. **Kill app completely:**
   ```bash
   adb shell am force-stop com.gia.familycontrol
   ```

6. **Wait 5 seconds, check services again:**
   ```bash
   adb shell dumpsys activity services | grep gia
   ```
7. **Services should restart automatically**

### Test Lock Persistence Across Restart

1. **Pair child device with parent**
2. **Parent locks device**
3. **Verify lock screen appears on child**
4. **Restart child device:**
   ```bash
   adb reboot
   ```
5. **After boot, lock screen should appear immediately**
6. **Try to exit lock screen → should stay locked**
7. **Parent unlocks device**
8. **Lock screen should disappear**

### Test Lock Without Opening App

1. **Install app on child device**
2. **Register and pair (open app once)**
3. **Close app completely**
4. **DO NOT open app again**
5. **Parent sends lock command**
6. **Child device should show lock screen immediately**
7. **Even though child never opened the app**

### Test Lock After App Update

1. **Child device paired and locked**
2. **Update app (install new APK)**
3. **Lock screen should remain active**
4. **Services should restart automatically**

## Logs to Monitor

### Check if services are running:
```bash
adb logcat | grep -E "GiaApplication|BootReceiver|LockMonitor"
```

**Expected output:**
```
GiaApplication: App started - Role: CHILD, DeviceId: 123
GiaApplication: Services started automatically
BootReceiver: Received: android.intent.action.BOOT_COMPLETED
BootReceiver: Role: CHILD, DeviceId: 123
BootReceiver: Services started successfully
```

### Check if lock is persisting:
```bash
adb logcat | grep -E "LockScreen|LockMonitor"
```

**Expected output:**
```
LockMonitorService: Monitoring active
LockScreen: Device is locked, showing lock screen
LockScreen: Lock task mode started
```

### Check SharedPreferences:
```bash
# Check lock state
adb shell run-as com.gia.familycontrol cat /data/data/com.gia.familycontrol/shared_prefs/gia_lock.xml

# Check device pairing
adb shell run-as com.gia.familycontrol cat /data/data/com.gia.familycontrol/shared_prefs/gia_prefs.xml
```

## Architecture

```
┌─────────────────────────────────────────┐
│         APP INSTALLATION                │
│  GiaApplication.onCreate() runs         │
│  → Checks if child is paired            │
│  → Starts all services automatically    │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      SERVICES RUNNING (FOREGROUND)      │
│  • LocationTrackingService              │
│  • AppMonitorService                    │
│  • LockMonitorService (NEW)             │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         DEVICE REBOOT/RESTART           │
│  BootReceiver.onReceive() runs          │
│  → Checks if child is paired            │
│  → Restarts all services                │
│  → Checks lock state                    │
│  → Shows lock screen if locked          │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│      LOCK MONITOR (CONTINUOUS)          │
│  Every 2 seconds:                       │
│  → Check if device should be locked     │
│  → Show lock screen if needed           │
│  → Ensure lock screen stays on top      │
└─────────────────────────────────────────┘
```

## Code Changes

### New Files
- `LockMonitorService.kt` - Persistent service monitoring lock state

### Modified Files
- `GiaApplication.kt` - Auto-start services on app start
- `BootReceiver.kt` - Handle multiple boot intents, start LockMonitorService
- `ChildDashboardActivity.kt` - Start LockMonitorService on pairing
- `AndroidManifest.xml` - Add LockMonitorService, more boot intents

### Key Features

**GiaApplication.onCreate():**
```kotlin
private fun startServicesIfNeeded() {
    val role = prefs.getString("role", null)
    val deviceId = prefs.getLong("device_id", -1L)
    
    if (role == "CHILD" && deviceId != -1L) {
        startForegroundService(LocationTrackingService)
        startForegroundService(AppMonitorService)
        startForegroundService(LockMonitorService)
        
        // Check if locked
        if (isLocked()) {
            showLockScreen()
        }
    }
}
```

**BootReceiver:**
```kotlin
override fun onReceive(context: Context, intent: Intent) {
    when (intent.action) {
        ACTION_BOOT_COMPLETED,
        ACTION_LOCKED_BOOT_COMPLETED,
        ACTION_QUICKBOOT_POWERON,
        ACTION_MY_PACKAGE_REPLACED -> {
            startServicesIfNeeded(context)
        }
    }
}
```

## Troubleshooting

### Services not starting automatically:
- Check if device is paired: `device_id != -1`
- Check if role is CHILD
- Check logcat for errors
- Verify app has all permissions

### Lock screen not appearing after restart:
- Check lock state: `gia_lock.xml`
- Verify LockMonitorService is running
- Check if Device Admin is enabled
- Look for errors in logcat

### Services getting killed:
- Disable battery optimization for app
- Enable "Autostart" in device settings
- Check if device has aggressive battery saver
- Verify foreground service notification is showing

### Lock screen can be bypassed:
- Enable Device Admin
- Set app as Device Owner (requires ADB)
- Check if LockMonitorService is running
- Verify lock state is persisted

## Battery Optimization

**Important:** Some devices aggressively kill background services

**Solution:**
1. Disable battery optimization:
   ```
   Settings → Apps → Gia Family Control → Battery → Unrestricted
   ```

2. Enable autostart:
   ```
   Settings → Apps → Gia Family Control → Autostart → Enable
   ```

3. Lock app in recent apps (prevents swipe-to-kill)

## Next Steps

If services still don't persist:
1. Use WorkManager for periodic tasks
2. Implement JobScheduler as fallback
3. Use AlarmManager for critical checks
4. Consider Device Owner mode for maximum control
