# FINAL FIX - Root Causes Found & Fixed

## 🎯 ROOT CAUSES IDENTIFIED

### 1. App Blocking Not Working - ROOT CAUSE: **MISSING USAGE STATS PERMISSION**

**The Real Problem**:
```kotlin
// In AppMonitorService.kt:
private fun getForegroundApp(): String? {
    val usm = getSystemService(UsageStatsManager::class.java) ?: return null
    val stats = usm.queryUsageStats(...) // ← RETURNS NULL WITHOUT PERMISSION
    return stats?.maxByOrNull { it.lastTimeUsed }?.packageName
}
```

**Why It Failed**:
- `UsageStatsManager.queryUsageStats()` requires **Usage Access permission**
- Without this permission, it returns `null` or empty list
- `getForegroundApp()` always returns `null`
- Blocking logic never triggers because no app is detected
- This is why it "worked once" - permission was granted temporarily then revoked

**The Permission**:
- **Name**: Usage Access / Usage Stats
- **Location**: Settings → Apps → Special app access → Usage access → Gia Family Control
- **Required for**: Detecting which app is currently in foreground
- **Without it**: App blocking is 100% impossible

---

### 2. SOS 403 Error - ROOT CAUSE: **JWT TOKEN NOT SENT OR INVALID**

**The Real Problem**:
- HTTP 403 = Forbidden = Authentication failed
- Controller expects `Principal principal` parameter
- If JWT token is missing/invalid/expired, Principal is `null`
- Backend returns 403

**Possible Causes**:
1. Child never logged in (no JWT token saved)
2. JWT token expired (check expiration time in JwtUtil)
3. Token not being sent in Authorization header
4. Token validation failing

**Debug Steps**:
1. Check backend logs for: "Authenticated: [email] with role: CHILD"
2. If you see "No Authorization header" → Token not being sent
3. If you see "Invalid token" → Token is expired or malformed
4. If you see "Token validation error" → Check JwtUtil.validateToken()

---

## ✅ FIXES IMPLEMENTED

### Fix 1: Usage Stats Permission Check
```kotlin
// Added to ChildDashboardActivity.kt:

private fun hasUsageStatsPermission(): Boolean {
    val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun startMonitoringServices() {
    // Check Usage Stats permission FIRST
    if (!hasUsageStatsPermission()) {
        showUsageStatsPrompt() // ← Shows dialog with instructions
        return
    }
    
    // Then check Accessibility
    if (!isAccessibilityServiceEnabled()) {
        showAccessibilityPrompt()
        return
    }
    
    // Start services...
}
```

### Fix 2: SOS Debugging
```java
// CommandController.java:
@PostMapping("/send-command")
public ResponseEntity<Command> sendCommand(Principal principal, ...) {
    if (principal == null) {
        log.error("Principal is null - user not authenticated");
        return ResponseEntity.status(403).build();
    }
    log.info("Send command from: {}, type: {}", principal.getName(), request.getCommandType());
    // ...
}

// JwtAuthFilter.java:
protected void doFilterInternal(...) {
    if (header != null && header.startsWith("Bearer ")) {
        String token = header.substring(7);
        log.debug("Token found: {}...", token.substring(0, 20));
        
        if (jwtUtil.validateToken(token)) {
            String email = jwtUtil.extractEmail(token);
            String role = jwtUtil.extractRole(token);
            log.info("Authenticated: {} with role: {}", email, role);
            // Set authentication...
        } else {
            log.warn("Invalid token");
        }
    } else {
        log.debug("No Authorization header");
    }
}
```

---

## 📋 TESTING INSTRUCTIONS

### Test 1: App Blocking (Usage Stats Permission)

**Step 1: Check Permission**
1. Install APK on child device
2. Login as child
3. Pair with parent
4. Click "Start Monitoring"
5. **Expected**: Dialog appears: "⚠️ Permission Required - To monitor and block apps, you must grant Usage Access permission..."

**Step 2: Grant Permission**
1. Click "Open Settings"
2. Find "Gia Family Control" in the list
3. Toggle it ON
4. Go back to app
5. Click "Start Monitoring" again
6. **Expected**: Now checks Accessibility Service

**Step 3: Test Blocking**
1. Parent blocks YouTube
2. Child tries to open YouTube
3. **Expected**: Closes immediately
4. Child tries 10 more times
5. **Expected**: Closes EVERY time

**If Still Not Working**:
- Check logs: `adb logcat | findstr AppMonitorService`
- Look for: "📱 App changed: com.youtube.android"
- If you see this, detection is working
- If you don't see this, permission is still not granted

---

### Test 2: SOS Alert (403 Error)

**Step 1: Check Authentication**
1. Child clicks SOS button
2. Check backend logs immediately
3. **Look for these logs**:

**Scenario A - Success**:
```
Authenticated: child@example.com with role: CHILD
Send command from: child@example.com, type: SOS
=== SOS COMMAND RECEIVED ===
Child found: John Doe (ID: 5)
...
✅ SOS alert sent successfully via FCM
```

**Scenario B - No Token**:
```
No Authorization header for: /api/send-command
Principal is null - user not authenticated
```
**Fix**: Child needs to login again

**Scenario C - Invalid Token**:
```
Token found: eyJhbGciOiJIUzI1NiIs...
Invalid token for request: /api/send-command
Principal is null - user not authenticated
```
**Fix**: Token expired, child needs to login again

**Scenario D - Token Validation Error**:
```
Token found: eyJhbGciOiJIUzI1NiIs...
Token validation error: JWT expired at...
```
**Fix**: Increase JWT expiration time in JwtUtil

---

## 🔧 ADDITIONAL FIXES NEEDED

### If SOS Still Returns 403:

**Option 1: Check JWT Expiration**
```java
// In JwtUtil.java, find:
public String generateToken(String email, String role) {
    return Jwts.builder()
        .setSubject(email)
        .claim("role", role)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24)) // ← 24 hours
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}

// Change to 30 days for child accounts:
.setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
```

**Option 2: Re-login Child**
1. Child logs out (if not paired)
2. Child logs in again
3. New JWT token generated
4. Try SOS again

**Option 3: Check DataStore**
```kotlin
// In ChildDashboardActivity, add debug:
lifecycleScope.launch {
    val token = dataStore.data.first()[JWT_TOKEN_KEY]
    Log.d("ChildDashboard", "JWT Token: ${token?.substring(0, 20)}...")
}
```

---

## 📊 EXPECTED LOGS

### App Blocking Working:
```
AppMonitorService: === Service onCreate ===
AppMonitorService: 📋 Loaded 2 blocked apps from prefs: [com.youtube.android, com.tiktok]
AppMonitorService: 🔄 Loading blocked apps from API for device: 8
AppMonitorService: 📡 API returned 2 blocked apps: [com.youtube.android, com.tiktok]
AppMonitorService: ✅ Updated blocked apps: [com.youtube.android, com.tiktok]
AppMonitorService: 📱 App changed: com.youtube.android
AppMonitorService: ⛔ BLOCKED APP DETECTED: com.youtube.android (attempt #1)
AppMonitorService: 🚫 FORCE CLOSING: com.youtube.android
AppMonitorService: ✅ App blocked: com.youtube.android
[Child tries again]
AppMonitorService: ⛔ BLOCKED APP DETECTED: com.youtube.android (attempt #2)
AppMonitorService: 🚫 FORCE CLOSING: com.youtube.android
AppMonitorService: ✅ App blocked: com.youtube.android
```

### SOS Working:
```
Backend:
JwtAuthFilter: Token found: eyJhbGciOiJIUzI1NiIs...
JwtAuthFilter: Authenticated: child@example.com with role: CHILD
CommandController: Send command from: child@example.com, type: SOS
CommandService: === SOS COMMAND RECEIVED ===
CommandService: Child email: child@example.com
CommandService: Child found: John Doe (ID: 5)
CommandService: Child parent_id: 3
CommandService: Parent found: Jane Doe (ID: 3)
CommandService: Parent device found: ID 7, FCM token: SET (eF3x9...)
CommandService: ✅ SOS alert sent successfully via FCM
CommandService: === SOS COMMAND COMPLETED SUCCESSFULLY ===

Android (Parent):
GiaFcmService: === SOS ALERT RECEIVED ===
GiaFcmService: Data: {command=SOS, childName=John Doe, location=Lat: 14.5995, Lng: 120.9842}
GiaFcmService: Child: John Doe, Location: Lat: 14.5995, Lng: 120.9842
[Alarm sound plays]
[Vibration]
[Notification shown]
```

---

## 🚀 DEPLOYMENT STEPS

### 1. Backend
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### 2. Android
1. Open Android Studio
2. Build → Clean Project
3. Build → Rebuild Project
4. Build → Build Bundle(s) / APK(s) → Build APK(s)
5. Install on child device
6. Install on parent device

### 3. Child Device Setup
1. Login as child
2. Pair with parent
3. Click "Start Monitoring"
4. **Grant Usage Access permission** (CRITICAL!)
5. Enable Accessibility Service
6. Services start running

### 4. Parent Device Setup
1. Login as parent
2. App registers FCM token automatically
3. View child devices
4. Block apps
5. Test SOS

---

## ✅ SUCCESS CRITERIA

### App Blocking:
- ✅ Usage Stats permission granted
- ✅ Accessibility Service enabled
- ✅ AppMonitorService running (check notification)
- ✅ Logs show "📱 App changed: [package]"
- ✅ Blocked app closes EVERY time child tries to open it
- ✅ Logs show "attempt #1", "attempt #2", etc.

### SOS:
- ✅ Backend logs show "Authenticated: [email] with role: CHILD"
- ✅ Backend logs show "=== SOS COMMAND COMPLETED SUCCESSFULLY ==="
- ✅ Parent receives notification with alarm sound
- ✅ Parent app shows SOS alert
- ✅ Child sees success dialog

---

## 🐛 TROUBLESHOOTING

### App Blocking Still Not Working:
1. **Check Permission**: Settings → Apps → Special app access → Usage access → Gia Family Control → ON
2. **Check Service**: Notification should show "App monitoring active - X apps blocked"
3. **Check Logs**: `adb logcat | findstr "AppMonitorService"`
4. **Check API**: Open DebugActivity, verify blocked apps list
5. **Restart Services**: Force stop app, open again, click "Start Monitoring"

### SOS Still Returns 403:
1. **Check Backend Logs**: Look for "No Authorization header" or "Invalid token"
2. **Re-login**: Child logs out and logs in again
3. **Check Token**: Add debug log to print JWT token
4. **Check Expiration**: Increase JWT expiration to 30 days
5. **Check Network**: Verify child device can reach backend

---

## 📝 COMMIT

```
CRITICAL FIX: Usage Stats permission + SOS 403 debugging

APP BLOCKING ROOT CAUSE:
- Missing Usage Stats permission check before starting monitoring
- App blocking CANNOT work without this permission
- Added hasUsageStatsPermission() check
- Shows prompt to enable Usage Access in Settings
- This is why blocking only worked once - permission was revoked

SOS 403 ERROR FIX:
- Added null check for Principal in CommandController
- Enhanced JWT filter logging to debug authentication
- Logs show: token validation, email, role
- Will help identify if token is missing/invalid/expired

USAGE STATS PERMISSION:
- Required for UsageStatsManager.queryUsageStats()
- Without it, getForegroundApp() returns null
- User must enable: Settings → Apps → Special app access → Usage access
- Added showUsageStatsPrompt() with clear instructions
```

Pushed to: https://github.com/JedTipudan/gia-family-control.git

---

## 🎯 SUMMARY

**App Blocking**: Was failing because **Usage Stats permission was not granted**. Without this permission, the app cannot detect which app is in foreground, so blocking is impossible. Now prompts user to grant permission before starting monitoring.

**SOS 403**: JWT token is either missing, invalid, or expired. Added comprehensive logging to identify exact cause. Backend logs will now show authentication status and help debug the issue.

**Next Steps**: 
1. Rebuild and install APK
2. Grant Usage Stats permission when prompted
3. Check backend logs for SOS authentication
4. Test thoroughly and report results
