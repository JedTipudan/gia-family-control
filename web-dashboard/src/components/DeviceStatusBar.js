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

  return (
    <div style={styles.bar}>
      <div style={styles.item}>
        <span style={{...styles.dot, background: isOnline ? '#22c55e' : '#ef4444'}}/>
        {isOnline ? 'Online' : 'Offline'}
      </div>
      <div style={styles.item}>
        🔋 {battery}%
      </div>
      <div style={styles.item}>
        {isLocked ? '🔒 Locked' : '🔓 Unlocked'}
      </div>
      {deviceInfo && (
        <div style={styles.item}>
          📱 {deviceInfo.deviceName || 'Child Device'}
        </div>
      )}
    </div>
  );
}

const styles = {
  bar: { display: 'flex', gap: 24, padding: '8px 24px', background: '#1e293b',
    color: '#94a3b8', fontSize: 13 },
  item: { display: 'flex', alignItems: 'center', gap: 6 },
  dot: { width: 8, height: 8, borderRadius: '50%', display: 'inline-block' },
};
