# Connection Status & Battery Tracking - Deployment Guide

## ✅ What's New

### Parent Dashboard Now Shows:
1. **📶 Connection Status**
   - 📶 WiFi icon when child is on WiFi
   - 📱 Mobile Data icon when on cellular
   - ❌ Offline when disconnected

2. **🔋 Accurate Battery Percentage**
   - Real-time battery level from child device
   - Updates every 5 seconds

3. **⏰ Last Seen Timestamp**
   - Shows "Active now" when online
   - Shows last connection time when offline

4. **🟢 Online/Offline Indicator**
   - Green dot = Online
   - Red dot = Offline

---

## 🚀 Deployment Steps

### Step 1: Update Database (REQUIRED)
Run this SQL on your Railway MySQL database:

```sql
USE railway;

ALTER TABLE devices 
ADD COLUMN connection_type VARCHAR(20) DEFAULT 'OFFLINE' COMMENT 'WIFI, MOBILE_DATA, OFFLINE'
AFTER is_online;
```

**How to run:**
1. Go to Railway dashboard
2. Click on your MySQL service
3. Click "Query" tab
4. Paste the SQL above
5. Click "Run"

### Step 2: Deploy Backend
The backend changes are already pushed. Railway will auto-deploy.

Wait for deployment to complete (~2-3 minutes).

### Step 3: Install New APK
1. Go to: https://github.com/JedTipudan/gia-family-control/releases
2. Download the latest APK
3. Install on both parent and child devices

---

## 📱 How It Works

### Child Device (Automatic):
- Every 5 seconds, sends:
  - GPS location
  - Battery level (accurate %)
  - Connection type (WiFi/Mobile Data/Offline)
  - Online status

### Parent Dashboard (Real-time):
- Polls every 8 seconds
- Shows:
  - Battery: "85%" with 🔋 icon
  - Connection: "WiFi" with 📶 icon or "Mobile Data" with 📱 icon
  - Status: Green dot (online) or Red dot (offline)
  - Last Seen: "Active now" or "Last: 2 mins ago"

---

## 🧪 Testing

### Test Connection Status:
1. Open child device
2. Turn WiFi ON → Parent should see 📶 "WiFi"
3. Turn WiFi OFF (use mobile data) → Parent should see 📱 "Mobile Data"
4. Turn airplane mode ON → Parent should see ❌ "Offline"

### Test Battery:
1. Check child device battery: Settings → Battery
2. Parent dashboard should show same percentage within 5 seconds

### Test Last Seen:
1. Close child app completely
2. Wait 10 seconds
3. Parent should see "Offline" with last seen timestamp

---

## 🐛 Troubleshooting

**Battery shows "---%" ?**
- Child device not paired yet
- Child app not running
- Wait 5-10 seconds for first update

**Connection shows "Offline" but child is online?**
- Check child app is running
- Check location service is active
- Restart child app

**Last Seen not updating?**
- Backend not deployed yet
- Database migration not run
- Check Railway logs

---

## 📊 Database Schema Change

**Before:**
```
devices:
  - battery_level (always 0)
  - is_online (not used)
```

**After:**
```
devices:
  - battery_level (real-time %)
  - is_online (true/false)
  - connection_type (WIFI/MOBILE_DATA/OFFLINE) ← NEW
  - last_seen (timestamp) ← NOW USED
```

---

## 🎯 Next Steps

After deployment, you can:
1. Monitor child battery remotely
2. See if child is on WiFi or using mobile data
3. Know exactly when child was last online
4. Get alerts when child goes offline (future feature)

---

**Need help?** Check Railway logs or test locally first.
