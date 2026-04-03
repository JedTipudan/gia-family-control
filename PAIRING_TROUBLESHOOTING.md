# Pairing Troubleshooting Guide

## Issue: "No Device Paired" on Parent After Child Pairs

### Quick Fixes

1. **Tap "No Device Paired" text to refresh**
   - Parent dashboard now has click-to-refresh
   - Tap the text to manually reload child devices

2. **Close and reopen parent app**
   - Parent dashboard loads child devices on resume
   - Simply close and reopen the app

3. **Check backend logs**
   - Look for pairing messages in Spring Boot console
   - Should see: "Pairing: Child=... with Parent=..."

### Step-by-Step Debugging

#### Step 1: Verify Child Pairing Succeeded
```
Child app should show:
"✅ Paired with parent successfully!"
"Monitoring started"
```

If child shows error:
- Check pair code format (must start with "GIA-")
- Verify parent account exists
- Check internet connection

#### Step 2: Check Backend Logs
```
Backend console should show:
"Pairing: Child=child@email.com (ID=2) with Parent=parent@email.com (ID=1)"
"Child parent_id set to: 1"
"Device saved: ID=3, Name=Samsung Galaxy"
```

If not showing:
- Backend might not be running
- Database connection issue
- Check Spring Boot console for errors

#### Step 3: Check Parent FCM Token
```
Parent dashboard should log:
"FCM Token: eyJ..."
"FCM token registered"
```

If not showing:
- Firebase not configured properly
- google-services.json missing
- Check parent app logs

#### Step 4: Check FCM Notification
```
Backend should log:
"Sending CHILD_PAIRED notification to parent FCM: ..."
"CHILD_PAIRED notification sent successfully"
```

If "Parent FCM token is null":
- Parent needs to open app at least once
- FCM token registration happens on parent dashboard onCreate

#### Step 5: Check Parent Receives Notification
```
Parent app should log:
"CHILD_PAIRED received: deviceId=3, name=Samsung Galaxy"
"Child device ID saved: 3"
"Pairing notification shown"
```

If not showing:
- FCM message not received
- Check Firebase Cloud Messaging setup
- Verify both devices have internet

#### Step 6: Manual Refresh
```
Parent dashboard:
1. Tap "No Device Paired" text
2. Should show "Refreshing..." toast
3. Logs: "Loading child devices..."
4. Logs: "Devices count: 1"
5. Should update to show child device name
```

### Common Issues

#### Issue: Child says "Paired" but parent shows "No Device Paired"

**Cause:** Parent FCM token not registered or FCM notification failed

**Solution:**
1. Open parent app (registers FCM token)
2. Close parent app
3. Pair child device
4. Reopen parent app
5. Tap "No Device Paired" to refresh

#### Issue: App crashes on child after pairing

**Cause:** Services trying to start without proper permissions

**Solution:**
1. Grant all permissions before pairing
2. Enable location permission
3. Enable notification permission
4. Enable usage stats permission

#### Issue: Parent never receives pairing notification

**Cause:** Parent device not registered in database

**Solution:**
1. Parent must login at least once
2. Parent device record created on login
3. FCM token saved to device record
4. Then child can pair

### Manual Database Check

If pairing still not working, check database directly:

```sql
-- Check if child has parent_id set
SELECT id, email, role, parent_id FROM users WHERE email = 'child@email.com';

-- Should show parent_id = (parent's user ID)

-- Check if device exists for child
SELECT * FROM devices WHERE user_id = (child's user ID);

-- Should show device with FCM token

-- Check if parent device exists
SELECT * FROM devices WHERE user_id = (parent's user ID);

-- Should show device with FCM token
```

### API Testing

Test pairing API directly:

```bash
# 1. Login as child
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"child@email.com","password":"password"}'

# Save JWT token from response

# 2. Pair with parent
curl -X POST http://localhost:8080/api/pair-device \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {child_jwt_token}" \
  -d '{
    "pairCode": "GIA-XXXXX",
    "deviceName": "Test Device",
    "deviceModel": "Test Model",
    "androidVersion": "13",
    "fcmToken": "test_token"
  }'

# Should return device object with ID

# 3. Login as parent
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"parent@email.com","password":"password"}'

# Save JWT token from response

# 4. Get child devices
curl -X GET http://localhost:8080/api/child-devices \
  -H "Authorization: Bearer {parent_jwt_token}"

# Should return array with child device
```

### Logs to Monitor

**Backend (Spring Boot):**
```
tail -f logs/spring-boot-application.log | grep -E "Pairing|CHILD_PAIRED|FCM"
```

**Android Parent:**
```
adb logcat | grep -E "ParentDashboard|GiaFcmService"
```

**Android Child:**
```
adb logcat | grep -E "ChildDashboard|LocationService"
```

### Quick Test Procedure

1. **Start backend**
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Install parent app**
   - Login as parent
   - Note pair code
   - Keep app open

3. **Install child app**
   - Login as child
   - Enter parent pair code
   - Click "Pair with Parent"
   - Should see success message

4. **Check parent app**
   - Should receive notification
   - Dashboard should update
   - If not, tap "No Device Paired" to refresh

5. **Test lock command**
   - Parent clicks "Lock"
   - Child should show lock screen
   - If not working, check logs

### Still Not Working?

If pairing still fails after all troubleshooting:

1. **Clear all data:**
   ```bash
   # Clear child app data
   adb shell pm clear com.gia.familycontrol
   
   # Clear parent app data
   adb shell pm clear com.gia.familycontrol
   
   # Clear database
   mysql -u root -p
   DROP DATABASE gia_family_control;
   CREATE DATABASE gia_family_control;
   USE gia_family_control;
   SOURCE database/schema.sql;
   ```

2. **Start fresh:**
   - Register new parent account
   - Register new child account
   - Pair devices
   - Test lock command

3. **Check versions:**
   - Backend: v38+
   - Android app: v38+
   - Both must be same version

4. **Verify Firebase:**
   - google-services.json in app/
   - firebase-service-account.json in backend/src/main/resources/
   - Both from same Firebase project
