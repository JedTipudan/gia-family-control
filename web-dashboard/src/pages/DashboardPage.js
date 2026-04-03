import React, { useState, useEffect, useCallback } from 'react';
import { GoogleMap, Marker, useJsApiLoader } from '@react-google-maps/api';
import { commandApi, locationApi } from '../services/api';
import { subscribeToDeviceLocation, subscribeToDeviceStatus } from '../services/firebase';
import { useAuth } from '../context/AuthContext';
import AppManagerPanel from '../components/AppManagerPanel';
import DeviceStatusBar from '../components/DeviceStatusBar';
import ActivityLog from '../components/ActivityLog';

const MAP_CONTAINER = { width: '100%', height: '400px' };
const DEFAULT_CENTER = { lat: 14.5995, lng: 120.9842 };

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const [childDeviceId] = useState(() => Number(localStorage.getItem('child_device_id') || 1));
  const [location, setLocation] = useState(null);
  const [deviceStatus, setDeviceStatus] = useState({});
  const [activeTab, setActiveTab] = useState('map');

  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_KEY || '',
  });

  useEffect(() => {
    const unsubLoc = subscribeToDeviceLocation(childDeviceId, setLocation);
    const unsubStatus = subscribeToDeviceStatus(childDeviceId, setDeviceStatus);
    return () => { unsubLoc(); unsubStatus(); };
  }, [childDeviceId]);

  // Fallback: poll REST API if Firebase not available
  useEffect(() => {
    const fetchLocation = async () => {
      try {
        const { data } = await locationApi.getLatest(childDeviceId);
        console.log('Location data:', data);
        if (data && data.latitude && data.longitude) {
          setLocation({ lat: data.latitude, lng: data.longitude });
        }
      } catch (err) {
        console.error('Failed to fetch location:', err);
      }
    };
    
    fetchLocation(); // Fetch immediately
    const interval = setInterval(fetchLocation, 5000); // Poll every 5 seconds
    return () => clearInterval(interval);
  }, [childDeviceId]);

  const sendCommand = async (type, packageName = null) => {
    try {
      await commandApi.send(childDeviceId, type, packageName);
      alert(`${type} command sent successfully`);
    } catch {
      alert('Failed to send command');
    }
  };

  return (
    <div style={styles.container}>
      <header style={styles.header}>
        <div style={styles.headerLeft}>
          <img src="/logo.jpg" alt="Gia" style={styles.logo} />
          <span style={styles.title}>Gia Family Control</span>
        </div>
        <div style={styles.headerRight}>
          <span style={styles.userName}>{user?.fullName}</span>
          <button style={styles.logoutBtn} onClick={logout}>Logout</button>
        </div>
      </header>

      <DeviceStatusBar deviceId={childDeviceId} status={deviceStatus} />

      <div style={styles.controls}>
        <button style={{...styles.ctrlBtn, background: '#ef4444'}} onClick={() => sendCommand('LOCK')}>
          🔒 Lock Device
        </button>
        <button style={{...styles.ctrlBtn, background: '#22c55e'}} onClick={() => sendCommand('UNLOCK')}>
          🔓 Unlock Device
        </button>
        <button style={{...styles.ctrlBtn, background: '#f59e0b'}} onClick={() => setActiveTab('apps')}>
          📱 Manage Apps
        </button>
        <button style={{...styles.ctrlBtn, background: '#8b5cf6'}} onClick={() => setActiveTab('logs')}>
          📋 Activity Logs
        </button>
      </div>

      <div style={styles.tabs}>
        {['map', 'apps', 'logs'].map(tab => (
          <button key={tab} style={{...styles.tab, ...(activeTab === tab ? styles.activeTab : {})}}
            onClick={() => setActiveTab(tab)}>
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>

      {activeTab === 'map' && (
        <div style={styles.mapContainer}>
          {!location && <p style={styles.noLocation}>⏳ Waiting for location data from child device...</p>}
          {isLoaded ? (
            <GoogleMap mapContainerStyle={MAP_CONTAINER}
              center={location ? { lat: location.lat, lng: location.lng } : DEFAULT_CENTER}
              zoom={16}>
              {location && (
                <Marker position={{ lat: location.lat, lng: location.lng }}
                  title="Child's Location" />
              )}
            </GoogleMap>
          ) : <div style={styles.mapPlaceholder}>Loading map...</div>}
          {location && (
            <p style={styles.coords}>
              📍 Lat: {location.lat?.toFixed(6)}, Lng: {location.lng?.toFixed(6)}
            </p>
          )}
        </div>
      )}

      {activeTab === 'apps' && (
        <AppManagerPanel deviceId={childDeviceId} onBlockApp={(pkg) => sendCommand('BLOCK_APP', pkg)} />
      )}

      {activeTab === 'logs' && <ActivityLog deviceId={childDeviceId} />}
    </div>
  );
}

const styles = {
  container: { minHeight: '100vh', background: '#f8fafc', fontFamily: 'sans-serif' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    background: '#1a1a2e', color: '#fff', padding: '12px 24px' },
  headerLeft: { display: 'flex', alignItems: 'center', gap: 12 },
  logo: { width: 40, height: 40, borderRadius: '50%' },
  title: { fontSize: 20, fontWeight: 'bold' },
  headerRight: { display: 'flex', alignItems: 'center', gap: 16 },
  userName: { fontSize: 14 },
  logoutBtn: { padding: '6px 16px', background: '#ef4444', color: '#fff',
    border: 'none', borderRadius: 6, cursor: 'pointer' },
  controls: { display: 'flex', gap: 12, padding: '16px 24px', flexWrap: 'wrap' },
  ctrlBtn: { padding: '10px 20px', color: '#fff', border: 'none', borderRadius: 8,
    cursor: 'pointer', fontSize: 14, fontWeight: 'bold' },
  tabs: { display: 'flex', gap: 0, padding: '0 24px', borderBottom: '2px solid #e2e8f0' },
  tab: { padding: '10px 24px', background: 'none', border: 'none', cursor: 'pointer',
    fontSize: 14, color: '#64748b' },
  activeTab: { borderBottom: '2px solid #4f46e5', color: '#4f46e5', fontWeight: 'bold' },
  mapContainer: { padding: 24 },
  mapPlaceholder: { height: 400, display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: '#e2e8f0', borderRadius: 8 },
  noLocation: { padding: 16, textAlign: 'center', color: '#64748b', fontSize: 14 },
  coords: { marginTop: 8, color: '#64748b', fontSize: 13 },
};
