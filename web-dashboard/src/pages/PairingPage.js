import React, { useState, useEffect, useRef } from 'react';
import QRCode from 'qrcode';
import { pairApi, commandApi } from '../services/api';

export default function PairingPage() {
  const [me, setMe]               = useState(null);
  const [devices, setDevices]     = useState([]);
  const [loading, setLoading]     = useState(true);
  const [unpairing, setUnpairing] = useState(null);
  const [copied, setCopied]       = useState(false);
  const canvasRef                 = useRef(null);
  const pollRef                   = useRef(null);

  const load = async () => {
    try {
      const [meRes, devRes] = await Promise.all([
        pairApi.getMe(),
        pairApi.getChildDevices(),
      ]);
      setMe(meRes.data);
      setDevices(devRes.data || []);
    } catch {}
    finally { setLoading(false); }
  };

  useEffect(() => {
    load();
    pollRef.current = setInterval(load, 5000);
    return () => clearInterval(pollRef.current);
  }, []);

  // Draw QR code whenever pairCode changes
  useEffect(() => {
    if (!me?.pairCode || !canvasRef.current) return;
    QRCode.toCanvas(canvasRef.current, me.pairCode, {
      width: 200,
      margin: 2,
      color: { dark: '#000000', light: '#ffffff' },
    });
  }, [me?.pairCode]);

  const copyCode = () => {
    navigator.clipboard.writeText(me?.pairCode || '');
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const unpair = async (deviceId) => {
    if (!window.confirm('Unpair this device? The child will lose parental controls.')) return;
    setUnpairing(deviceId);
    try {
      await pairApi.unpair(deviceId);
      setDevices(prev => prev.filter(d => d.id !== deviceId));
    } catch { alert('Failed to unpair'); }
    finally { setUnpairing(null); }
  };

  if (loading) return <div style={s.empty}>Loading…</div>;

  return (
    <div style={s.wrap}>

      {/* ── Pair Code Card ── */}
      <div style={s.row}>
        <div style={s.card}>
          <div style={s.cardHeader}>
            <span style={s.cardTitle}>Your Pair Code</span>
            <span style={s.cardSub}>Share this with the child device</span>
          </div>

          <div style={s.codeBlock}>
            <span style={s.codeText}>{me?.pairCode || '—'}</span>
            <button style={{ ...s.copyBtn, ...(copied ? s.copyBtnDone : {}) }} onClick={copyCode}>
              {copied ? '✓ Copied' : 'Copy'}
            </button>
          </div>

          <p style={s.hint}>
            On the child's phone: open <strong>Gia Family Control</strong> →
            tap the gear icon → enter this code → tap <strong>Pair with Parent</strong>.
          </p>
        </div>

        {/* QR Code */}
        <div style={s.qrCard}>
          <div style={s.cardHeader}>
            <span style={s.cardTitle}>QR Code</span>
            <span style={s.cardSub}>Child can scan this</span>
          </div>
          <div style={s.qrWrap}>
            <canvas ref={canvasRef} style={s.qrCanvas} />
          </div>
        </div>
      </div>

      {/* ── Paired Devices ── */}
      <div style={s.card}>
        <div style={s.cardHeader}>
          <span style={s.cardTitle}>Paired Devices</span>
          <span style={s.badge}>{devices.length}</span>
        </div>

        {devices.length === 0 ? (
          <div style={s.empty}>
            <span style={s.emptyIcon}>📱</span>
            <p style={s.emptyText}>No devices paired yet.</p>
            <p style={s.emptyHint}>Share your pair code or QR with the child's device.</p>
          </div>
        ) : (
          <div style={s.deviceList}>
            {devices.map(device => {
              const online = device.isOnline;
              return (
                <div key={device.id} style={s.deviceRow}>
                  <div style={s.deviceIcon}>📱</div>
                  <div style={s.deviceInfo}>
                    <span style={s.deviceName}>{device.deviceName || 'Child Device'}</span>
                    <span style={s.deviceMeta}>
                      {device.deviceModel && `${device.deviceModel} · `}
                      Android {device.androidVersion}
                    </span>
                  </div>
                  <div style={s.deviceStats}>
                    <span style={{ ...s.statusPill, background: online ? '#0d2e1f' : '#1a1a1a', color: online ? '#34d399' : '#6b7280', border: `1px solid ${online ? '#1a5c3a' : '#2a2a2a'}` }}>
                      <span style={{ ...s.dot, background: online ? '#34d399' : '#4b5563' }} />
                      {online ? 'Online' : 'Offline'}
                    </span>
                    {device.batteryLevel != null && (
                      <span style={s.chip}>🔋 {device.batteryLevel}%</span>
                    )}
                    <span style={s.chip}>{device.isLocked ? '🔒 Locked' : '🔓 Unlocked'}</span>
                  </div>
                  <button
                    style={{ ...s.unpairBtn, opacity: unpairing === device.id ? 0.5 : 1 }}
                    onClick={() => unpair(device.id)}
                    disabled={unpairing === device.id}
                  >
                    {unpairing === device.id ? 'Unpairing…' : 'Unpair'}
                  </button>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* ── How it works ── */}
      <div style={s.card}>
        <div style={s.cardHeader}>
          <span style={s.cardTitle}>How Pairing Works</span>
        </div>
        <div style={s.steps}>
          {[
            ['1', 'Install the child app', 'Download GiaFamilyControl-Child.apk on the child\'s phone from the GitHub release.'],
            ['2', 'Set as home launcher', 'When prompted, set Gia as the default home app. It will ask on first install.'],
            ['3', 'Login on child device', 'Register or login with a CHILD account on the child\'s phone.'],
            ['4', 'Enter pair code', 'Tap the gear icon on the launcher → enter the pair code above → tap Pair.'],
            ['5', 'Start monitoring', 'Tap Start Monitoring on the child device. You\'re now connected.'],
          ].map(([num, title, desc]) => (
            <div key={num} style={s.step}>
              <span style={s.stepNum}>{num}</span>
              <div>
                <p style={s.stepTitle}>{title}</p>
                <p style={s.stepDesc}>{desc}</p>
              </div>
            </div>
          ))}
        </div>
      </div>

    </div>
  );
}

const s = {
  wrap: { display: 'flex', flexDirection: 'column', gap: 16 },
  row: { display: 'grid', gridTemplateColumns: '1fr auto', gap: 16 },
  card: { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 12 },
  cardHeader: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 20px', borderBottom: '1px solid var(--border-primary)' },
  cardTitle: { fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' },
  cardSub: { fontSize: 12, color: 'var(--text-tertiary)' },
  badge: { padding: '3px 10px', borderRadius: 980, background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', fontSize: 12, fontWeight: 600, color: 'var(--text-tertiary)' },
  codeBlock: { display: 'flex', alignItems: 'center', gap: 12, padding: '20px 20px 8px' },
  codeText: { flex: 1, fontSize: 28, fontWeight: 800, letterSpacing: '0.12em', color: 'var(--text-primary)', fontFamily: 'ui-monospace, monospace' },
  copyBtn: { padding: '8px 18px', background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', borderRadius: 8, fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)', cursor: 'pointer', transition: 'all 0.15s' },
  copyBtnDone: { background: '#0d2e1f', border: '1px solid #1a5c3a', color: '#34d399' },
  hint: { padding: '8px 20px 20px', fontSize: 13, color: 'var(--text-tertiary)', lineHeight: 1.6, margin: 0 },
  qrCard: { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 12, width: 260, flexShrink: 0 },
  qrWrap: { display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '20px' },
  qrCanvas: { borderRadius: 8 },
  deviceList: { display: 'flex', flexDirection: 'column' },
  deviceRow: { display: 'flex', alignItems: 'center', gap: 16, padding: '16px 20px', borderBottom: '1px solid var(--border-primary)' },
  deviceIcon: { fontSize: 28, flexShrink: 0 },
  deviceInfo: { display: 'flex', flexDirection: 'column', gap: 3, flex: 1, minWidth: 0 },
  deviceName: { fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' },
  deviceMeta: { fontSize: 12, color: 'var(--text-tertiary)' },
  deviceStats: { display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 },
  statusPill: { display: 'inline-flex', alignItems: 'center', gap: 5, padding: '3px 10px', borderRadius: 980, fontSize: 12, fontWeight: 600 },
  dot: { width: 6, height: 6, borderRadius: '50%' },
  chip: { padding: '3px 10px', borderRadius: 980, background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', fontSize: 12, color: 'var(--text-secondary)' },
  unpairBtn: { padding: '7px 14px', background: 'rgba(239,68,68,0.08)', border: '1px solid rgba(239,68,68,0.2)', borderRadius: 7, fontSize: 13, fontWeight: 600, color: '#f87171', cursor: 'pointer', flexShrink: 0 },
  empty: { display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '40px 20px', gap: 8 },
  emptyIcon: { fontSize: 36 },
  emptyText: { fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)', margin: 0 },
  emptyHint: { fontSize: 13, color: 'var(--text-tertiary)', margin: 0 },
  steps: { display: 'flex', flexDirection: 'column', gap: 0 },
  step: { display: 'flex', alignItems: 'flex-start', gap: 16, padding: '14px 20px', borderBottom: '1px solid var(--border-primary)' },
  stepNum: { width: 26, height: 26, borderRadius: '50%', background: 'var(--accent-subtle)', color: 'var(--accent-primary)', fontSize: 13, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  stepTitle: { fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', margin: '0 0 3px' },
  stepDesc: { fontSize: 12, color: 'var(--text-tertiary)', margin: 0, lineHeight: 1.5 },
};
