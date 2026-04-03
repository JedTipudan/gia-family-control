# Persistent Child Login & Continuous Location Tracking

## Features Implemented

### 👤 Persistent Child Login (Never Logs Out)

**Child Account:**
- Stays logged in permanently
- No session timeout
- Cannot logout if paired with parent
- Back button minimizes app instead of closing
- Services continue running in background

**Parent Account:**
- Session timeout: 30 minutes of inactivity
- Must login again after timeout
- Can logout anytime
- Normal app behavior

### 📍 Continuous Location Tracking (Every 5 Seconds)

**Features:**
- Sends GPS location every 5 seconds
- Works even when app is closed
- Runs as foreground service (can't be killed)
- Only sends location if device is paired
- Includes battery level with each update
- Automatic retry on failure

### 🔄 How It Works

#### Child Login Flow
```
1. Child logs in → JWT token saved
2. Child pairs with parent → device_id saved
3. Services start automatically
4. Child closes app → Services keep running
5. Child reopens app → Still logged in
6. Location continues sending every 5 seconds
```

#### Parent Login Flow
```
1. Parent logs in → JWT token saved + timestamp
2. Parent uses app → Timestamp updated
3. Parent closes app → Timestamp saved
4. Parent reopens after 30 min → Must login again
5. Parent reopens within 30 min → Still logged in
```

#### Location Tracking Flow
```
Every 5 seconds:
1. LocationTrackingService gets GPS coordinates
2. Check if device is paired (device_id != -1)
3. If paired, send location to backend
4. Backend saves location to database
5. Parent dashboard polls location every 5 seconds
6. Parent sees child's location on map
```

## Code Changes

### SplashActivity.kt
```kotlin
private fun checkAuth() {
    val role = prefs.getString("role", null)
    val token = prefs.getString("jwt_token", null)

    val dest = when {
        // Child stays logged in permanently
        role == "CHILD" && token != null -> ChildDashboardActivity
        
        // Parent requires active session (30 min timeout)
        role == "PARENT" && token != null && isParentSessionActive() -> ParentDashboardActivity
        
        else -> LoginActivity
    }
}

private fun isParentSessionActive(): Boolean {
    val lastActive = prefs.getLong("parent_last_active", 0L)
    val now = System.currentTimeMillis()
    val timeout = 30 * 60 * 1000L // 30 minutes
    
    return (now - lastActive) < timeout
}
```

### LocationTrackingService.kt
```kotlin
// Update interval: 5 seconds
val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
    .setMinUpdateIntervalMillis(3000L)
    .setMaxUpdateDelayMillis(10000L)
    .build()

// Check if paired before sending
val deviceId = prefs.getLong("device_id", -1L)
if (deviceId == -1L) {
    Log.w("LocationService", "Device not paired, skipping location update")
    return
}

// Send location
api.updateLocation(LocationUpdateRequest(
    latitude = location.latitude,
    longitude = location.longitude,
    accuracy = location.accuracy,
    speed = location.speed,
    batteryLevel = getBatteryLevel()
))
```

### ChildDashboardActivity.kt
```kotlin
// Prevent logout if paired
private fun logout() {
    val deviceId = prefs.getLong("device_id", -1L)
    
    if (deviceId != -1L) {
        AlertDialog.Builder(this)
            .setTitle("Cannot Logout")
            .setMessage("Child accounts stay logged in permanently for safety monitoring.")
            .show()
        return
    }
}

// Minimize instead of closing
override fun onBackPressed() {
    moveTaskToBack(true) // Keep services running
}
```

### ParentDashboardActivity.kt
```kotlin
override fun onResume() {
    super.onResume()
    // Update parent last active timestamp
    prefs.edit()
        .putLong("parent_last_active", System.currentTimeMillis())
        .apply()
}
```

## Testing Guide

### Test Persistent Child Login

1. **Login as child:**
   ```
   - Open app
   - Login with child account
   - Pair with parent
   ```

2. **Close app completely:**
   ```bash
   adb shell am force-stop com.gia.familycontrol
   ```

3. **Reopen app:**
   - Should go directly to ChildDashboard
   - No login screen
   - Services should be running

4. **Restart device:**
   ```bash
   adb reboot
   ```

5. **After boot:**
   - App should auto-start services
   - Child still logged in
   - Location tracking active

### Test Parent Session Timeout

1. **Login as parent:**
   - Open app
   - Use app normally

2. **Close app:**
   - Wait 31 minutes

3. **Reopen app:**
   - Should show login screen
   - Must login again

4. **Close and reopen within 30 min:**
   - Should go directly to ParentDashboard
   - Still logged in

### Test Continuous Location Tracking

1. **Setup:**
   ```bash
   # Enable location logging
   adb logcat | grep LocationService
   ```

2. **Pair child device:**
   - Login as child
   - Pair with parent

3. **Check logs:**
   ```
   LocationService: Service started
   LocationService: Location updates started (every 5 seconds)
   LocationService: Location: 14.5995, 120.9842
   LocationService: Location sent successfully
   ```

4. **Close child app:**
   ```bash
   adb shell am force-stop com.gia.familycontrol
   ```

5. **Wait 10 seconds, check logs:**
   - Location should still be sending
   - Services auto-restarted

6. **Check parent dashboard:**
   - Open parent app
   - View map
   - Child location should update every 5 seconds

### Test Location Without Opening App

1. **Install app on child device**
2. **Login and pair (open app once)**
3. **Close app completely**
4. **DO NOT open app again**
5. **Check parent dashboard:**
   - Child location should be visible
   - Updates every 5 seconds
   - Even though child never opened app

## Debugging

### Check if location service is running:
```bash
adb shell dumpsys activity services | grep LocationTrackingService
```

**Expected output:**
```
ServiceRecord{...} com.gia.familycontrol/.service.LocationTrackingService
```

### Check location logs:
```bash
adb logcat | grep -E "LocationService|GiaApplication"
```

**Expected output:**
```
LocationService: Service created
LocationService: Service started
LocationService: Location updates started (every 5 seconds)
LocationService: Location: 14.5995, 120.9842
LocationService: Location sent successfully
```

### Check if child is logged in:
```bash
adb shell run-as com.gia.familycontrol cat /data/data/com.gia.familycontrol/shared_prefs/gia_prefs.xml
```

**Should contain:**
```xml
<string name="role">CHILD</string>
<string name="jwt_token">eyJ...</string>
<long name="device_id" value="123" />
```

### Check parent session:
```bash
adb shell run-as com.gia.familycontrol cat /data/data/com.gia.familycontrol/shared_prefs/gia_prefs.xml | grep parent_last_active
```

**Should show timestamp:**
```xml
<long name="parent_last_active" value="1234567890123" />
```

## Troubleshooting

### Location not sending:
1. Check if device is paired: `device_id != -1`
2. Check if location permission granted
3. Check if location service is running
4. Check backend logs for API errors
5. Verify JWT token is valid

### Child logs out automatically:
- This should NOT happen
- Check if role is saved correctly
- Check if JWT token is saved
- Check SplashActivity logic

### Parent stays logged in forever:
- Check if `parent_last_active` is being updated
- Verify 30-minute timeout logic
- Check SplashActivity session check

### Services not running:
- Check if device is paired
- Check if GiaApplication.onCreate() runs
- Check if BootReceiver is triggered
- Disable battery optimization

## Architecture

```
┌─────────────────────────────────────────┐
│         CHILD LOGIN (PERMANENT)         │
│  • Login once → Never logout            │
│  • JWT token saved permanently          │
│  • Services auto-start on boot          │
│  • Back button minimizes app            │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│    LOCATION SERVICE (EVERY 5 SEC)       │
│  • Foreground service (can't be killed) │
│  • GPS updates every 5 seconds          │
│  • Sends to backend if paired           │
│  • Includes battery level               │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│         BACKEND (STORES LOCATION)       │
│  • Receives location via REST API       │
│  • Saves to MySQL database              │
│  • Returns latest location to parent    │
└─────────────────────────────────────────┘
                    ↓
┌─────────────────────────────────────────┐
│    PARENT DASHBOARD (POLLS EVERY 5S)    │
│  • Fetches latest location              │
│  • Updates map marker                   │
│  • Shows real-time tracking             │
│  • Session timeout: 30 minutes          │
└─────────────────────────────────────────┘
```

## Key Benefits

1. **Child Safety:** Child account stays logged in permanently, ensuring continuous monitoring
2. **Parent Privacy:** Parent session expires after 30 minutes of inactivity
3. **Real-time Tracking:** Location updates every 5 seconds for accurate tracking
4. **Background Operation:** Services run even when app is closed
5. **Persistent Monitoring:** Survives app kills, device restarts, and updates

## Important Notes

- Child accounts are designed for safety monitoring
- Child knows they're being monitored (persistent notification)
- Parent can unpair device anytime
- Child can uninstall app after unpairing
- Complies with parental control policies
- No hidden spyware behavior
