import React, { useState, useEffect } from 'react';
import { GoogleMap, Marker, useJsApiLoader } from '@react-google-maps/api';
import { commandApi, locationApi } from '../services/api';
import { subscribeToDeviceLocation, subscribeToDeviceStatus } from '../services/firebase';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import AppManagerPanel from '../components/AppManagerPanel';
import DeviceStatusBar from '../components/DeviceStatusBar';
import ActivityLog from '../components/ActivityLog';

const MAP_CONTAINER = { width: '100%', height: '100%' };
const DEFAULT_CENTER = { lat: 14.5995, lng: 120.9842 };

const MAP_DARK_STYLE = [
  { elementType: 'geometry', stylers: [{ color: '#0f1011' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#0f1011' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#62666d' }] },
  { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#191a1b' }] },
  { featureType: 'road', elementType: 'geometry.stroke', stylers: [{ color: '#23252a' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#08090a' }] },
  { featureType: 'poi', stylers: [{ visibility: 'off' }] },
  { featureType: 'transit', stylers: [{ visibility: 'off' }] },
];

const TABS = [
  { id: 'map',  label: 'Live Map',    icon: '📍' },
  { id: 'apps', label: 'App Controls', icon: '📱' },
  { id: 'logs', label: 'Activity Log', icon: '📋' },
];

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const [childDeviceId] = useState(() => Number(localStorage.getItem('child_device_id') || 1));
  const [location, setLocation] = useState(null);
  const [deviceStatus, setDeviceStatus] = useState({});
  const [activeTab, setActiveTab] = useState('map');
  const [cmdLoading, setCmdLoading] = useState(null);

  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_KEY || '',
  });

  useEffect(() => {
    const unsubLoc = subscribeToDeviceLocation(childDeviceId, setLocation);
    const unsubStatus = subscribeToDeviceStatus(childDeviceId, setDeviceStatus);
    return () => { unsubLoc(); unsubStatus(); };
  }, [childDeviceId]);

  useEffect(() => {
    const fetchLocation = async () => {
      try {
        const { data } = await locationApi.getLatest(childDeviceId);
        if (data?.latitude && data?.longitude)
          setLocation({ lat: data.latitude, lng: data.longitude });
      } catch {}
    };
    fetchLocation();
    const id = setInterval(fetchLocation, 5000);
    return () => clearInterval(id);
  }, [childDeviceId]);

  const sendCommand = async (type, packageName = null) => {
    setCmdLoading(type);
    try {
      await commandApi.send(childDeviceId, type, packageName);
    } catch {
      alert('Failed to send command');
    } finally {
      setCmdLoading(null);
    }
  };

  return (
    <div style={s.shell}>
      {/* Sidebar */}
      <aside style={s.sidebar}>
        <div style={s.sideTop}>
          <div style={s.brand}>
            <img src="/logo.jpg" alt="Gia" style={s.brandLogo} />
            <span style={s.brandName}>Gia</span>
          </div>
          <nav style={s.nav}>
            {TABS.map(tab => (
              <button
                key={tab.id}
                style={{ ...s.navItem, ...(activeTab === tab.id ? s.navActive : {}) }}
                onClick={() => setActiveTab(tab.id)}
              >
                <span style={s.navIcon}>{tab.icon}</span>
                <span>{tab.label}</span>
              </button>
            ))}
          </nav>
        </div>
        <div style={s.sideBottom}>
          <button style={s.themeBtn} onClick={toggleTheme}>
            {isDark ? '☀️' : '🌙'} {isDark ? 'Light' : 'Dark'} Mode
          </button>
          <div style={s.userRow}>
            <div style={s.avatar}>{user?.fullName?.[0]?.toUpperCase() || 'P'}</div>
            <div style={s.userInfo}>
              <span style={s.userName}>{user?.fullName || 'Parent'}</span>
              <span style={s.userRole}>Parent</span>
            </div>
          </div>
          <button style={s.logoutBtn} onClick={logout}>Sign out</button>
        </div>
      </aside>

      {/* Main */}
      <main style={s.main}>
        {/* Top bar */}
        <div style={s.topbar}>
          <div>
            <h1 style={s.pageTitle}>{TABS.find(t => t.id === activeTab)?.label}</h1>
          </div>
          <div style={s.cmdRow}>
            <CmdButton label="Lock" icon="🔒" color="#ef4444" loading={cmdLoading === 'LOCK'}
              onClick={() => sendCommand('LOCK')} />
            <CmdButton label="Unlock" icon="🔓" color="#10b981" loading={cmdLoading === 'UNLOCK'}
              onClick={() => sendCommand('UNLOCK')} />
          </div>
        </div>

        <DeviceStatusBar deviceId={childDeviceId} status={deviceStatus} />

        {/* Content */}
        <div style={s.content}>
          {activeTab === 'map' && (
            <div style={s.mapWrap}>
              {!location && (
                <div style={s.mapOverlay}>
                  <span style={s.mapOverlayText}>⏳ Waiting for location data…</span>
                </div>
              )}
              {isLoaded ? (
                <GoogleMap
                  mapContainerStyle={MAP_CONTAINER}
                  center={location ? { lat: location.lat, lng: location.lng } : DEFAULT_CENTER}
                  zoom={16}
                  options={{ styles: MAP_DARK_STYLE, disableDefaultUI: false, zoomControl: true }}
                >
                  {location && (
                    <Marker
                      position={{ lat: location.lat, lng: location.lng }}
                      title="Child's Location"
                    />
                  )}
                </GoogleMap>
              ) : (
                <div style={s.mapLoading}>Loading map…</div>
              )}
              {location && (
                <div style={s.coordsBar}>
                  <span style={s.coordsLabel}>📍 Live Location</span>
                  <span style={s.coords}>
                    {location.lat?.toFixed(6)}, {location.lng?.toFixed(6)}
                  </span>
                </div>
              )}
            </div>
          )}
          {activeTab === 'apps' && (
            <AppManagerPanel deviceId={childDeviceId} onBlockApp={(pkg) => sendCommand('BLOCK_APP', pkg)} />
          )}
          {activeTab === 'logs' && <ActivityLog deviceId={childDeviceId} />}
        </div>
      </main>
    </div>
  );
}

function CmdButton({ label, icon, color, loading, onClick }) {
  return (
    <button
      style={{
        ...s.cmdBtn,
        background: `${color}18`,
        color,
        borderColor: `${color}33`,
        opacity: loading ? 0.6 : 1,
      }}
      onClick={onClick}
      disabled={loading}
    >
      {icon} {loading ? '…' : label}
    </button>
  );
}

const s = {
  shell: {
    display: 'flex', minHeight: '100vh',
    background: 'var(--bg-primary)',
    fontFamily: "'Inter', -apple-system, system-ui, sans-serif",
    color: 'var(--text-primary)',
  },
  sidebar: {
    width: 220, flexShrink: 0,
    background: 'var(--bg-secondary)',
    borderRight: '1px solid var(--border-subtle)',
    display: 'flex', flexDirection: 'column',
    justifyContent: 'space-between',
    padding: '20px 0',
    position: 'sticky', top: 0, height: '100vh',
  },
  sideTop: { display: 'flex', flexDirection: 'column', gap: 28 },
  brand: { display: 'flex', alignItems: 'center', gap: 10, padding: '0 16px' },
  brandLogo: { width: 28, height: 28, borderRadius: '50%', border: '1px solid var(--border-subtle)' },
  brandName: { fontSize: 15, fontWeight: 590, color: 'var(--text-primary)', letterSpacing: '-0.165px' },
  nav: { display: 'flex', flexDirection: 'column', gap: 2, padding: '0 8px' },
  navItem: {
    display: 'flex', alignItems: 'center', gap: 10,
    padding: '8px 10px', borderRadius: 6,
    background: 'none', border: 'none', cursor: 'pointer',
    fontSize: 13, fontWeight: 510, color: 'var(--text-tertiary)',
    textAlign: 'left', width: '100%',
    transition: 'background 0.12s, color 0.12s',
    fontFamily: 'inherit',
  },
  navActive: {
    background: 'var(--border-subtle)',
    color: 'var(--text-primary)',
  },
  navIcon: { fontSize: 14, width: 18, textAlign: 'center' },
  sideBottom: { padding: '0 12px', display: 'flex', flexDirection: 'column', gap: 10 },
  themeBtn: {
    padding: '7px 12px',
    background: 'var(--bg-elevated)',
    border: '1px solid var(--border-subtle)',
    borderRadius: 6, fontSize: 12, fontWeight: 510,
    color: 'var(--text-tertiary)', cursor: 'pointer', fontFamily: 'inherit',
    textAlign: 'center', transition: 'all 0.15s',
  },
  userRow: { display: 'flex', alignItems: 'center', gap: 10, padding: '8px 4px' },
  avatar: {
    width: 28, height: 28, borderRadius: '50%',
    background: 'var(--accent-primary)', color: '#fff',
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    fontSize: 12, fontWeight: 590, flexShrink: 0,
  },
  userInfo: { display: 'flex', flexDirection: 'column', gap: 1, minWidth: 0 },
  userName: { fontSize: 13, fontWeight: 510, color: 'var(--text-secondary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  userRole: { fontSize: 11, color: 'var(--text-quaternary)' },
  logoutBtn: {
    padding: '7px 12px',
    background: 'var(--bg-elevated)',
    border: '1px solid var(--border-subtle)',
    borderRadius: 6, fontSize: 12, fontWeight: 510,
    color: 'var(--text-tertiary)', cursor: 'pointer', fontFamily: 'inherit',
    textAlign: 'center',
  },
  main: { flex: 1, display: 'flex', flexDirection: 'column', minWidth: 0 },
  topbar: {
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '16px 24px',
    borderBottom: '1px solid var(--border-subtle)',
    background: 'var(--bg-secondary)',
  },
  pageTitle: { margin: 0, fontSize: 18, fontWeight: 590, color: 'var(--text-primary)', letterSpacing: '-0.24px' },
  cmdRow: { display: 'flex', gap: 8 },
  cmdBtn: {
    padding: '7px 14px', borderRadius: 6,
    border: '1px solid', fontSize: 13, fontWeight: 510,
    cursor: 'pointer', fontFamily: 'inherit',
    transition: 'opacity 0.15s',
  },
  content: { flex: 1, overflow: 'auto' },
  mapWrap: {
    position: 'relative',
    height: 'calc(100vh - 120px)',
    background: 'var(--bg-secondary)',
  },
  mapOverlay: {
    position: 'absolute', inset: 0, zIndex: 10,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: 'rgba(8,9,10,0.6)', pointerEvents: 'none',
  },
  mapOverlayText: { fontSize: 14, color: 'var(--text-tertiary)' },
  mapLoading: {
    height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: 'var(--text-quaternary)', fontSize: 14,
  },
  coordsBar: {
    position: 'absolute', bottom: 16, left: '50%', transform: 'translateX(-50%)',
    display: 'flex', alignItems: 'center', gap: 10,
    padding: '8px 16px',
    background: 'var(--bg-elevated)',
    border: '1px solid var(--border-subtle)',
    borderRadius: 8, backdropFilter: 'blur(8px)',
    zIndex: 5,
  },
  coordsLabel: { fontSize: 12, fontWeight: 510, color: 'var(--text-secondary)' },
  coords: { fontSize: 12, color: 'var(--text-tertiary)', fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace' },
};
