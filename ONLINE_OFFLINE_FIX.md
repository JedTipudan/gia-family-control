# Online/Offline Detection - Fixed Issues

## 🐛 Problems Fixed

### 1. **"Active now" showing question marks**
**Cause:** Null values not handled properly in Kotlin
**Fix:** Added null-safe operators (`?.` and `?:`) throughout the code

### 2. **Child offline but still showing "Online"**
**Cause:** Backend wasn't calculating if device is truly online
**Fix:** Backend now checks if `lastSeen` is within 30 seconds
```java
long secondsSinceLastSeen = Duration.between(device.getLastSeen(), LocalDateTime.now()).getSeconds();
boolean isActuallyOnline = secondsSinceLastSeen < 30;
```

### 3. **Connection type not updating when child turns off data**
**Cause:** NetworkChangeReceiver was sending commands instead of updating device status
**Fix:** Now properly calls `updateDeviceStatus()` with connection type

---

## ✅ How It Works Now

### Child Device:
1. **Every 5 seconds** (LocationTrackingService):
   - Sends GPS location
   - Updates battery level
   - Updates connection type (WIFI/MOBILE_DATA/OFFLINE)
   - Updates `lastSeen` timestamp

2. **When network changes** (NetworkChangeReceiver):
   - Immediately updates connection status
   - Sends battery level
   - Updates `isOnline` flag

### Backend:
- Receives updates from child
- Calculates if device is truly online:
  - **Online** = `lastSeen` within 30 seconds
  - **Offline** = `lastSeen` older than 30 seconds or null
- Sets `connectionType` to "OFFLINE" if device is offline

### Parent Dashboard:
- **Polls every 8 seconds**
- Shows:
  - 🟢 Green dot + "Active now" = Online (< 30s)
  - 🔴 Red dot + "Last: Xm ago" = Offline (> 30s)
  - Connection icon: 📶 WiFi, 📱 Mobile Data, ❌ Offline
  - Battery: Real-time percentage

---

## 🧪 Testing Scenarios

### Test 1: Child goes offline
1. Child device: Turn on Airplane mode
2. Wait 30 seconds
3. Parent should see:
   - ❌ "Offline"
   - Red dot
   - "Last: just now" → "Last: 1m ago"

### Test 2: Child switches WiFi to Mobile Data
1. Child device: Turn off WiFi (mobile data on)
2. Within 5 seconds, parent should see:
   - 📱 "Mobile Data"
   - Green dot
   - "Active now"

### Test 3: Child reconnects
1. Child device: Turn off Airplane mode
2. Within 5 seconds, parent should see:
   - 📶 "WiFi" or 📱 "Mobile Data"
   - Green dot
   - "Active now"

---

## 📊 Status Update Flow

```
CHILD DEVICE
    ↓ (every 5s)
LocationTrackingService
    ↓
updateLocation() + updateDeviceStatus()
    ↓
BACKEND
    ↓
Updates: battery, connectionType, lastSeen
    ↓ (every 8s)
PARENT DASHBOARD
    ↓
getChildDevices()
    ↓
Backend calculates: isOnline = (now - lastSeen < 30s)
    ↓
Parent UI updates: status, battery, connection
```

---

## 🔧 Key Changes

### Backend (DeviceService.java):
```java
// Calculate online status based on lastSeen
long secondsSinceLastSeen = Duration.between(device.getLastSeen(), LocalDateTime.now()).getSeconds();
boolean isActuallyOnline = secondsSinceLastSeen < 30;
device.setIsOnline(isActuallyOnline);

// If offline, set connection to OFFLINE
if (!isActuallyOnline) {
    device.setConnectionType("OFFLINE");
}
```

### Android (ParentDashboardActivity.kt):
```kotlin
// Null-safe handling
val isOnline = device.isOnline ?: false

// Format lastSeen time
val duration = Duration.between(lastSeenTime, now)
when {
    duration.toMinutes() < 1 -> "Last: just now"
    duration.toMinutes() < 60 -> "Last: ${duration.toMinutes()}m ago"
    duration.toHours() < 24 -> "Last: ${duration.toHours()}h ago"
    else -> "Last: ${duration.toDays()}d ago"
}
```

### Android (NetworkChangeReceiver.kt):
```kotlin
// Update device status on network change
api.updateDeviceStatus(DeviceStatusUpdate(
    batteryLevel = battery,
    isOnline = isConnected,
    fcmToken = null,
    connectionType = if (isConnected) connectionType else "OFFLINE"
))
```

---

## 🚀 Deployment

1. **Run database migration** (if not done):
   ```sql
   ALTER TABLE devices 
   ADD COLUMN connection_type VARCHAR(20) DEFAULT 'OFFLINE'
   AFTER is_online;
   ```

2. **Wait for GitHub Actions** to build new APK

3. **Install new APK** on both devices

4. **Test the scenarios above**

---

## 📝 Notes

- **30-second threshold**: Device is considered offline if no update for 30+ seconds
- **5-second updates**: Child sends location/status every 5 seconds
- **8-second polling**: Parent checks status every 8 seconds
- **Instant network change**: NetworkChangeReceiver updates immediately when WiFi/Data changes

---

**All issues should now be resolved!** 🎉
