# Gia Family Control — System Architecture

## Text-Based Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                        GIA FAMILY CONTROL                           │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐         ┌──────────────────┐
│   PARENT DEVICE  │         │   CHILD DEVICE   │
│  (Android App)   │         │  (Android App)   │
│                  │         │                  │
│ • View Map       │         │ • Location Svc   │
│ • Lock Device    │         │ • App Monitor    │
│ • Block Apps     │         │ • FCM Receiver   │
│ • View Logs      │         │ • Lock Overlay   │
│ • Geofence Mgmt  │         │ • SOS Button     │
└────────┬─────────┘         └────────┬─────────┘
         │  HTTPS/JWT                 │  HTTPS/JWT
         ▼                            ▼
┌─────────────────────────────────────────────────┐
│              SPRING BOOT BACKEND                │
│                  (Java 17)                      │
│                                                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │   Auth   │  │ Location │  │  App Control │  │
│  │Controller│  │Controller│  │  Controller  │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
│  ┌──────────┐  ┌──────────┐  ┌──────────────┐  │
│  │  Device  │  │ Command  │  │  Geofence    │  │
│  │Controller│  │Controller│  │  Controller  │  │
│  └──────────┘  └──────────┘  └──────────────┘  │
│                                                 │
│  ┌─────────────────────────────────────────┐    │
│  │         Security Layer (JWT)            │    │
│  └─────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────┐    │
│  │       Firebase Admin SDK Service        │    │
│  └─────────────────────────────────────────┘    │
└──────────────────────┬──────────────────────────┘
                       │
          ┌────────────┴────────────┐
          ▼                         ▼
┌──────────────────┐     ┌──────────────────────┐
│   MySQL Database │     │   Firebase Services  │
│                  │     │                      │
│ • users          │     │ • FCM (push cmds)    │
│ • devices        │     │ • Realtime DB        │
│ • locations      │     │   (live location)    │
│ • commands       │     │ • App state sync     │
│ • apps           │     └──────────────────────┘
│ • app_controls   │
│ • geofences      │
└──────────────────┘
          ▲
          │  HTTPS
          ▼
┌──────────────────────────────────────┐
│        REACT WEB DASHBOARD           │
│                                      │
│ • Live Map (Google Maps)             │
│ • App Management Panel               │
│ • Device Status & Logs               │
│ • User & Device Management           │
│ • Geofence Configuration             │
└──────────────────────────────────────┘
```

## Data Flow

1. Child device → sends GPS every 5-10s → Backend API + Firebase Realtime DB
2. Parent sends command → Backend validates JWT → Firebase FCM → Child device
3. Child FCM receiver → executes lock/block → confirms back to backend
4. Web dashboard → polls/subscribes Firebase → shows live updates

## Security Layers

- JWT authentication on all API endpoints
- Parent-only command authority enforced on backend
- HTTPS for all communication
- Device pairing via unique one-time code
- Android Device Admin for tamper prevention
