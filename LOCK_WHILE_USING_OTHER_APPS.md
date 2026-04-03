# Lock Child While Using Other Apps (Roblox, YouTube, etc.)

## ✅ YES! You Can Lock Child Device Anytime

The app is designed to work **even when the child never opens it**. Once paired, the parent can lock the device at any time, regardless of what the child is doing.

## How It Works

### 1. Background Services Always Running

After pairing, these services run 24/7 in the background:

- **LocationTrackingService** - Sends GPS every 5 seconds
- **AppMonitorService** - Monitors app usage
- **LockMonitorService** - Listens for lock commands
- **GiaFcmService** - Receives commands from parent

These services start automatically:
- When device boots
- When app is installed
- When app process starts
- Even if child never opens the app

### 2. FCM Push Notifications

When parent clicks "Lock":
1. Parent app sends command to backend
2. Backend sends FCM push notification to child device
3. Child device receives notification **even if app is closed**
4. GiaFcmService wakes up and processes command
5. Lock screen appears immediately

### 3. Lock Screen Takes Over

The lock screen:
- Appears on top of any app (Roblox, YouTube, etc.)
- Blocks all navigation (home, back, recent apps)
- Uses Kiosk Mode (Lock Task Mode)
- Cannot be dismissed without parent unlocking

## Real-World Scenarios

### Scenario 1: Child Playing Roblox

```
Child's Activity:
1. Child opens Roblox
2. Child is playing game
3. Child never opened Gia Family Control app

Parent's Action:
1. Parent opens Gia Family Control app
2. Parent clicks "🔒 Lock Device"

What Happens:
1. Lock command sent via FCM
2. Child device receives push notification
3. Lock screen appears ON TOP of Roblox
4. Roblox is still running in background
5. Child cannot access Roblox
6. Child sees: "Device locked by parent"
```

### Scenario 2: Child Watching YouTube

```
Child's Activity:
1. Child opens YouTube app
2. Child is watching videos
3. Gia Family Control app is closed

Parent's Action:
1. Parent clicks "🔒 Lock Device"

What Happens:
1. Lock screen appears immediately
2. YouTube video stops playing
3. Child cannot go back to YouTube
4. Child cannot press home button
5. Device is completely locked
```

### Scenario 3: Child Never Opened App

```
Child's Activity:
1. App was installed and paired once
2. Child closed app
3. Child never opened app again
4. Child is using phone normally

Parent's Action:
1. Parent clicks "🔒 Lock Device"

What Happens:
1. Services are already running in background
2. FCM notification received
3. Lock screen appears
4. Device locked successfully
```

## Step-by-Step Test

### Setup (Do Once)

1. **Install app on child device**
2. **Open app and login as child**
3. **Pair with parent** (scan QR or enter code)
4. **Grant all permissions**
5. **Close the app completely**

### Test Lock While Child Uses Other Apps

#### Test 1: Lock While Playing Game

1. **Child device:**
   - Open any game (Roblox, Minecraft, etc.)
   - Start playing
   - DO NOT open Gia Family Control app

2. **Parent device:**
   - Open Gia Family Control app
   - Click "🔒 Lock Device"

3. **Expected result:**
   - Child device shows lock screen immediately
   - Game is hidden behind lock screen
   - Child cannot access game
   - Child cannot press home button

#### Test 2: Lock While Watching YouTube

1. **Child device:**
   - Open YouTube
   - Start watching video
   - App is closed in background

2. **Parent device:**
   - Click "🔒 Lock Device"

3. **Expected result:**
   - Lock screen appears
   - YouTube video stops
   - Child cannot go back to YouTube

#### Test 3: Lock While Browsing

1. **Child device:**
   - Open Chrome browser
   - Browse websites
   - Gia app never opened

2. **Parent device:**
   - Click "🔒 Lock Device"

3. **Expected result:**
   - Lock screen appears
   - Browser hidden
   - Device locked

### Test Unlock

1. **Parent device:**
   - Click "🔓 Unlock Device"

2. **Expected result:**
   - Lock screen disappears on child device
   - Child can use phone normally
   - Previous app (game/YouTube) is still there

## Technical Details

### How FCM Works When App is Closed

```
Parent clicks Lock
    ↓
Backend sends FCM message
    ↓
Google FCM servers
    ↓
Child device receives push notification
    ↓
Android wakes up GiaFcmService
    ↓
GiaFcmService.onMessageReceived() runs
    ↓
Lock screen activity launched
    ↓
Device locked
```

**Key Point:** FCM can wake up the app even when it's completely closed!

### Services Running in Background

Check if services are running:
```bash
adb shell dumpsys activity services | grep gia
```

Should show:
```
ServiceRecord{...} com.gia.familycontrol/.service.LocationTrackingService
ServiceRecord{...} com.gia.familycontrol/.service.AppMonitorService
ServiceRecord{...} com.gia.familycontrol/.service.LockMonitorService
```

### FCM Message Structure

When parent locks device:
```json
{
  "to": "child_fcm_token",
  "data": {
    "command": "LOCK"
  },
  "priority": "high"
}
```

### Lock Screen Behavior

```kotlin
// Lock screen appears on top of everything
flags = Intent.FLAG_ACTIVITY_NEW_TASK or
        Intent.FLAG_ACTIVITY_CLEAR_TASK or
        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS

// Starts lock task mode (kiosk mode)
startLockTask()

// Blocks all keys
override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
    return true // Block ALL keys
}
```

## Troubleshooting

### Lock command not working when child uses other apps

**Check 1: Are services running?**
```bash
adb shell dumpsys activity services | grep gia
```

If no services showing:
- Device might not be paired
- Services might have been killed
- Check battery optimization settings

**Check 2: Is FCM working?**
```bash
adb logcat | grep GiaFcmService
```

Should see:
```
GiaFcmService: onMessageReceived: LOCK
```

If not receiving:
- Check internet connection
- Verify FCM token is valid
- Check Firebase configuration

**Check 3: Is lock screen appearing?**
```bash
adb logcat | grep LockScreen
```

Should see:
```
LockScreen: Device is locked, showing lock screen
LockScreen: Lock task mode started
```

### Child can still use phone after lock

**Possible causes:**
1. Device Admin not enabled
2. Lock Task Mode not working
3. Lock state not persisted

**Solutions:**
1. Enable Device Admin in child app
2. Set app as Device Owner (requires ADB)
3. Check lock state in SharedPreferences

### Services stop running

**Cause:** Aggressive battery optimization

**Solution:**
```
Settings → Apps → Gia Family Control → Battery → Unrestricted
Settings → Apps → Gia Family Control → Autostart → Enable
```

## Important Notes

### ✅ What Works

- Lock device while child plays games
- Lock device while child watches videos
- Lock device while child browses internet
- Lock device even if child never opens app
- Lock persists across device restarts
- Services run 24/7 in background

### ⚠️ Limitations

- **Power button:** Child can turn off screen (but device stays locked)
- **Safe mode:** Child can boot into safe mode to disable app
- **Factory reset:** Child can factory reset device
- **Battery optimization:** Some devices aggressively kill background services

### 🔒 Security Features

- Child knows they're being monitored (persistent notification)
- Child cannot logout if paired
- Child cannot uninstall without parent unpairing
- Device Admin prevents easy removal
- Lock persists across restarts

## Real Parent Use Cases

### Use Case 1: Bedtime Enforcement

```
9:00 PM - Child should be sleeping
Child is playing Roblox under blanket
Parent clicks "Lock Device"
Child device locks immediately
Child cannot access phone
```

### Use Case 2: Homework Time

```
Child should be doing homework
Child is watching YouTube
Parent clicks "Lock Device"
YouTube stops
Child must do homework
```

### Use Case 3: Dinner Time

```
Family dinner time
Child is gaming
Parent clicks "Lock Device"
Game hidden
Child joins family
```

### Use Case 4: School Hours

```
Child at school
Should not use phone
Parent locks device remotely
Child cannot use phone during class
Parent unlocks after school
```

## Summary

**YES! You can lock the child device anytime, even when:**
- Child is playing Roblox ✅
- Child is watching YouTube ✅
- Child is using any other app ✅
- Child never opened Gia Family Control app ✅
- App is completely closed ✅

**The system works because:**
1. Services run 24/7 in background
2. FCM wakes up app when needed
3. Lock screen appears on top of everything
4. Kiosk mode blocks all navigation
5. Lock state persists across restarts

**Parent has full control at all times!** 🎉
