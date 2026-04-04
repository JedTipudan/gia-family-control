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
    padding: '10px 24px',
    background: 'var(--bg-secondary)',
    borderBottom: '1px solid var(--border-primary)',
  },
  left: { display: 'flex', alignItems: 'center', gap: 12 },
  right: { display: 'flex', alignItems: 'center', gap: 12 },
  deviceName: { fontSize: 15, fontWeight: 600, color: 'var(--text-primary)' },
  divider: { width: 1, height: 16, background: 'var(--border-primary)' },
  pill: {
    display: 'inline-flex', alignItems: 'center', gap: 6,
    padding: '4px 10px', borderRadius: 980,
    fontSize: 13, fontWeight: 600,
  },
  dot: { width: 8, height: 8, borderRadius: '50%' },
  tag: {
    padding: '4px 10px', borderRadius: 980,
    background: 'var(--bg-primary)',
    fontSize: 12, fontWeight: 600, color: 'var(--text-tertiary)',
    textTransform: 'uppercase',
  },
  stat: { fontSize: 15, fontWeight: 400, color: 'var(--text-secondary)' },
};
