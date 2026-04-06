import React, { useState, useEffect, useCallback } from 'react';
import { GoogleMap, Marker, useJsApiLoader } from '@react-google-maps/api';
import { commandApi, locationApi, pairApi } from '../services/api';
import { subscribeToDeviceLocation, subscribeToDeviceStatus } from '../services/firebase';
import { useAuth } from '../context/AuthContext';
import { useTheme } from '../context/ThemeContext';
import AppManagerPanel from '../components/AppManagerPanel';
import ActivityLog from '../components/ActivityLog';
import PairingPage from './PairingPage';

const MAP_CONTAINER  = { width: '100%', height: '100%' };
const DEFAULT_CENTER = { lat: 14.5995, lng: 120.9842 };
const PRESETS        = [5, 10, 15, 30, 60, 120];

const NAV = [
  { id: 'overview', label: 'Overview',    icon: '⊞' },
  { id: 'location', label: 'Location',    icon: '📍' },
  { id: 'apps',     label: 'App Control', icon: '📱' },
  { id: 'activity', label: 'Activity',    icon: '📋' },
  { id: 'pairing',  label: 'Pairing',     icon: '🔗' },
];

// Dark map style — only used when isDark
const DARK_MAP = [
  { elementType: 'geometry',           stylers: [{ color: '#1a1a2e' }] },
  { elementType: 'labels.text.fill',   stylers: [{ color: '#8a8f98' }] },
  { elementType: 'labels.text.stroke', stylers: [{ color: '#1a1a2e' }] },
  { featureType: 'road',  elementType: 'geometry', stylers: [{ color: '#2a2a3e' }] },
  { featureType: 'water', elementType: 'geometry', stylers: [{ color: '#0d1117' }] },
  { featureType: 'poi',   stylers: [{ visibility: 'off' }] },
];

// Variant colors — light and dark versions
const VARIANTS = {
  danger:  { dark: { bg:'#3b1212', cl:'#f87171', br:'#5c1a1a' }, light: { bg:'#fce8e6', cl:'#c5221f', br:'#f5c6c4' } },
  success: { dark: { bg:'#0d2e1f', cl:'#34d399', br:'#1a5c3a' }, light: { bg:'#e6f4ea', cl:'#137333', br:'#b7dfbf' } },
  warn:    { dark: { bg:'#2e2412', cl:'#fbbf24', br:'#5c4a1a' }, light: { bg:'#fef7e0', cl:'#b06000', br:'#f5e0a0' } },
  muted:   { dark: { bg:'#1a1a1a', cl:'#6b7280', br:'#2a2a2a' }, light: { bg:'#f1f3f4', cl:'#5f6368', br:'#dadce0' } },
  info:    { dark: { bg:'#12203b', cl:'#818cf8', br:'#1a2e5c' }, light: { bg:'#e8f0fe', cl:'#1a73e8', br:'#aecbfa' } },
};

export default function DashboardPage() {
  const { user, logout }        = useAuth();
  const { isDark, toggleTheme } = useTheme();
  const [childDeviceId, setChildDeviceId] = useState(() => Number(localStorage.getItem('child_device_id') || 0));
  const [location, setLocation]           = useState(null);
  const [deviceStatus, setDeviceStatus]   = useState({});
  const [page, setPage]                   = useState('overview');
  const [cmdLoading, setCmdLoading]       = useState(null);
  const [sideCollapsed, setSideCollapsed] = useState(false);
  const [showTempModal, setShowTempModal] = useState(false);
  const [showPinModal, setShowPinModal]   = useState(false);
  const [cmdError, setCmdError]           = useState(null);

  useEffect(() => {
    pairApi.getChildDevices().then(({ data }) => {
      const id = data?.[0]?.id;
      if (id) { setChildDeviceId(id); localStorage.setItem('child_device_id', id); }
    }).catch(() => {});
  }, []);

  const { isLoaded } = useJsApiLoader({ googleMapsApiKey: process.env.REACT_APP_GOOGLE_MAPS_KEY || '' });

  useEffect(() => {
    const u1 = subscribeToDeviceLocation(childDeviceId, setLocation);
    const u2 = subscribeToDeviceStatus(childDeviceId, setDeviceStatus);
    return () => { u1(); u2(); };
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

  const sendCommand = useCallback(async (type, value = null) => {
    setCmdLoading(type); setCmdError(null);
    try {
      if (type === 'BLOCK_APP' || type === 'UNBLOCK_APP')
        await commandApi.sendApp(childDeviceId, type, value);
      else
        await commandApi.send(childDeviceId, type, value);
    } catch (err) {
      const d = err?.response?.data;
      setCmdError(typeof d === 'string' ? d : d?.message ?? d?.error ?? err?.message ?? 'Error');
    } finally { setCmdLoading(null); }
  }, [childDeviceId]);

  const unpairDevice = useCallback(async () => {
    if (!childDeviceId) return;
    if (!window.confirm('Unpair this device? The child will lose parental controls.')) return;
    try {
      await pairApi.unpair(childDeviceId);
      setChildDeviceId(0);
      localStorage.removeItem('child_device_id');
      setDeviceStatus({});
    } catch (err) {
      const d = err?.response?.data;
      setCmdError(typeof d === 'string' ? d : d?.message ?? 'Unpair failed');
    }
  }, [childDeviceId]);

  const setPin = useCallback(async (pin) => {
    try {
      await commandApi.send(childDeviceId, 'SET_PIN', pin);
    } catch (err) {
      const d = err?.response?.data;
      setCmdError(typeof d === 'string' ? d : d?.message ?? 'Set PIN failed');
    }
  }, [childDeviceId]);

  const isOnline  = deviceStatus?.isOnline      ?? false;
  const isLocked  = deviceStatus?.isLocked      ?? false;
  const battery   = deviceStatus?.batteryLevel  ?? '--';
  const connType  = deviceStatus?.connectionType ?? null;
  const childName = deviceStatus?.deviceName    ?? 'Child Device';
  const mapStyle  = isDark ? DARK_MAP : [];

  const onlinePill = {
    background: isOnline ? (isDark ? '#0d2e1f' : '#e6f4ea') : (isDark ? '#2e0d0d' : '#fce8e6'),
    color:      isOnline ? (isDark ? '#34d399' : '#137333') : (isDark ? '#f87171' : '#c5221f'),
    border:     `1px solid ${isOnline ? (isDark ? '#1a5c3a' : '#b7dfbf') : (isDark ? '#5c1a1a' : '#f5c6c4')}`,
  };

  return (
    <div style={s.shell}>
      {showTempModal && (
        <TempAccessModal isDark={isDark}
          onGrant={mins => { sendCommand('GRANT_TEMP_ACCESS', mins.toString()); setShowTempModal(false); }}
          onClose={() => setShowTempModal(false)}
        />
      )}
      {showPinModal && (
        <SetPinModal isDark={isDark}
          onSet={pin => { setPin(pin); setShowPinModal(false); }}
          onClose={() => setShowPinModal(false)}
        />
      )}

      {/* Sidebar */}
      <aside style={{ ...s.sidebar, width: sideCollapsed ? 64 : 220 }}>
        <div style={s.brand}>
          <img src="/logo.jpg" alt="Gia" style={s.brandLogo} />
          {!sideCollapsed && <span style={s.brandName}>Gia Control</span>}
        </div>

        <nav style={s.nav}>
          {NAV.map(n => (
            <button key={n.id}
              style={{ ...s.navItem, ...(page === n.id ? s.navItemActive : {}) }}
              onClick={() => setPage(n.id)} title={n.label}>
              <span style={s.navIcon}>{n.icon}</span>
              {!sideCollapsed && <span style={s.navLabel}>{n.label}</span>}
            </button>
          ))}
        </nav>

        {!sideCollapsed && (
          <div style={s.quickControls}>
            <p style={s.qcTitle}>Quick Controls</p>
            <Cmd isDark={isDark} label="Lock"         icon="🔒" variant="danger"  loading={cmdLoading==='LOCK'}             onClick={() => sendCommand('LOCK')} />
            <Cmd isDark={isDark} label="Unlock"       icon="🔓" variant="success" loading={cmdLoading==='UNLOCK'}           onClick={() => sendCommand('UNLOCK')} />
            <Cmd isDark={isDark} label="Launcher ON"  icon="🏠" variant="warn"    loading={cmdLoading==='ENABLE_LAUNCHER'}  onClick={() => sendCommand('ENABLE_LAUNCHER')} />
            <Cmd isDark={isDark} label="Launcher OFF" icon="🏠" variant="muted"   loading={cmdLoading==='DISABLE_LAUNCHER'} onClick={() => sendCommand('DISABLE_LAUNCHER')} />
            <Cmd isDark={isDark} label="Temp Access"  icon="⏱" variant="info"    loading={cmdLoading==='GRANT_TEMP_ACCESS'} onClick={() => setShowTempModal(true)} />
            <Cmd isDark={isDark} label="Set PIN"       icon="🔑" variant="info"    loading={false}                            onClick={() => setShowPinModal(true)} />
            <Cmd isDark={isDark} label="Unpair"        icon="🔓" variant="muted"   loading={false}                            onClick={unpairDevice} />
          </div>
        )}

        <button style={s.collapseBtn} onClick={() => setSideCollapsed(v => !v)}>
          {sideCollapsed ? '›' : '‹'}
        </button>

        <div style={s.sideFooter}>
          {!sideCollapsed && <span style={s.sideUser}>{user?.fullName || 'Parent'}</span>}
          <button style={s.themeBtn} onClick={toggleTheme} title={isDark ? 'Light mode' : 'Dark mode'}>
            {isDark ? '☀️' : '🌙'}
          </button>
          <button style={s.logoutBtn} onClick={logout} title="Sign out">⏻</button>
        </div>
      </aside>

      {/* Main */}
      <main style={s.main}>
        <header style={s.topbar}>
          <h1 style={s.pageTitle}>{NAV.find(n => n.id === page)?.label}</h1>
          <div style={s.topbarRight}>
            {cmdError && (
              <span style={s.errorChip} onClick={() => setCmdError(null)}>⚠️ {cmdError}</span>
            )}
            <span style={{ ...s.statusPill, ...onlinePill }}>
              <span style={{ ...s.dot, background: isOnline ? (isDark?'#34d399':'#137333') : (isDark?'#f87171':'#c5221f') }} />
              {childName} · {isOnline ? 'Online' : 'Offline'}
            </span>
            <span style={s.statChip}>🔋 {battery === '--' ? '--' : `${battery}%`}</span>
            <span style={s.statChip}>{isLocked ? '🔒 Locked' : '🔓 Unlocked'}</span>
            {connType && connType !== 'OFFLINE' && (
              <span style={s.statChip}>{connType === 'WIFI' ? '📶' : '📱'} {connType}</span>
            )}
          </div>
        </header>

        <div style={s.content}>
          {page === 'overview' && (
            <OverviewPage isOnline={isOnline} isLocked={isLocked} battery={battery}
              connType={connType} location={location} isLoaded={isLoaded}
              cmdLoading={cmdLoading} sendCommand={sendCommand}
              onTempAccess={() => setShowTempModal(true)}
              onSetPin={() => setShowPinModal(true)}
              onUnpair={unpairDevice}
              isDark={isDark} mapStyle={mapStyle} />
          )}
          {page === 'location' && <LocationPage location={location} isLoaded={isLoaded} mapStyle={mapStyle} />}
          {page === 'apps'     && <AppManagerPanel deviceId={childDeviceId} />}
          {page === 'activity' && <ActivityLog deviceId={childDeviceId} />}
          {page === 'pairing'  && <PairingPage />}
        </div>
      </main>
    </div>
  );
}

function OverviewPage({ isOnline, isLocked, battery, connType, location, isLoaded, cmdLoading, sendCommand, onTempAccess, onSetPin, onUnpair, isDark, mapStyle }) {
  return (
    <div style={s.overviewGrid}>
      <StatCard icon="📡" label="Status"     value={isOnline?'Online':'Offline'}          color={isOnline?(isDark?'#34d399':'#137333'):(isDark?'#f87171':'#c5221f')} />
      <StatCard icon="🔋" label="Battery"    value={battery==='--'?'--':`${battery}%`}    color={battery<20?(isDark?'#f87171':'#c5221f'):battery<50?(isDark?'#fbbf24':'#b06000'):(isDark?'#34d399':'#137333')} />
      <StatCard icon="🔒" label="Lock State" value={isLocked?'Locked':'Unlocked'}          color={isLocked?(isDark?'#f87171':'#c5221f'):(isDark?'#34d399':'#137333')} />
      <StatCard icon="📶" label="Connection" value={connType||'Offline'}                   color={isDark?'#818cf8':'#1a73e8'} />

      <div style={s.overviewMap}>
        <div style={s.cardHeader}>
          <span style={s.cardTitle}>Live Location</span>
          {location && <span style={s.cardSub}>{location.lat?.toFixed(5)}, {location.lng?.toFixed(5)}</span>}
        </div>
        <div style={s.miniMapWrap}>
          {!location && <div style={s.mapWait}>⏳ Waiting for location…</div>}
          {isLoaded ? (
            <GoogleMap mapContainerStyle={MAP_CONTAINER} center={location??DEFAULT_CENTER} zoom={15}
              options={{ disableDefaultUI:true, zoomControl:true, styles: mapStyle }}>
              {location && <Marker position={location} />}
            </GoogleMap>
          ) : <div style={s.mapWait}>Loading map…</div>}
        </div>
      </div>

      <div style={s.controlsCard}>
        <div style={s.cardHeader}><span style={s.cardTitle}>Device Controls</span></div>
        <div style={s.controlsGrid}>
          <CtrlBtn isDark={isDark} label="Lock Device"   icon="🔒" variant="danger"  loading={cmdLoading==='LOCK'}             onClick={() => sendCommand('LOCK')} />
          <CtrlBtn isDark={isDark} label="Unlock Device" icon="🔓" variant="success" loading={cmdLoading==='UNLOCK'}           onClick={() => sendCommand('UNLOCK')} />
          <CtrlBtn isDark={isDark} label="Launcher ON"   icon="🏠" variant="warn"    loading={cmdLoading==='ENABLE_LAUNCHER'}  onClick={() => sendCommand('ENABLE_LAUNCHER')} />
          <CtrlBtn isDark={isDark} label="Launcher OFF"  icon="🏠" variant="muted"   loading={cmdLoading==='DISABLE_LAUNCHER'} onClick={() => sendCommand('DISABLE_LAUNCHER')} />
          <CtrlBtn isDark={isDark} label="Temp Access"   icon="⏱" variant="info"    loading={cmdLoading==='GRANT_TEMP_ACCESS'} onClick={onTempAccess} />
          <CtrlBtn isDark={isDark} label="Set PIN"        icon="🔑" variant="info"    loading={false}                            onClick={onSetPin} />
          <CtrlBtn isDark={isDark} label="Unpair Device"  icon="🔓" variant="muted"   loading={false}                            onClick={onUnpair} />
        </div>
      </div>
    </div>
  );
}

function LocationPage({ location, isLoaded, mapStyle }) {
  return (
    <div style={s.locationPage}>
      <div style={s.fullMapCard}>
        {!location && <div style={s.mapWait}>⏳ Waiting for GPS data…</div>}
        {isLoaded ? (
          <GoogleMap mapContainerStyle={MAP_CONTAINER} center={location??DEFAULT_CENTER} zoom={16}
            options={{ disableDefaultUI:false, zoomControl:true, styles: mapStyle }}>
            {location && <Marker position={location} title="Child's Location" />}
          </GoogleMap>
        ) : <div style={s.mapWait}>Loading map…</div>}
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

function TempAccessModal({ onGrant, onClose, isDark }) {
  const [custom, setCustom] = useState('');
  const [err, setErr]       = useState('');
  const grant = (mins) => { if (!mins||mins<1){setErr('Enter a valid number');return;} onGrant(mins); };
  const handleCustom = () => { const v=parseInt(custom,10); if(!v||v<1){setErr('Enter valid minutes');return;} grant(v); };
  const v = VARIANTS.info[isDark?'dark':'light'];
  return (
    <div style={m.overlay} onClick={onClose}>
      <div style={m.modal} onClick={e=>e.stopPropagation()}>
        <div style={m.header}>
          <span style={m.title}>⏱ Temporary Access</span>
          <button style={m.closeBtn} onClick={onClose}>✕</button>
        </div>
        <p style={m.sub}>Grant the child temporary access to all apps for a set time.</p>
        <div style={m.presetGrid}>
          {PRESETS.map(min => (
            <button key={min} style={m.presetBtn} onClick={() => grant(min)}>
              {min < 60 ? `${min} min` : `${min/60} hr`}
            </button>
          ))}
        </div>
        <p style={m.orLabel}>— or enter custom —</p>
        <div style={m.customRow}>
          <input style={m.input} type="number" min="1" placeholder="e.g. 45"
            value={custom} onChange={e=>{setCustom(e.target.value);setErr('');}}
            onKeyDown={e=>e.key==='Enter'&&handleCustom()} />
          <span style={m.unit}>minutes</span>
          <button style={{ ...m.grantBtn, background:v.bg, color:v.cl, border:`1px solid ${v.br}` }} onClick={handleCustom}>Grant</button>
        </div>
        {err && <p style={m.err}>{err}</p>}
      </div>
    </div>
  );
}

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

function Cmd({ label, icon, variant, loading, onClick, isDark }) {
  const v = VARIANTS[variant][isDark?'dark':'light'];
  return (
    <button style={{ ...s.cmdBtn, background:v.bg, color:v.cl, opacity:loading?0.5:1 }} onClick={onClick} disabled={loading}>
      {icon} {loading ? '…' : label}
    </button>
  );
}

function CtrlBtn({ label, icon, variant, loading, onClick, isDark }) {
  const v = VARIANTS[variant][isDark?'dark':'light'];
  return (
    <button style={{ ...s.ctrlBtn, background:v.bg, color:v.cl, border:`1px solid ${v.br}`, opacity:loading?0.5:1 }} onClick={onClick} disabled={loading}>
      <span style={s.ctrlIcon}>{icon}</span>
      <span>{loading ? '…' : label}</span>
    </button>
  );
}

function SetPinModal({ onSet, onClose, isDark }) {
  const [pin, setPin]   = useState('');
  const [err, setErr]   = useState('');
  const v = VARIANTS.info[isDark?'dark':'light'];

  const handleSet = () => {
    if (pin.length < 4) { setErr('PIN must be at least 4 digits'); return; }
    if (!/^\d+$/.test(pin)) { setErr('PIN must be numbers only'); return; }
    onSet(pin);
  };

  return (
    <div style={m.overlay} onClick={onClose}>
      <div style={m.modal} onClick={e => e.stopPropagation()}>
        <div style={m.header}>
          <span style={m.title}>🔑 Set Parent PIN</span>
          <button style={m.closeBtn} onClick={onClose}>✕</button>
        </div>
        <p style={m.sub}>This PIN protects the launcher exit on the child device. Min 4 digits.</p>
        <input
          style={{ ...m.input, width:'100%', marginBottom:12 }}
          type="password" inputMode="numeric" placeholder="Enter new PIN"
          value={pin}
          onChange={e => { setPin(e.target.value); setErr(''); }}
          onKeyDown={e => e.key === 'Enter' && handleSet()}
          maxLength={8}
        />
        {err && <p style={{ ...m.err, marginBottom:12 }}>{err}</p>}
        <button style={{ ...m.grantBtn, background:v.bg, color:v.cl, border:`1px solid ${v.br}`, width:'100%' }} onClick={handleSet}>
          Set PIN
        </button>
      </div>
    </div>
  );
}

const m = {
  overlay:    { position:'fixed', inset:0, background:'rgba(0,0,0,0.5)', display:'flex', alignItems:'center', justifyContent:'center', zIndex:1000 },
  modal:      { background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:14, width:380, padding:24 },
  header:     { display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:8 },
  title:      { fontSize:16, fontWeight:700, color:'var(--text-primary)' },
  closeBtn:   { background:'none', border:'none', color:'var(--text-tertiary)', fontSize:18, cursor:'pointer', padding:4 },
  sub:        { fontSize:13, color:'var(--text-tertiary)', marginBottom:20 },
  presetGrid: { display:'grid', gridTemplateColumns:'repeat(3, 1fr)', gap:8, marginBottom:16 },
  presetBtn:  { padding:'11px 0', background:'var(--bg-tertiary)', border:'1px solid var(--border-primary)', borderRadius:8, color:'var(--text-primary)', fontSize:14, fontWeight:600, cursor:'pointer' },
  orLabel:    { fontSize:12, color:'var(--text-tertiary)', textAlign:'center', margin:'0 0 14px' },
  customRow:  { display:'flex', alignItems:'center', gap:8 },
  input:      { flex:1, padding:'10px 12px', background:'var(--bg-tertiary)', border:'1px solid var(--border-primary)', borderRadius:8, fontSize:14, color:'var(--text-primary)', width:0 },
  unit:       { fontSize:13, color:'var(--text-tertiary)', whiteSpace:'nowrap' },
  grantBtn:   { padding:'10px 18px', borderRadius:8, fontSize:14, fontWeight:600, cursor:'pointer', whiteSpace:'nowrap', border:'none' },
  err:        { marginTop:10, fontSize:13, color:'var(--danger)' },
};

const s = {
  shell:        { display:'flex', height:'100vh', overflow:'hidden', background:'var(--bg-primary)', color:'var(--text-primary)' },
  sidebar:      { display:'flex', flexDirection:'column', background:'var(--bg-secondary)', borderRight:'1px solid var(--border-primary)', transition:'width 0.2s', flexShrink:0, overflow:'hidden' },
  brand:        { display:'flex', alignItems:'center', gap:10, padding:'20px 16px', borderBottom:'1px solid var(--border-primary)' },
  brandLogo:    { width:28, height:28, borderRadius:8, flexShrink:0 },
  brandName:    { fontSize:15, fontWeight:700, color:'var(--text-primary)', whiteSpace:'nowrap' },
  nav:          { display:'flex', flexDirection:'column', gap:2, padding:'12px 8px' },
  navItem:      { display:'flex', alignItems:'center', gap:10, padding:'9px 10px', borderRadius:8, background:'none', border:'none', color:'var(--text-tertiary)', fontSize:14, fontWeight:500, cursor:'pointer', transition:'all 0.15s', textAlign:'left', whiteSpace:'nowrap' },
  navItemActive:{ background:'var(--accent-subtle)', color:'var(--text-primary)' },
  navIcon:      { fontSize:16, flexShrink:0, width:20, textAlign:'center' },
  navLabel:     { fontSize:14 },
  quickControls:{ padding:'12px 8px', borderTop:'1px solid var(--border-primary)', marginTop:8 },
  qcTitle:      { fontSize:11, fontWeight:600, color:'var(--text-tertiary)', textTransform:'uppercase', letterSpacing:'0.08em', padding:'0 10px', marginBottom:8 },
  cmdBtn:       { display:'flex', alignItems:'center', gap:8, width:'100%', padding:'8px 10px', borderRadius:8, border:'none', fontSize:13, fontWeight:500, cursor:'pointer', marginBottom:4, transition:'opacity 0.15s' },
  collapseBtn:  { margin:'8px', padding:'6px', background:'var(--bg-tertiary)', border:'1px solid var(--border-primary)', borderRadius:6, color:'var(--text-tertiary)', cursor:'pointer', fontSize:16, alignSelf:'flex-end' },
  sideFooter:   { marginTop:'auto', display:'flex', alignItems:'center', justifyContent:'space-between', padding:'12px', borderTop:'1px solid var(--border-primary)' },
  sideUser:     { fontSize:13, color:'var(--text-secondary)', overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' },
  logoutBtn:    { background:'none', border:'none', color:'var(--text-tertiary)', cursor:'pointer', fontSize:18, padding:4, flexShrink:0 },
  themeBtn:     { background:'none', border:'none', cursor:'pointer', fontSize:16, padding:4, flexShrink:0 },
  main:         { flex:1, display:'flex', flexDirection:'column', overflow:'hidden' },
  topbar:       { display:'flex', alignItems:'center', justifyContent:'space-between', padding:'0 24px', height:56, borderBottom:'1px solid var(--border-primary)', background:'var(--bg-secondary)', flexShrink:0 },
  topbarRight:  { display:'flex', alignItems:'center', gap:8 },
  pageTitle:    { fontSize:18, fontWeight:600, color:'var(--text-primary)', margin:0 },
  statusPill:   { display:'inline-flex', alignItems:'center', gap:6, padding:'4px 12px', borderRadius:980, fontSize:13, fontWeight:600 },
  dot:          { width:7, height:7, borderRadius:'50%' },
  statChip:     { padding:'4px 10px', borderRadius:980, background:'var(--bg-tertiary)', border:'1px solid var(--border-primary)', fontSize:13, color:'var(--text-secondary)' },
  errorChip:    { padding:'4px 12px', borderRadius:980, background:'var(--danger-bg)', border:'1px solid var(--danger-border)', fontSize:12, color:'var(--danger)', cursor:'pointer', maxWidth:300, overflow:'hidden', textOverflow:'ellipsis', whiteSpace:'nowrap' },
  content:      { flex:1, overflow:'auto', padding:24 },
  overviewGrid: { display:'grid', gridTemplateColumns:'repeat(4, 1fr)', gridTemplateRows:'auto 1fr', gap:16 },
  statCard:     { background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:12, padding:20, display:'flex', alignItems:'center', gap:16 },
  statIcon:     { fontSize:28 },
  statLabel:    { fontSize:12, color:'var(--text-tertiary)', fontWeight:500, marginBottom:4, textTransform:'uppercase', letterSpacing:'0.06em' },
  statValue:    { fontSize:20, fontWeight:700 },
  overviewMap:  { gridColumn:'1 / 3', background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:12, overflow:'hidden', display:'flex', flexDirection:'column' },
  controlsCard: { gridColumn:'3 / 5', background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:12, overflow:'hidden', display:'flex', flexDirection:'column' },
  cardHeader:   { display:'flex', alignItems:'center', justifyContent:'space-between', padding:'16px 20px', borderBottom:'1px solid var(--border-primary)' },
  cardTitle:    { fontSize:14, fontWeight:600, color:'var(--text-primary)' },
  cardSub:      { fontSize:12, color:'var(--text-tertiary)', fontFamily:'ui-monospace, monospace' },
  miniMapWrap:  { flex:1, minHeight:280, position:'relative' },
  controlsGrid: { padding:16, display:'flex', flexDirection:'column', gap:8 },
  ctrlBtn:      { display:'flex', alignItems:'center', gap:10, padding:'11px 16px', borderRadius:8, fontSize:14, fontWeight:500, cursor:'pointer', transition:'opacity 0.15s' },
  ctrlIcon:     { fontSize:16 },
  locationPage: { height:'100%', display:'flex', flexDirection:'column' },
  fullMapCard:  { flex:1, borderRadius:12, overflow:'hidden', position:'relative', border:'1px solid var(--border-primary)', minHeight:500 },
  mapWait:      { position:'absolute', inset:0, display:'flex', alignItems:'center', justifyContent:'center', background:'var(--bg-secondary)', color:'var(--text-tertiary)', fontSize:15, zIndex:5 },
  coordsBar:    { position:'absolute', bottom:20, left:'50%', transform:'translateX(-50%)', display:'flex', alignItems:'center', gap:10, padding:'8px 18px', background:'var(--bg-secondary)', borderRadius:980, border:'1px solid var(--border-primary)', zIndex:10 },
  coordsLabel:  { fontSize:13, fontWeight:600, color:'var(--text-primary)' },
  coords:       { fontSize:13, color:'var(--text-tertiary)', fontFamily:'ui-monospace, monospace' },
};
