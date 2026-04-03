# Pairing & Lock Command Fixes

## Issues Fixed

### 1. ✅ Child Device Auto-Display on Parent After Pairing

**Problem:** After child scans QR code and pairs successfully, parent dashboard still shows "No Device Paired"

**Solution:**
- Backend sends FCM notification to parent when child pairs
- Parent app receives `CHILD_PAIRED` command with device info
- Parent app automatically saves child device ID
- Parent dashboard shows notification: "✅ Child Device Paired"
- Parent dashboard reloads child devices on resume
- New endpoint: `GET /api/child-devices` to fetch all paired children

**Flow:**
1. Child scans parent QR code
2. Child sends pair request to backend
3. Backend saves pairing relationship
4. Backend sends FCM `CHILD_PAIRED` to parent device
5. Parent app saves child_device_id in SharedPreferences
6. Parent app shows notification
7. Parent dashboard automatically displays child device

### 2. ✅ Lock Command Not Working

**Problem:** Parent clicks "Lock" button but child device doesn't lock

**Root Causes:**
- Parent might not have child device ID saved
- FCM token might be invalid
- Backend might reject command due to ownership check

**Solutions:**
- Added logging to track command flow
- Parent dashboard reloads child devices on start and resume
- Better error messages show exact failure reason
- Backend verifies parent owns child device before sending command

**Debug Logging:**
```kotlin
// Parent side
android.util.Log.d("ParentDashboard", "Sending LOCK command to device $childDeviceId")
android.util.Log.d("ParentDashboard", "Command response: ${response.code()} - ${response.message()}")

// Backend side
log.info("FCM sent: {} -> {}", commandType, response)
log.error("FCM send failed for command {}: {}", commandType, e.getMessage())
```

## New Features

### FCM Command: CHILD_PAIRED
```json
{
  "command": "CHILD_PAIRED",
  "childDeviceId": "123",
  "deviceName": "Samsung Galaxy",
  "deviceModel": "SM-G991B"
}
```

**Parent app actions:**
- Saves child_device_id to SharedPreferences
- Shows high-priority notification
- Updates dashboard UI

### API Endpoint: Get Child Devices
```
GET /api/child-devices
Authorization: Bearer {parent_jwt_token}
```

**Response:**
```json
[
  {
    "id": 123,
    "deviceName": "Samsung Galaxy",
    "isLocked": false,
    "batteryLevel": 85
  }
]
```

## Testing Guide

### Test Auto-Display After Pairing

1. **Setup:**
   - Parent app installed and logged in
   - Child app installed and logged in
   - Both devices have internet connection

2. **Pair devices:**
   - Parent: Open dashboard, tap pair code to show QR
   - Child: Click "Scan QR Code" button
   - Child: Scan parent's QR code
   - Child: Should show "✅ Paired with parent successfully!"

3. **Verify parent receives notification:**
   - Parent device should show notification: "✅ Child Device Paired"
   - Parent dashboard should update to show child device name
   - Parent dashboard should show child device instead of "No Device Paired"

4. **Test persistence:**
   - Close parent app completely
   - Reopen parent app
   - Child device should still be displayed

### Test Lock Command

1. **Verify pairing:**
   - Parent dashboard shows child device name (not "No Device Paired")
   - Check logcat: `adb logcat | grep ParentDashboard`

2. **Send lock command:**
   - Parent: Click "🔒 Lock" button
   - Check logcat for: "Sending LOCK command to device X"
   - Check logcat for: "Command response: 200 - OK"

3. **Verify child locks:**
   - Child device should show lock screen immediately
   - Lock screen should block all navigation
   - Check child logcat: `adb logcat | grep LockScreen`

4. **Test unlock:**
   - Parent: Click "🔓 Unlock" button
   - Child lock screen should disappear

### Troubleshooting

**Parent shows "No Device Paired" after child pairs:**
- Check if parent has internet connection
- Check if parent FCM token is valid
- Check backend logs for FCM send errors
- Manually reload: Close and reopen parent app

**Lock command fails:**
- Check parent logcat for error message
- Verify child_device_id is saved: `adb shell run-as com.gia.familycontrol cat /data/data/com.gia.familycontrol/shared_prefs/gia_prefs.xml`
- Check backend logs for ownership verification
- Verify child FCM token is valid

**Lock screen doesn't appear:**
- Check child logcat: `adb logcat | grep GiaFcmService`
- Verify FCM message received
- Check if lock state is saved: `adb shell run-as com.gia.familycontrol cat /data/data/com.gia.familycontrol/shared_prefs/gia_lock.xml`
- Ensure Device Admin is enabled

## Code Changes

### Backend
- `DeviceService.java` - Send FCM notification on pairing
- `DeviceService.java` - Add getChildDevices() method
- `DeviceController.java` - Add GET /api/child-devices endpoint
- `UserRepository.java` - Add findByParentId() method
- `CommandService.java` - Enhanced logging

### Android
- `GiaFcmService.kt` - Handle CHILD_PAIRED command
- `ParentDashboardActivity.kt` - Load child devices on start/resume
- `ParentDashboardActivity.kt` - Enhanced error logging
- `ApiService.kt` - Add getChildDevices() API call

## Next Steps

If issues persist:

1. **Enable verbose logging:**
   ```bash
   adb logcat | grep -E "ParentDashboard|GiaFcmService|LockScreen|DeviceService"
   ```

2. **Check FCM tokens:**
   - Parent: Settings → Apps → Gia Family Control → Notifications (should be enabled)
   - Child: Settings → Apps → Gia Family Control → Notifications (should be enabled)

3. **Verify backend:**
   - Check Spring Boot console for FCM send logs
   - Verify Firebase service account JSON is valid
   - Check database for parent_id relationship

4. **Test FCM directly:**
   - Use Firebase Console to send test notification
   - Verify both devices receive notifications
