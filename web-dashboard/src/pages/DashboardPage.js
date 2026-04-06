import React, { useState, useEffect, useCallback } from 'react';
import { GoogleMap, Marker, useJsApiLoader } from '@react-google-maps/api';
import { commandApi, locationApi } from '../services/api';
import { subscribeToDeviceLocation, subscribeToDeviceStatus } from '../services/firebase';
import { useAuth } from '../context/AuthContext';
import AppManagerPanel from '../components/AppManagerPanel';
import ActivityLog from '../components/ActivityLog';

const MAP_CONTAINER = { width: '100%', height: '100%' };
const DEFAULT_CENTER = { lat: 14.5995, lng: 120.9842 };

const NAV = [
  { id: 'overview', label: 'Overview',    icon: '⊞' },
  { id: 'location', label: 'Location',    icon: '📍' },
  { id: 'apps',     label: 'App Control', icon: '📱' },
  { id: 'activity', label: 'Activity',    icon: '📋' },
];

export default function DashboardPage() {
  const { user, logout } = useAuth();
  const [childDeviceId] = useState(() => Number(localStorage.getItem('child_device_id') || 1));
  const [location, setLocation]       = useState(null);
  const [deviceStatus, setDeviceStatus] = useState({});
  const [page, setPage]               = useState('overview');
  const [cmdLoading, setCmdLoading]   = useState(null);
  const [sideCollapsed, setSideCollapsed] = useState(false);

  const { isLoaded } = useJsApiLoader({
    googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_KEY || '',
  });

  useEffect(() => {
    const unsubLoc    = subscribeToDeviceLocation(childDeviceId, setLocation);
    const unsubStatus = subscribeToDeviceStatus(childDeviceId, setDeviceStatus);
    return () => { unsubLoc(); unsubStatus(); };
  }, [childDeviceId]);

  useEffect(() => {
    const poll = async () => {
      try {
        const { data } = await locationApi.getLatest(childDeviceId);
        if (data?.latitude) setLocation({ lat: data.latitude, lng: data.longitude });
      } catch {}
    };
    poll();
    const id = setInterval(poll, 5000);
    return () => clearInterval(id);
  }, [childDeviceId]);

  const sendCommand = useCallback(async (type, pkg = null) => {
    setCmdLoading(type);
    try {
      await commandApi.send(childDeviceId, type, pkg);
    } catch {
      alert('Failed to send command');
    } finally {
      setCmdLoading(null);
    }
  }, [childDeviceId]);

  const isOnline  = deviceStatus?.isOnline  ?? false;
  const isLocked  = deviceStatus?.isLocked  ?? false;
  const battery   = deviceStatus?.batteryLevel ?? '--';
  const connType  = deviceStatus?.connectionType ?? null;
  const childName = deviceStatus?.deviceName ?? localStorage.getItem('child_device_name') ?? 'Child Device';

  return (
    <div style={s.shell}>
      {/* ── Sidebar ── */}
      <aside style={{ ...s.sidebar, width: sideCollapsed ? 64 : 220 }}>
        {/* Brand */}
        <div style={s.brand}>
          <img src="/logo.jpg" alt="Gia" style={s.brandLogo} />
          {!sideCollapsed && <span style={s.brandName}>Gia Control</span>}
        </div>

        {/* Nav */}
        <nav style={s.nav}>
          {NAV.map(n => (
            <button
              key={n.id}
              style={{ ...s.navItem, ...(page === n.id ? s.navItemActive : {}) }}
              onClick={() => setPage(n.id)}
              title={n.label}
            >
              <span style={s.navIcon}>{n.icon}</span>
              {!sideCollapsed && <span style={s.navLabel}>{n.label}</span>}
            </button>
          ))}
        </nav>

        {/* Quick Controls */}
        {!sideCollapsed && (
          <div style={s.quickControls}>
            <p style={s.qcTitle}>Quick Controls</p>
            <Cmd label="Lock"    icon="🔒" variant="danger"  loading={cmdLoading === 'LOCK'}   onClick={() => sendCommand('LOCK')} />
            <Cmd label="Unlock"  icon="🔓" variant="success" loading={cmdLoading === 'UNLOCK'} onClick={() => sendCommand('UNLOCK')} />
            <Cmd label="Launcher ON"  icon="🏠" variant="warn" loading={cmdLoading === 'ENABLE_LAUNCHER'}  onClick={() => sendCommand('ENABLE_LAUNCHER')} />
            <Cmd label="Launcher OFF" icon="🏠" variant="muted" loading={cmdLoading === 'DISABLE_LAUNCHER'} onClick={() => sendCommand('DISABLE_LAUNCHER')} />
          </div>
        )}

        {/* Collapse toggle */}
        <button style={s.collapseBtn} onClick={() => setSideCollapsed(v => !v)}>
          {sideCollapsed ? '›' : '‹'}
        </button>

        {/* User + Logout */}
        <div style={s.sideFooter}>
          {!sideCollapsed && (
            <span style={s.sideUser}>{user?.fullName || 'Parent'}</span>
          )}
          <button style={s.logoutBtn} onClick={logout} title="Sign out">⏻</button>
        </div>
      </aside>

      {/* ── Main ── */}
      <main style={s.main}>
        {/* Top bar */}
        <header style={s.topbar}>
          <div style={s.topbarLeft}>
            <h1 style={s.pageTitle}>{NAV.find(n => n.id === page)?.label}</h1>
          </div>
          <div style={s.topbarRight}>
            {/* Status pill */}
            <span style={{ ...s.statusPill, background: isOnline ? '#0d2e1f' : '#2e0d0d', color: isOnline ? '#34d399' : '#f87171', border: `1px solid ${isOnline ? '#1a5c3a' : '#5c1a1a'}` }}>
              <span style={{ ...s.dot, background: isOnline ? '#34d399' : '#f87171' }} />
              {childName} · {isOnline ? 'Online' : 'Offline'}
            </span>
            <span style={s.statChip}>🔋 {battery === '--' ? '--' : `${battery}%`}</span>
            <span style={s.statChip}>{isLocked ? '🔒 Locked' : '🔓 Unlocked'}</span>
            {connType && connType !== 'OFFLINE' && (
              <span style={s.statChip}>{connType === 'WIFI' ? '📶' : '📱'} {connType}</span>
            )}
          </div>
        </header>

        {/* Page content */}
        <div style={s.content}>
          {page === 'overview' && (
            <OverviewPage
              isOnline={isOnline} isLocked={isLocked} battery={battery}
              connType={connType} childName={childName} location={location}
              cmdLoading={cmdLoading} sendCommand={sendCommand}
              isLoaded={isLoaded}
            />
          )}
          {page === 'location' && (
            <LocationPage location={location} isLoaded={isLoaded} />
          )}
          {page === 'apps' && (
            <AppManagerPanel deviceId={childDeviceId} onBlockApp={pkg => sendCommand('BLOCK_APP', pkg)} />
          )}
          {page === 'activity' && (
            <ActivityLog deviceId={childDeviceId} />
          )}
        </div>
      </main>
    </div>
  );
}

/* ── Overview Page ── */
function OverviewPage({ isOnline, isLocked, battery, connType, childName, location, cmdLoading, sendCommand, isLoaded }) {
  return (
    <div style={s.overviewGrid}>
      {/* Stat cards */}
      <StatCard icon="📡" label="Status"     value={isOnline ? 'Online' : 'Offline'}   color={isOnline ? '#34d399' : '#f87171'} />
      <StatCard icon="🔋" label="Battery"    value={battery === '--' ? '--' : `${battery}%`} color={battery < 20 ? '#f87171' : battery < 50 ? '#fbbf24' : '#34d399'} />
      <StatCard icon="🔒" label="Lock State" value={isLocked ? 'Locked' : 'Unlocked'}  color={isLocked ? '#f87171' : '#34d399'} />
      <StatCard icon="📶" label="Connection" value={connType || 'Offline'}              color="#818cf8" />

      {/* Mini map */}
      <div style={s.overviewMap}>
        <div style={s.cardHeader}>
          <span style={s.cardTitle}>Live Location</span>
          {location && <span style={s.cardSub}>{location.lat?.toFixed(5)}, {location.lng?.toFixed(5)}</span>}
        </div>
        <div style={s.miniMapWrap}>
          {!location && (
            <div style={s.mapWait}>⏳ Waiting for location…</div>
          )}
          {isLoaded ? (
            <GoogleMap
              mapContainerStyle={MAP_CONTAINER}
              center={location ? { lat: location.lat, lng: location.lng } : DEFAULT_CENTER}
              zoom={15}
              options={{ disableDefaultUI: true, zoomControl: true, styles: darkMapStyle }}
            >
              {location && <Marker position={{ lat: location.lat, lng: location.lng }} />}
            </GoogleMap>
          ) : (
            <div style={s.mapWait}>Loading map…</div>
          )}
        </div>
      </div>

      {/* Controls card */}
      <div style={s.controlsCard}>
        <div style={s.cardHeader}>
          <span style={s.cardTitle}>Device Controls</span>
        </div>
        <div style={s.controlsGrid}>
          <CtrlBtn label="Lock Device"     icon="🔒" variant="danger"  loading={cmdLoading === 'LOCK'}             onClick={() => sendCommand('LOCK')} />
          <CtrlBtn label="Unlock Device"   icon="🔓" variant="success" loading={cmdLoading === 'UNLOCK'}           onClick={() => sendCommand('UNLOCK')} />
          <CtrlBtn label="Launcher ON"     icon="🏠" variant="warn"    loading={cmdLoading === 'ENABLE_LAUNCHER'}  onClick={() => sendCommand('ENABLE_LAUNCHER')} />
          <CtrlBtn label="Launcher OFF"    icon="🏠" variant="muted"   loading={cmdLoading === 'DISABLE_LAUNCHER'} onClick={() => sendCommand('DISABLE_LAUNCHER')} />
          <CtrlBtn label="Temp Access 30m" icon="⏱" variant="info"    loading={cmdLoading === 'GRANT_TEMP_ACCESS'} onClick={() => sendCommand('GRANT_TEMP_ACCESS', '30')} />
        </div>
      </div>
    </div>
  );
}

/* ── Location Page ── */
function LocationPage({ location, isLoaded }) {
  return (
    <div style={s.locationPage}>
      <div style={s.fullMapCard}>
        {!location && (
          <div style={s.mapWait}>⏳ Waiting for GPS data…</div>
        )}
        {isLoaded ? (
          <GoogleMap
            mapContainerStyle={MAP_CONTAINER}
            center={location ? { lat: location.lat, lng: location.lng } : DEFAULT_CENTER}
            zoom={16}
            options={{ disableDefaultUI: false, zoomControl: true, styles: darkMapStyle }}
          >
            {location && <Marker position={{ lat: location.lat, lng: location.lng }} title="Child's Location" />}
          </GoogleMap>
        ) : (
          <div style={s.mapWait}>Loading map…</div>
        )}
        {location && (
          <div style={s.coordsBar}>
            <span style={s.coordsLabel}>📍 Live</span>
            <span style={s.coords}>{location.lat?.toFixed(6)}, {location.lng?.toFixed(6)}</span>
          </div>
        )}
      </div>
    </div>
  );
}

/* ── Small components ── */
function StatCard({ icon, label, value, color }) {
  return (
    <div style={s.statCard}>
      <span style={s.statIcon}>{icon}</span>
      <div>
        <p style={s.statLabel}>{label}</p>
        <p style={{ ...s.statValue, color }}>{value}</p>
      </div>
    </div>
  );
}

function Cmd({ label, icon, variant, loading, onClick }) {
  const bg = { danger: '#3b1212', success: '#0d2e1f', warn: '#2e2412', muted: '#1a1a1a' };
  const cl = { danger: '#f87171', success: '#34d399', warn: '#fbbf24', muted: '#6b7280' };
  return (
    <button style={{ ...s.cmdBtn, background: bg[variant], color: cl[variant], opacity: loading ? 0.5 : 1 }} onClick={onClick} disabled={loading}>
      {icon} {loading ? '…' : label}
    </button>
  );
}

function CtrlBtn({ label, icon, variant, loading, onClick }) {
  const bg = { danger: '#3b1212', success: '#0d2e1f', warn: '#2e2412', muted: '#1a1a1a', info: '#12203b' };
  const cl = { danger: '#f87171', success: '#34d399', warn: '#fbbf24', muted: '#9ca3af', info: '#818cf8' };
  const br = { danger: '#5c1a1a', success: '#1a5c3a', warn: '#5c4a1a', muted: '#2a2a2a', info: '#1a2e5c' };
  return (
    <button style={{ ...s.ctrlBtn, background: bg[variant], color: cl[variant], border: `1px solid ${br[variant]}`, opacity: loading ? 0.5 : 1 }} onClick={onClick} disabled={loading}>
      <span style={s.ctrlIcon}>{icon}</span>
      <span>{loading ? '…' : label}</span>
    </button>
  );
}

/* ── Dark map style ── */
const darkMapStyle = [
  { elementType: 'geometry', stylers: [{ color: '#1a1a2e' }] },
  { elementType: 'labels.text.fill', stylers: [{ color: '#8a8f98' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#1a1a2e' }] },
  { featureType: 'road', elementType: 'geometry', stylers: [{ color: '#2a2a3e' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#0d1117' }] },
  { featureType: 'poi', stylers: [{ visibility: 'off' }] },
];

/* ── Styles ── */
const s = {
  shell: { display: 'flex', height: '100vh', overflow: 'hidden', background: 'var(--bg-primary)', color: 'var(--text-primary)' },

  /* Sidebar */
  sidebar: { display: 'flex', flexDirection: 'column', background: 'var(--bg-secondary)', borderRight: '1px solid var(--border-primary)', transition: 'width 0.2s', flexShrink: 0, overflow: 'hidden' },
  brand: { display: 'flex', alignItems: 'center', gap: 10, padding: '20px 16px', borderBottom: '1px solid var(--border-primary)' },
  brandLogo: { width: 28, height: 28, borderRadius: 8, flexShrink: 0 },
  brandName: { fontSize: 15, fontWeight: 700, color: 'var(--text-primary)', whiteSpace: 'nowrap' },
  nav: { display: 'flex', flexDirection: 'column', gap: 2, padding: '12px 8px' },
  navItem: { display: 'flex', alignItems: 'center', gap: 10, padding: '9px 10px', borderRadius: 8, background: 'none', border: 'none', color: 'var(--text-tertiary)', fontSize: 14, fontWeight: 500, cursor: 'pointer', transition: 'all 0.15s', textAlign: 'left', whiteSpace: 'nowrap' },
  navItemActive: { background: 'var(--accent-subtle)', color: 'var(--text-primary)' },
  navIcon: { fontSize: 16, flexShrink: 0, width: 20, textAlign: 'center' },
  navLabel: { fontSize: 14 },
  quickControls: { padding: '12px 8px', borderTop: '1px solid var(--border-primary)', marginTop: 8 },
  qcTitle: { fontSize: 11, fontWeight: 600, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.08em', padding: '0 10px', marginBottom: 8 },
  cmdBtn: { display: 'flex', alignItems: 'center', gap: 8, width: '100%', padding: '8px 10px', borderRadius: 8, border: 'none', fontSize: 13, fontWeight: 500, cursor: 'pointer', marginBottom: 4, transition: 'opacity 0.15s' },
  collapseBtn: { margin: '8px', padding: '6px', background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', borderRadius: 6, color: 'var(--text-tertiary)', cursor: 'pointer', fontSize: 16, alignSelf: 'flex-end' },
  sideFooter: { marginTop: 'auto', display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '12px 12px', borderTop: '1px solid var(--border-primary)' },
  sideUser: { fontSize: 13, color: 'var(--text-secondary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  logoutBtn: { background: 'none', border: 'none', color: 'var(--text-tertiary)', cursor: 'pointer', fontSize: 18, padding: 4, flexShrink: 0 },

  /* Main */
  main: { flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' },
  topbar: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px', height: 56, borderBottom: '1px solid var(--border-primary)', background: 'var(--bg-secondary)', flexShrink: 0 },
  topbarLeft: {},
  topbarRight: { display: 'flex', alignItems: 'center', gap: 8 },
  pageTitle: { fontSize: 18, fontWeight: 600, color: 'var(--text-primary)', margin: 0 },
  statusPill: { display: 'inline-flex', alignItems: 'center', gap: 6, padding: '4px 12px', borderRadius: 980, fontSize: 13, fontWeight: 600 },
  dot: { width: 7, height: 7, borderRadius: '50%' },
  statChip: { padding: '4px 10px', borderRadius: 980, background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', fontSize: 13, color: 'var(--text-secondary)' },
  content: { flex: 1, overflow: 'auto', padding: 24 },

  /* Overview */
  overviewGrid: { display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gridTemplateRows: 'auto 1fr', gap: 16 },
  statCard: { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 12, padding: '20px', display: 'flex', alignItems: 'center', gap: 16 },
  statIcon: { fontSize: 28 },
  statLabel: { fontSize: 12, color: 'var(--text-tertiary)', fontWeight: 500, marginBottom: 4, textTransform: 'uppercase', letterSpacing: '0.06em' },
  statValue: { fontSize: 20, fontWeight: 700 },
  overviewMap: { gridColumn: '1 / 3', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 12, overflow: 'hidden', display: 'flex', flexDirection: 'column' },
  controlsCard: { gridColumn: '3 / 5', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 12, overflow: 'hidden', display: 'flex', flexDirection: 'column' },
  cardHeader: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 20px', borderBottom: '1px solid var(--border-primary)' },
  cardTitle: { fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' },
  cardSub: { fontSize: 12, color: 'var(--text-tertiary)', fontFamily: 'ui-monospace, monospace' },
  miniMapWrap: { flex: 1, minHeight: 280, position: 'relative' },
  controlsGrid: { padding: 16, display: 'flex', flexDirection: 'column', gap: 8 },
  ctrlBtn: { display: 'flex', alignItems: 'center', gap: 10, padding: '11px 16px', borderRadius: 8, fontSize: 14, fontWeight: 500, cursor: 'pointer', transition: 'opacity 0.15s' },
  ctrlIcon: { fontSize: 16 },

  /* Location page */
  locationPage: { height: '100%', display: 'flex', flexDirection: 'column' },
  fullMapCard: { flex: 1, borderRadius: 12, overflow: 'hidden', position: 'relative', border: '1px solid var(--border-primary)', minHeight: 500 },
  mapWait: { position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-secondary)', color: 'var(--text-tertiary)', fontSize: 15, zIndex: 5 },
  coordsBar: { position: 'absolute', bottom: 20, left: '50%', transform: 'translateX(-50%)', display: 'flex', alignItems: 'center', gap: 10, padding: '8px 18px', background: 'var(--bg-secondary)', borderRadius: 980, border: '1px solid var(--border-primary)', zIndex: 10 },
  coordsLabel: { fontSize: 13, fontWeight: 600, color: 'var(--text-primary)' },
  coords: { fontSize: 13, color: 'var(--text-tertiary)', fontFamily: 'ui-monospace, monospace' },
};
