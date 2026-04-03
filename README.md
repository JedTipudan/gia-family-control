# Gia Family Control

A secure, production-ready parental control system for Android, with a Spring Boot backend and React web dashboard.

---

## System Architecture

```
┌──────────────────┐         ┌──────────────────┐
│   PARENT DEVICE  │         │   CHILD DEVICE   │
│  (Android App)   │         │  (Android App)   │
│ • View Map       │         │ • Location Svc   │
│ • Lock Device    │         │ • App Monitor    │
│ • Block Apps     │         │ • FCM Receiver   │
│ • View Logs      │         │ • Lock Overlay   │
└────────┬─────────┘         └────────┬─────────┘
         │  HTTPS/JWT                 │  HTTPS/JWT
         ▼                            ▼
┌─────────────────────────────────────────────────┐
│           SPRING BOOT BACKEND (Java 17)         │
│  Auth │ Location │ Commands │ App Control       │
│  JWT Security Layer + Firebase Admin SDK        │
└──────────────────┬──────────────────────────────┘
                   │
       ┌───────────┴───────────┐
       ▼                       ▼
┌──────────────┐     ┌──────────────────────┐
│ MySQL DB     │     │ Firebase             │
│ users        │     │ • FCM (commands)     │
│ devices      │     │ • Realtime DB        │
│ locations    │     │   (live location)    │
│ commands     │     └──────────────────────┘
│ app_controls │
│ geofences    │
└──────────────┘
       ▲
       │ HTTPS
       ▼
┌──────────────────────────────────┐
│     REACT WEB DASHBOARD          │
│ Live Map │ App Manager │ Logs    │
└──────────────────────────────────┘
```

---

## Project Structure

```
Gia Family Control/
├── backend/                    # Spring Boot (Java 17)
│   └── src/main/java/com/gia/familycontrol/
│       ├── controller/         # REST API endpoints
│       ├── service/            # Business logic
│       ├── model/              # JPA entities
│       ├── repository/         # Spring Data JPA
│       ├── security/           # JWT filter + util
│       ├── firebase/           # FCM service
│       └── config/             # Security config
├── android-app/                # Kotlin Android App
│   └── app/src/main/
│       ├── java/com/gia/familycontrol/
│       │   ├── auth/           # Login, Register
│       │   ├── service/        # Location, AppMonitor, FCM
│       │   ├── ui/parent/      # Parent dashboard, App manager
│       │   ├── ui/child/       # Child dashboard, Lock screen, Block overlay
│       │   ├── admin/          # Device Admin receiver
│       │   ├── receiver/       # Boot receiver
│       │   ├── network/        # Retrofit client + API service
│       │   └── model/          # Data models
│       └── res/                # Layouts, icons, XML
├── web-dashboard/              # React 18
│   └── src/
│       ├── pages/              # Login, Dashboard
│       ├── components/         # AppManagerPanel, DeviceStatusBar, ActivityLog
│       ├── services/           # Axios API, Firebase
│       └── context/            # Auth context
└── database/
    └── schema.sql              # MySQL schema
```

---

## Setup Guide

### 1. Database
```sql
mysql -u root -p < database/schema.sql
```

### 2. Backend (Spring Boot)
```bash
cd backend
# Set environment variables:
# DB_USERNAME, DB_PASSWORD, JWT_SECRET, FIREBASE_CREDENTIALS
mvn spring-boot:run
```
Place your Firebase service account JSON at:
`backend/src/main/resources/firebase-service-account.json`

### 3. Android App
1. Open `android-app/` in Android Studio
2. Add your `google-services.json` to `app/`
3. Update `BASE_URL` in `RetrofitClient.kt` to your backend URL
4. Add Google Maps API key to `AndroidManifest.xml` meta-data
5. Build and install on device

### 4. Web Dashboard
```bash
cd web-dashboard
cp .env.example .env
# Fill in your API URL, Firebase config, Google Maps key
npm install
npm start
```

---

## API Endpoints

| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | /api/auth/register | Public | Register user |
| POST | /api/auth/login | Public | Login, get JWT |
| POST | /api/pair-device | Child JWT | Pair child with parent |
| PUT | /api/device/status | Child JWT | Update battery/online status |
| POST | /api/location/update | Child JWT | Send GPS location |
| GET | /api/location/{id}/latest | JWT | Get latest location |
| GET | /api/location/{id}/history | JWT | Get location history |
| POST | /api/send-command | Parent JWT | Lock/Unlock/SOS |
| GET | /api/commands/{deviceId} | JWT | Command history |
| GET | /api/apps/controls/{deviceId} | JWT | Get app rules |
| POST | /api/apps/control | Parent JWT | Block/Allow app |
| DELETE | /api/apps/control/{id}/{pkg} | Parent JWT | Remove app rule |

---

## Key Android Features

### Location Tracking
- `LocationTrackingService` — Foreground service, sends GPS every 5-10s
- Uses `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY`
- Persists through device reboot via `BootReceiver`

### App Monitoring & Blocking
- `AppMonitorService` — Polls `UsageStatsManager` every 1 second
- Compares foreground app against blocked list from backend
- Shows `AppBlockOverlayActivity` fullscreen when blocked app detected

### Remote Lock
- `GiaFcmService` receives FCM `LOCK` command
- Launches `LockScreenActivity` fullscreen, blocks all hardware keys
- Parent sends `UNLOCK` via FCM to dismiss

### Device Admin
- `GiaDeviceAdminReceiver` prevents easy uninstall
- Registered via `DevicePolicyManager`

---

## Security Notes

- All API calls use HTTPS + JWT Bearer tokens
- Parent-only commands enforced on backend (role check + device ownership check)
- Persistent notification shown on child device ("Device is being monitored")
- Device Admin prevents easy removal
- Explicit consent required during pairing flow
- Follows Android parental control policies (no hidden/spyware behavior)

---

## Icon
The `icon.jpg` from `C:\Users\Jed\Downloads\icon.jpg` has been:
- Copied to all Android mipmap density folders (see `android-app/ICON_SETUP.md`)
- Used as the web dashboard logo (`web-dashboard/public/logo.jpg`)

For proper Android launcher icon generation, see `android-app/ICON_SETUP.md`.
