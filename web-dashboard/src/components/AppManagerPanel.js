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
  container: { 
    padding: 24,
    background: 'var(--bg-secondary)'
  },
  heading: { 
    marginBottom: 16, 
    color: 'var(--text-primary)',
    fontSize: 18,
    fontWeight: 600,
    letterSpacing: 'var(--letter-spacing-tight)'
  },
  loading: { 
    padding: 24, 
    color: 'var(--text-tertiary)',
    fontSize: 13
  },
  empty: { 
    color: 'var(--text-tertiary)',
    fontSize: 13,
    padding: 'var(--space-4)',
    background: 'var(--bg-tertiary)',
    borderRadius: 'var(--radius-md)',
    border: '1px solid var(--border-primary)'
  },
  list: { 
    display: 'flex', 
    flexDirection: 'column', 
    gap: 8 
  },
  item: { 
    display: 'flex', 
    justifyContent: 'space-between', 
    alignItems: 'center',
    padding: '12px 16px', 
    background: 'var(--bg-elevated)', 
    borderRadius: 'var(--radius-md)',
    border: '1px solid var(--border-primary)',
    transition: 'all 0.15s ease'
  },
  pkgName: { 
    fontWeight: 500, 
    marginBottom: 4,
    color: 'var(--text-primary)',
    fontSize: 13,
    fontFamily: 'monospace'
  },
  badge: { 
    padding: '2px 8px', 
    borderRadius: 'var(--radius-sm)', 
    fontSize: 11,
    fontWeight: 500,
    letterSpacing: '0.02em',
    textTransform: 'uppercase'
  },
  unblockBtn: { 
    padding: '6px 12px', 
    background: 'var(--accent-primary)', 
    color: 'var(--text-primary)',
    border: 'none', 
    borderRadius: 'var(--radius-sm)',
    fontSize: 12,
    fontWeight: 500
  },
};
