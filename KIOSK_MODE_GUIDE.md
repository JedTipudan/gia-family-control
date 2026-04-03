# Kiosk Mode & Permanent Pairing Implementation

## Features Implemented

### 1. 🔒 Kiosk Mode (Lock Task Mode)

**What is Kiosk Mode?**
- True device lockdown using Android's Lock Task Mode API
- Prevents ALL navigation (home, back, recent apps, notifications)
- Device becomes completely locked to the lock screen activity
- Cannot exit without parent unlocking

**How it works:**
- When parent sends LOCK command via FCM
- Lock screen activity starts in Lock Task Mode
- Device admin/owner permissions enable full lockdown
- All hardware buttons blocked at system level
- Lock state persists across app restarts and device reboots

**Requirements:**
- Device Admin must be enabled (app prompts on first launch)
- For full kiosk mode: Device Owner mode (optional, requires ADB setup)

**Device Owner Setup (Optional - for maximum security):**
```bash
# Connect device via USB with USB debugging enabled
adb shell dpm set-device-owner com.gia.familycontrol/.admin.GiaDeviceAdminReceiver
```

**Without Device Owner:**
- App uses Device Admin mode
- Lock Task Mode works but user can still access power menu
- Still very effective for parental control

### 2. 🔗 Permanent Pairing

**Child Cannot Unpair:**
- Once paired with parent, child cannot logout
- Logout button shows message: "Only your parent can unpair this device"
- Child can still uninstall app freely (ethical design)
- Services stop automatically when app is uninstalled

**Parent Can Unpair:**
- New "🔗 Unpair Device" button in parent dashboard
- Sends UNPAIR command via FCM to child device
- Clears pairing data on both devices
- Stops all monitoring services on child device
- Child receives notification: "Device unpaired. You can now uninstall the app."

**Backend Implementation:**
- New endpoint: `POST /api/unpair-device/{deviceId}`
- Only accessible by PARENT role
- Verifies parent owns the child device
- Removes parent_id relationship in database
- Sends FCM command to child

## Testing Guide

### Test Kiosk Mode Lock

1. **Pair devices:**
   - Parent shares QR code
   - Child scans and pairs

2. **Lock device from parent:**
   - Parent clicks "🔒 Lock" button
   - Child device shows lock screen immediately

3. **Try to escape (should all fail):**
   - Press home button → stays locked
   - Press back button → stays locked
   - Press recent apps → stays locked
   - Swipe down notifications → blocked
   - Restart device → lock screen appears after boot

4. **Unlock from parent:**
   - Parent clicks "🔓 Unlock" button
   - Lock screen disappears on child device

### Test Permanent Pairing

1. **Child tries to logout after pairing:**
   - Open child app navigation drawer
   - Click "Logout"
   - Should show: "Cannot Logout - Only your parent can unpair this device"

2. **Parent unpairs device:**
   - Open parent dashboard
   - Click "🔗 Unpair Device"
   - Confirm dialog
   - Child receives notification
   - Child can now logout/uninstall freely

3. **Verify services stopped:**
   - Check child device - location service should stop
   - Check child device - app monitor service should stop
   - Parent dashboard shows "No Device Paired"

## API Endpoints

### Unpair Device
```
POST /api/unpair-device/{deviceId}
Authorization: Bearer {parent_jwt_token}
```

**Response:**
- 200 OK - Device unpaired successfully
- 401 Unauthorized - Not a parent account
- 403 Forbidden - Parent doesn't own this device
- 404 Not Found - Device not found

## FCM Commands

### UNPAIR Command
```json
{
  "command": "UNPAIR"
}
```

**Child device actions:**
- Clears device_id from SharedPreferences
- Stops LocationTrackingService
- Stops AppMonitorService
- Shows notification to user

## Security Considerations

### Ethical Design
- Child knows they're being monitored (persistent notification)
- Child can uninstall app after unpairing
- No hidden spyware behavior
- Complies with parental control policies

### Lock Task Mode Limitations
- **Power button:** Cannot prevent device power off
- **Safe mode:** User can boot into safe mode to disable app
- **Factory reset:** User can factory reset (by design for safety)
- **ADB:** Developer with ADB access can disable

### Recommended Setup
1. Enable Device Admin (required)
2. Set as Device Owner for full kiosk mode (optional)
3. Disable developer options on child device
4. Set screen lock PIN/password

## Troubleshooting

### Lock screen can be bypassed
- Check if Device Admin is enabled
- Try setting app as Device Owner (requires ADB)
- Ensure lock state persists in SharedPreferences

### Child can logout after pairing
- Check if device_id is saved in SharedPreferences
- Verify pairing was successful
- Check backend parent_id relationship

### Unpair doesn't work
- Verify parent JWT token is valid
- Check if parent owns the child device
- Ensure FCM token is valid on child device
- Check backend logs for errors

## Code Structure

### Android
- `LockScreenActivity.kt` - Kiosk mode implementation
- `ChildDashboardActivity.kt` - Logout prevention
- `ParentDashboardActivity.kt` - Unpair button
- `GiaFcmService.kt` - UNPAIR command handler
- `GiaDeviceAdminReceiver.kt` - Device admin policies

### Backend
- `DeviceController.java` - Unpair endpoint
- `DeviceService.java` - Unpair business logic
- `FcmService.java` - Send UNPAIR command

## Next Steps

If you want even stronger lock:
1. Set app as Device Owner (requires factory reset)
2. Disable safe mode access
3. Use MDM (Mobile Device Management) solution
4. Implement remote wipe capability
