import React, { useState, useEffect } from 'react';
import api from '../services/api';

export default function DeviceStatusBar({ deviceId, status }) {
  const [deviceInfo, setDeviceInfo] = useState(null);

  useEffect(() => {
    api.get(`/api/device/${deviceId}`)
      .then(({ data }) => setDeviceInfo(data))
      .catch(() => {});
  }, [deviceId]);

  const battery = status?.batteryLevel ?? deviceInfo?.batteryLevel ?? '--';
  const isOnline = status?.isOnline ?? deviceInfo?.isOnline ?? false;
  const isLocked = status?.isLocked ?? deviceInfo?.isLocked ?? false;
  const connType = status?.connectionType ?? deviceInfo?.connectionType ?? null;
  const deviceName = deviceInfo?.deviceName || 'Child Device';

  const batteryColor = battery === '--' ? '#62666d'
    : battery < 20 ? '#ef4444'
    : battery < 50 ? '#f59e0b'
    : '#10b981';

  return (
    <div style={s.bar}>
      <div style={s.left}>
        <span style={s.deviceName}>{deviceName}</span>
        <span style={s.divider} />
        <span style={{ ...s.pill, background: isOnline ? 'rgba(16,185,129,0.12)' : 'rgba(239,68,68,0.1)', color: isOnline ? '#10b981' : '#f87171', borderColor: isOnline ? 'rgba(16,185,129,0.25)' : 'rgba(239,68,68,0.2)' }}>
          <span style={{ ...s.dot, background: isOnline ? '#10b981' : '#ef4444' }} />
          {isOnline ? 'Online' : 'Offline'}
        </span>
        {connType && connType !== 'OFFLINE' && (
          <span style={s.tag}>{connType}</span>
        )}
      </div>
      <div style={s.right}>
        <span style={{ ...s.stat, color: batteryColor }}>
          {battery === '--' ? '🔋 --' : `🔋 ${battery}%`}
        </span>
        <span style={s.divider} />
        <span style={{ ...s.stat, color: isLocked ? '#f87171' : '#8a8f98' }}>
          {isLocked ? '🔒 Locked' : '🔓 Unlocked'}
        </span>
      </div>
    </div>
  );
}

const s = {
  bar: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '8px 24px',
    background: '#0f1011',
    borderBottom: '1px solid rgba(255,255,255,0.05)',
    fontFamily: "'Inter Variable', Inter, -apple-system, sans-serif",
    fontFeatureSettings: '"cv01","ss03"',
  },
  left: { display: 'flex', alignItems: 'center', gap: 10 },
  right: { display: 'flex', alignItems: 'center', gap: 10 },
  deviceName: { fontSize: 13, fontWeight: 510, color: '#d0d6e0', letterSpacing: '-0.13px' },
  divider: { width: 1, height: 14, background: 'rgba(255,255,255,0.08)' },
  pill: {
    display: 'inline-flex', alignItems: 'center', gap: 5,
    padding: '2px 8px', borderRadius: 9999,
    border: '1px solid', fontSize: 12, fontWeight: 510,
  },
  dot: { width: 6, height: 6, borderRadius: '50%' },
  tag: {
    padding: '2px 8px', borderRadius: 9999,
    background: 'rgba(255,255,255,0.05)',
    border: '1px solid rgba(255,255,255,0.08)',
    fontSize: 11, fontWeight: 510, color: '#62666d',
    textTransform: 'uppercase', letterSpacing: '0.04em',
  },
  stat: { fontSize: 13, fontWeight: 400, color: '#8a8f98' },
};
