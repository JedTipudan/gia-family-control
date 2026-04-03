import React, { useState, useEffect } from 'react';
import { appApi } from '../services/api';

export default function AppManagerPanel({ deviceId, onBlockApp }) {
  const [controls, setControls] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    appApi.getControls(deviceId)
      .then(({ data }) => setControls(data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [deviceId]);

  const toggleBlock = async (packageName, currentlyBlocked) => {
    try {
      if (currentlyBlocked) {
        await appApi.removeControl(deviceId, packageName);
        setControls(prev => prev.filter(c => c.packageName !== packageName));
      } else {
        const { data } = await appApi.setControl({
          deviceId, packageName, controlType: 'BLOCKED'
        });
        setControls(prev => [...prev.filter(c => c.packageName !== packageName), data]);
      }
    } catch {
      alert('Failed to update app control');
    }
  };

  if (loading) return <div style={styles.loading}>Loading app controls...</div>;

  return (
    <div style={styles.container}>
      <h3 style={styles.heading}>App Controls</h3>
      {controls.length === 0 ? (
        <p style={styles.empty}>No app controls set. Apps are currently unrestricted.</p>
      ) : (
        <div style={styles.list}>
          {controls.map(ctrl => (
            <div key={ctrl.id} style={styles.item}>
              <div>
                <div style={styles.pkgName}>{ctrl.packageName}</div>
                <span style={{...styles.badge,
                  background: ctrl.controlType === 'BLOCKED' ? '#fee2e2' : '#dcfce7',
                  color: ctrl.controlType === 'BLOCKED' ? '#ef4444' : '#22c55e'}}>
                  {ctrl.controlType}
                </span>
              </div>
              <button style={styles.unblockBtn}
                onClick={() => toggleBlock(ctrl.packageName, ctrl.controlType === 'BLOCKED')}>
                {ctrl.controlType === 'BLOCKED' ? 'Unblock' : 'Block'}
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const styles = {
  container: { padding: 24 },
  heading: { marginBottom: 16, color: '#1a1a2e' },
  loading: { padding: 24, color: '#64748b' },
  empty: { color: '#64748b' },
  list: { display: 'flex', flexDirection: 'column', gap: 8 },
  item: { display: 'flex', justifyContent: 'space-between', alignItems: 'center',
    padding: '12px 16px', background: '#fff', borderRadius: 8,
    boxShadow: '0 1px 4px rgba(0,0,0,0.08)' },
  pkgName: { fontWeight: 'bold', marginBottom: 4 },
  badge: { padding: '2px 8px', borderRadius: 12, fontSize: 12 },
  unblockBtn: { padding: '6px 16px', background: '#4f46e5', color: '#fff',
    border: 'none', borderRadius: 6, cursor: 'pointer' },
};
