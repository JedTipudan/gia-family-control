# Critical Fixes Applied

## Issues Fixed

### 1. ✅ Lock Screen Not Persisting
**Problem**: Lock screen could be bypassed by pressing home button, and disappeared when returning to app.

**Solution**:
- Added `SharedPreferences` to persist lock state (`gia_lock` → `is_locked`)
- Lock state is saved when FCM sends `LOCK` command
- Lock state is cleared when FCM sends `UNLOCK` command
- Lock screen checks lock state on:
  - `onCreate()` - exits if not locked
  - `onResume()` - exits if not locked
  - Monitoring loop (every 300ms) - exits if not locked
- Added `onStop()` to bring lock screen back if user switches apps
- Boot receiver checks lock state and shows lock screen after device restart
- Child dashboard checks lock state on startup

### 2. ✅ Lock Screen Bypassed by Home Button
**Problem**: User could press home button and access other apps while "locked".

**Solution**:
- Changed `onKeyDown()` to return `true` for ALL keys (not just specific ones)
- Added `dispatchKeyEvent()` override to block all key events at dispatch level
- Added `FLAG_ACTIVITY_REORDER_TO_FRONT` to bring lock screen back when user tries to leave
- Monitoring loop now uses `moveTaskToFront()` to aggressively bring lock screen back
- Added more system UI flags for immersive fullscreen mode

### 3. ✅ Lock Only Works When App is Open
**Problem**: FCM lock command only worked if app was already running.

**Solution**:
- Lock state is now persisted in SharedPreferences
- Boot receiver checks lock state and launches lock screen after device restart
- Child dashboard checks lock state on startup and launches lock screen if needed
- Lock screen activity uses `FLAG_ACTIVITY_NEW_TASK` to launch from background

### 4. ✅ Location Not Showing in Web Dashboard
**Problem**: Web dashboard showed "Waiting for location..." even though child app was running.

**Solution**:
- Added logging to `LocationTrackingService` to debug location sending
- Changed web dashboard polling from 10 seconds to 5 seconds
- Added immediate location fetch on dashboard load (don't wait for first interval)
- Added console logging to see location API responses
- Added "Waiting for location data..." message when no location available

## Testing Checklist

### Lock Screen Tests
- [ ] Lock device from parent app → child device shows lock screen
- [ ] Press home button while locked → lock screen stays on top
- [ ] Press back button while locked → lock screen stays on top
- [ ] Open recent apps while locked → lock screen stays on top
- [ ] Restart device while locked → lock screen appears after boot
- [ ] Unlock from parent app → lock screen disappears
- [ ] Lock, then open child app → lock screen appears immediately

### Location Tests
- [ ] Pair child device with parent
- [ ] Open web dashboard → location appears within 5-10 seconds
- [ ] Check browser console for location API responses
- [ ] Check Android logcat for "LocationService" logs
- [ ] Move child device → location updates on web dashboard

### Integration Tests
- [ ] Lock device while app is closed → lock screen appears
- [ ] Lock device while app is open → lock screen appears
- [ ] Restart device while locked → lock screen appears after boot
- [ ] Location continues updating while device is locked

## Debugging

### Check if location is being sent:
```bash
adb logcat | grep LocationService
```

### Check if lock state is persisted:
```bash
adb shell run-as com.gia.familycontrol cat /data/data/com.gia.familycontrol/shared_prefs/gia_lock.xml
```

### Check web dashboard console:
Open browser DevTools → Console → Look for "Location data:" logs

### Check backend logs:
Look for location update requests in Spring Boot console

## Known Limitations

1. **Home Button**: Android 10+ restricts blocking home button completely. Lock screen uses aggressive monitoring to bring itself back to front.

2. **Task Switcher**: Some Android versions allow accessing task switcher briefly before lock screen returns.

3. **Power Button**: Cannot prevent power button from turning off screen. Lock screen reappears when screen turns back on.

4. **Safe Mode**: User can boot into safe mode to disable app. Requires device admin to be enabled.

5. **Factory Reset**: User can factory reset device to remove app. This is by design for safety.

## Next Steps

If issues persist:

1. **Location not updating**: Check if child device has internet connection and location permissions
2. **Lock screen bypassed**: Enable device admin in child app settings
3. **Lock disappears**: Check logcat for errors in LockScreenActivity
4. **Web shows old location**: Clear browser cache and check API endpoint directly
