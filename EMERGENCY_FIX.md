# EMERGENCY FIX - Child App Crashes After Pairing

## Problem
1. Child app crashes/closes after successful pairing
2. Parent shows "No Device Paired" even after child pairs successfully
3. Both devices don't detect each other

## Root Causes Fixed
1. **Services crashing**: Added error handling and 2-second delay before starting services
2. **Parent device missing**: Backend now auto-creates parent device if it doesn't exist
3. **Better logging**: Added detailed logs with ✅ ❌ emojis to track issues

---

## STEP-BY-STEP FIX

### Step 1: Rebuild and Reinstall Apps

**Backend:**
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

**Android App:**
1. In Android Studio, click **Build → Clean Project**
2. Click **Build → Rebuild Project**
3. **Uninstall** the old app from both devices
4. Click **Run** to install fresh build

---

### Step 2: Test Pairing (Fresh Start)

**A. Create Parent Account:**
1. Open parent device
2. Register new parent: `parent2@test.com` / `password123`
3. Login
4. **WAIT 5 SECONDS** for parent device to be created
5. Note the pair code: `GIA-XXXXX`

**Check Logs:**
```bash
adb logcat | grep "ParentDashboard"
```
Look for:
```
FCM Token: <token>
Parent device record created/updated
```

**B. Create Child Account:**
1. Open child device
2. Register new child: `child2@test.com` / `password123`
3. Login
4. Grant ALL permissions when asked

**C. Pair Child:**
1. Enter parent's pair code: `GIA-XXXXX`
2. Click "Pair with Parent"
3. **WATCH THE APP** - it should NOT crash

**Check Child Logs:**
```bash
adb logcat | grep "ChildDashboard"
```
Look for:
```
=== Starting pairing process ===
Response code: 200
✅ Pairing successful! Device ID: X
=== Starting tracking services ===
✅ Location service started
✅ App monitor service started
✅ Lock monitor service started
=== All services started ===
```

**D. Verify Parent Sees Child:**
1. On parent device, wait 5 seconds
2. If still shows "No Device Paired", **tap on it** to refresh
3. Should change to child device name

---

### Step 3: Check Database (If Still Not Working)

Run this SQL query:
```sql
USE railway;

-- Check if pairing exists in database
SELECT 
    p.email as parent_email,
    c.email as child_email,
    c.parent_id,
    pd.id as parent_device_id,
    cd.id as child_device_id
FROM users p
LEFT JOIN users c ON c.parent_id = p.id
LEFT JOIN devices pd ON pd.user_id = p.id
LEFT JOIN devices cd ON cd.user_id = c.id
WHERE p.email = 'parent2@test.com';
```

**Expected Result:**
| parent_email | child_email | parent_id | parent_device_id | child_device_id |
|--------------|-------------|-----------|------------------|-----------------|
| parent2@test.com | child2@test.com | 1 | 1 | 2 |

**If parent_device_id is NULL:**
```sql
-- Manually create parent device
INSERT INTO devices (user_id, device_name, device_model, is_online, last_seen)
VALUES (
    (SELECT id FROM users WHERE email = 'parent2@test.com'),
    'Parent Device',
    'Web',
    TRUE,
    NOW()
);
```

---

### Step 4: Check Backend Logs

Look for these messages during pairing:

**✅ SUCCESS:**
```
Pairing: Child=child2@test.com (ID=2) with Parent=parent2@test.com (ID=1)
Child parent_id set to: 1
Device saved: ID=2, Name=<device>
Sending CHILD_PAIRED notification to parent FCM: <token>
CHILD_PAIRED notification sent successfully
```

**❌ ERROR:**
```
Parent device not found in database
Failed to send FCM notification
```

---

### Step 5: Manual Refresh on Parent

If parent still shows "No Device Paired":

1. **Tap on "No Device Paired (Tap to Refresh)"** text
2. Wait 2 seconds
3. Should load child device

**Check Parent Logs:**
```bash
adb logcat | grep "ParentDashboard"
```
Look for:
```
=== Loading child devices ===
Response code: 200
Devices received: 1
✅ Device found: ID=2, Name=<device>
```

---

## Common Issues & Solutions

### Issue 1: Child app crashes immediately after pairing

**Cause:** Service startup crash  
**Solution:** 
- Check if location permission is granted
- Check logcat for crash stack trace
- Services now have 2-second delay and error handling

### Issue 2: "No Device Paired" on parent after successful child pairing

**Cause:** Parent device doesn't exist in database  
**Solution:**
- Restart parent app (it will auto-create device)
- Or manually create parent device using SQL above
- Tap "No Device Paired" to refresh

### Issue 3: Backend shows "Parent device not found"

**Cause:** Parent didn't register FCM token  
**Solution:**
- Restart parent app
- Check parent logs for "FCM token registered"
- Backend now auto-creates device if missing

### Issue 4: Child pairs but services don't start

**Cause:** Missing permissions  
**Solution:**
- Grant location permission
- Grant notification permission
- Grant usage stats permission (Settings → Apps → Special Access → Usage Access)

---

## Verification Checklist

After pairing, verify these:

**On Child Device:**
- [ ] App doesn't crash
- [ ] Shows "✅ Paired with parent successfully!"
- [ ] Persistent notification: "Device is being monitored"
- [ ] Services running (check Settings → Apps → Gia Family Control → Running services)

**On Parent Device:**
- [ ] Shows child device name (not "No Device Paired")
- [ ] Map shows child's location
- [ ] Lock/Unlock buttons work

**In Database:**
- [ ] Child has parent_id set
- [ ] Parent has device record
- [ ] Child has device record
- [ ] Both devices have FCM tokens

**In Backend Logs:**
- [ ] "Pairing successful" message
- [ ] "CHILD_PAIRED notification sent"
- [ ] No error messages

---

## Still Not Working?

1. **Check Android logs:**
   ```bash
   adb logcat | grep -E "(ChildDashboard|ParentDashboard|LocationService|AppMonitorService)"
   ```

2. **Check backend logs** for errors

3. **Run diagnostic SQL:**
   ```bash
   mysql -u root -p < database/diagnostic_pairing.sql
   ```

4. **Try with fresh accounts** (new parent + new child)

5. **Check BASE_URL** in `RetrofitClient.kt`:
   - Emulator: `http://10.0.2.2:8080/`
   - Physical device: `http://YOUR_COMPUTER_IP:8080/`
