# Pairing Testing Guide - Step by Step

## What Was Fixed

1. **Parent device auto-creation**: Parent device is now automatically created when parent logs in
2. **Better error handling**: Clear error messages and logging throughout the pairing process
3. **Manual refresh**: Parent can tap "No Device Paired" to manually refresh
4. **Improved logging**: Detailed logs with emojis (✅ ❌ ⚠️) to easily track the pairing flow

---

## Testing Steps

### Step 1: Start Backend
```bash
cd backend
mvn spring-boot:run
```

**Watch for these logs:**
- `Started FamilyControlApplication`
- Backend should be running on port 8080

---

### Step 2: Create Parent Account

1. Open parent device/emulator
2. Launch Gia Family Control app
3. Click "Register"
4. Fill in:
   - Full Name: `Test Parent`
   - Email: `parent@test.com`
   - Password: `password123`
   - Role: **PARENT**
5. Click "Register"
6. Login with same credentials

**Expected Result:**
- Parent dashboard opens
- You see "Pair Code: GIA-XXXXX" at the top
- Backend logs show:
  ```
  FCM Token: <token>
  Creating new device for user: parent@test.com (ID: X)
  Device status updated: User=parent@test.com, Device ID=X, FCM Token=SET
  Parent device record created/updated
  ```

---

### Step 3: Create Child Account

1. Open child device/emulator
2. Launch Gia Family Control app
3. Click "Register"
4. Fill in:
   - Full Name: `Test Child`
   - Email: `child@test.com`
   - Password: `password123`
   - Role: **CHILD**
5. Click "Register"
6. Login with same credentials

**Expected Result:**
- Child dashboard opens
- You see "Pair with Parent" button
- Status shows "Not paired yet"

---

### Step 4: Pair Child with Parent

**Option A: Manual Code Entry**
1. On parent device, note the pair code (e.g., `GIA-ABC123`)
2. On child device, type the code in the text field
3. Click "Pair with Parent"

**Option B: QR Code Scan**
1. On parent device, tap on "Pair Code: GIA-XXXXX" to show QR code
2. On child device, click "Scan QR Code"
3. Point camera at parent's QR code

**Expected Child Logs:**
```
=== Starting pairing process ===
Pair code: GIA-ABC123
FCM Token obtained: <token>...
Sending pair request to backend...
Response code: 200
✅ Pairing successful! Device ID: Y
Starting tracking services...
Location service started
App monitor service started
Lock monitor service started
```

**Expected Backend Logs:**
```
Pairing: Child=child@test.com (ID=2) with Parent=parent@test.com (ID=1)
Child parent_id set to: 1
Device saved: ID=2, Name=<device model>
Sending CHILD_PAIRED notification to parent FCM: <token>
CHILD_PAIRED notification sent successfully
```

**Expected Child UI:**
- Button changes to "Paired ✓"
- Status shows "✅ Paired with parent successfully!"
- Toast: "✅ Paired! Monitoring started."
- Persistent notification appears: "Device is being monitored"

---

### Step 5: Verify Parent Sees Child

**Automatic (via FCM notification):**
- Parent app should automatically show child device name
- "No Device Paired" changes to device name (e.g., "Samsung SM-G950F")

**Manual Refresh (if automatic doesn't work):**
1. On parent device, tap on "No Device Paired (Tap to Refresh)"
2. Wait 1-2 seconds

**Expected Parent Logs:**
```
=== Loading child devices ===
Response code: 200
Devices received: 1
✅ Device found: ID=2, Name=Samsung SM-G950F
```

**Expected Parent UI:**
- Child device name appears at top
- Toast: "✅ Samsung SM-G950F connected"
- Map starts showing child's location
- Lock/Unlock buttons become active

---

## Troubleshooting

### Issue: Parent shows "No Device Paired" after successful child pairing

**Check 1: Verify Database**
```sql
-- Check parent-child relationship
SELECT 
    p.id as parent_id, p.email as parent_email,
    c.id as child_id, c.email as child_email, c.parent_id,
    d.id as device_id, d.device_name
FROM users p
LEFT JOIN users c ON c.parent_id = p.id
LEFT JOIN devices d ON d.user_id = c.id
WHERE p.email = 'parent@test.com';
```

**Expected Result:**
| parent_id | parent_email | child_id | child_email | parent_id | device_id | device_name |
|-----------|--------------|----------|-------------|-----------|-----------|-------------|
| 1 | parent@test.com | 2 | child@test.com | 1 | 2 | Samsung SM-G950F |

**Check 2: Verify Parent Device Exists**
```sql
SELECT * FROM devices WHERE user_id = (SELECT id FROM users WHERE email = 'parent@test.com');
```

**Expected Result:**
- Should return 1 row with parent's device info and FCM token

**Check 3: Backend Logs**
Look for errors in backend console:
- `Failed to send FCM notification` → FCM issue, but pairing still works
- `Parent device not found` → Parent device missing (should be auto-created now)

**Check 4: Android Logs**
```bash
# Child logs
adb logcat | grep "ChildDashboard"

# Parent logs
adb logcat | grep "ParentDashboard"
```

**Solution:**
1. Tap "No Device Paired (Tap to Refresh)" on parent device
2. If still not working, restart parent app
3. If still not working, check database queries above

---

### Issue: Child shows "Connection error" during pairing

**Possible Causes:**
1. Backend not running
2. Wrong BASE_URL in RetrofitClient.kt
3. Network connectivity issue
4. Invalid pair code

**Solution:**
1. Check backend is running: `curl http://localhost:8080/actuator/health`
2. Check BASE_URL in `android-app/app/src/main/java/com/gia/familycontrol/network/RetrofitClient.kt`
3. For emulator, use `http://10.0.2.2:8080/` instead of `localhost`
4. For physical device, use your computer's IP: `http://192.168.x.x:8080/`

---

### Issue: "Invalid pair code or already used"

**Cause:** Pair code was already used by another child

**Solution:**
1. Each pair code can only be used once
2. Parent needs to unpair the old child first
3. Or create a new parent account with a fresh pair code

---

## Success Criteria

✅ Parent device is created automatically on login  
✅ Child can pair using manual code or QR scan  
✅ Backend logs show successful pairing  
✅ Parent sees child device (automatically or after manual refresh)  
✅ Location tracking starts immediately  
✅ Lock/Unlock commands work  
✅ Services survive app restart and device reboot  

---

## Next Steps After Successful Pairing

1. **Test Location Tracking**: Check if parent's map shows child's location
2. **Test Lock Command**: Parent clicks "Lock Device" → Child device locks
3. **Test Unlock Command**: Parent clicks "Unlock Device" → Child device unlocks
4. **Test App Blocking**: Parent blocks an app → Child can't open it
5. **Test Reboot**: Restart child device → Services auto-start, lock state persists
