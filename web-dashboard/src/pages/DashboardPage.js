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

const TABS = [
  { id: 'map', label: 'Live Map', icon: '📍' },
  { id: 'apps', label: 'App Controls', icon: '📱' },
  { id: 'logs', label: 'Activity', icon: '📋' },
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
      {/* Navigation */}
      <nav style={s.nav}>
        <div style={s.navContent}>
          <div style={s.navLeft}>
            <img src="/logo.jpg" alt="Gia" style={s.navLogo} />
            <span style={s.navBrand}>Gia</span>
          </div>
          <div style={s.navCenter}>
            {TABS.map(tab => (
              <button
                key={tab.id}
                style={{ ...s.navLink, ...(activeTab === tab.id ? s.navLinkActive : {}) }}
                onClick={() => setActiveTab(tab.id)}
              >
                {tab.label}
              </button>
            ))}
          </div>
          <div style={s.navRight}>
            <button style={s.themeToggle} onClick={toggleTheme}>
              {isDark ? '☀️' : '🌙'}
            </button>
            <button style={s.logoutBtn} onClick={logout}>Sign out</button>
          </div>
        </div>
      </nav>

      <DeviceStatusBar deviceId={childDeviceId} status={deviceStatus} />

      {/* Hero Section */}
      <section style={s.hero}>
        <div style={s.heroContent}>
          <h1 style={s.heroTitle}>Family Safety.<br/>Simplified.</h1>
          <p style={s.heroSub}>Monitor, protect, and stay connected with your family.</p>
          <div style={s.heroActions}>
            <CmdButton 
              label="Lock Device" 
              icon="🔒" 
              variant="danger"
              loading={cmdLoading === 'LOCK'}
              onClick={() => sendCommand('LOCK')} 
            />
            <CmdButton 
              label="Unlock Device" 
              icon="🔓" 
              variant="success"
              loading={cmdLoading === 'UNLOCK'}
              onClick={() => sendCommand('UNLOCK')} 
            />
          </div>
        </div>
      </section>

      {/* Content */}
      <div style={s.content}>
        {activeTab === 'map' && (
          <section style={s.section}>
            <div style={s.sectionContent}>
              <h2 style={s.sectionTitle}>Live Location</h2>
              <p style={s.sectionDesc}>Real-time tracking of your child's device.</p>
              <div style={s.mapCard}>
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
                    options={{ disableDefaultUI: false, zoomControl: true }}
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
                    <span style={s.coordsLabel}>📍 Current Location</span>
                    <span style={s.coords}>
                      {location.lat?.toFixed(6)}, {location.lng?.toFixed(6)}
                    </span>
                  </div>
                )}
              </div>
            </div>
          </section>
        )}
        {activeTab === 'apps' && (
          <section style={s.section}>
            <div style={s.sectionContent}>
              <AppManagerPanel deviceId={childDeviceId} onBlockApp={(pkg) => sendCommand('BLOCK_APP', pkg)} />
            </div>
          </section>
        )}
        {activeTab === 'logs' && (
          <section style={s.section}>
            <div style={s.sectionContent}>
              <ActivityLog deviceId={childDeviceId} />
            </div>
          </section>
        )}
      </div>
    </div>
  );
}

function CmdButton({ label, icon, variant, loading, onClick }) {
  const colors = {
    danger: { bg: 'var(--danger)', hover: 'var(--danger)' },
    success: { bg: 'var(--success)', hover: 'var(--success)' },
  };
  const color = colors[variant] || colors.success;
  
  return (
    <button
      style={{
        ...s.pillBtn,
        background: color.bg,
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
    minHeight: '100vh',
    background: 'var(--bg-primary)',
    color: 'var(--text-primary)',
  },
  nav: {
    position: 'sticky', top: 0, zIndex: 100,
    background: 'var(--bg-secondary)',
    borderBottom: '1px solid var(--border-primary)',
    backdropFilter: 'blur(20px)',
  },
  navContent: {
    maxWidth: 980, margin: '0 auto',
    display: 'flex', alignItems: 'center', justifyContent: 'space-between',
    padding: '12px 24px', height: 48,
  },
  navLeft: { display: 'flex', alignItems: 'center', gap: 10 },
  navLogo: { width: 24, height: 24, borderRadius: 6 },
  navBrand: { fontSize: 17, fontWeight: 600, color: 'var(--text-primary)' },
  navCenter: { display: 'flex', gap: 24 },
  navLink: {
    background: 'none', border: 'none', cursor: 'pointer',
    fontSize: 14, fontWeight: 400, color: 'var(--text-tertiary)',
    padding: '4px 0', transition: 'color 0.2s',
  },
  navLinkActive: { color: 'var(--text-primary)', fontWeight: 600 },
  navRight: { display: 'flex', gap: 12, alignItems: 'center' },
  themeToggle: {
    background: 'none', border: 'none', cursor: 'pointer',
    fontSize: 20, padding: 4,
  },
  logoutBtn: {
    padding: '6px 14px',
    background: 'var(--bg-primary)',
    border: '1px solid var(--border-primary)',
    borderRadius: 980, fontSize: 14, fontWeight: 600,
    color: 'var(--text-primary)', cursor: 'pointer',
    transition: 'all 0.2s',
  },
  hero: {
    padding: '80px 24px',
    textAlign: 'center',
    background: 'var(--bg-secondary)',
    borderBottom: '1px solid var(--border-primary)',
  },
  heroContent: { maxWidth: 980, margin: '0 auto' },
  heroTitle: {
    fontSize: 56, fontWeight: 600, lineHeight: 1.07,
    letterSpacing: '-0.28px', margin: '0 0 16px',
    color: 'var(--text-primary)',
  },
  heroSub: {
    fontSize: 21, fontWeight: 400, lineHeight: 1.19,
    color: 'var(--text-secondary)', margin: '0 0 32px',
  },
  heroActions: { display: 'flex', gap: 16, justifyContent: 'center', flexWrap: 'wrap' },
  pillBtn: {
    padding: '12px 24px', borderRadius: 980,
    border: 'none', fontSize: 17, fontWeight: 600,
    color: '#fff', cursor: 'pointer',
    transition: 'opacity 0.2s, transform 0.1s',
    display: 'inline-flex', alignItems: 'center', gap: 8,
  },
  content: { minHeight: '60vh' },
  section: {
    padding: '60px 24px',
    borderBottom: '1px solid var(--border-primary)',
  },
  sectionContent: { maxWidth: 980, margin: '0 auto' },
  sectionTitle: {
    fontSize: 40, fontWeight: 600, lineHeight: 1.1,
    margin: '0 0 12px', textAlign: 'center',
  },
  sectionDesc: {
    fontSize: 21, fontWeight: 400, lineHeight: 1.19,
    color: 'var(--text-secondary)', margin: '0 0 40px',
    textAlign: 'center',
  },
  mapCard: {
    position: 'relative',
    height: 500,
    background: 'var(--bg-elevated)',
    borderRadius: 18,
    overflow: 'hidden',
    boxShadow: 'var(--shadow) 3px 5px 30px 0px',
  },
  mapOverlay: {
    position: 'absolute', inset: 0, zIndex: 10,
    display: 'flex', alignItems: 'center', justifyContent: 'center',
    background: 'rgba(0,0,0,0.3)',
  },
  mapOverlayText: { fontSize: 17, color: 'var(--text-secondary)' },
  mapLoading: {
    height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center',
    color: 'var(--text-tertiary)', fontSize: 17,
  },
  coordsBar: {
    position: 'absolute', bottom: 20, left: '50%', transform: 'translateX(-50%)',
    display: 'flex', alignItems: 'center', gap: 12,
    padding: '10px 20px',
    background: 'var(--bg-secondary)',
    borderRadius: 980,
    boxShadow: 'var(--shadow) 0px 4px 12px 0px',
    zIndex: 5,
  },
  coordsLabel: { fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' },
  coords: { fontSize: 14, color: 'var(--text-tertiary)', fontFamily: 'ui-monospace, monospace' },
};
