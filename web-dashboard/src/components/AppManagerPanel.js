import React, { useState, useEffect } from 'react';
import { appApi } from '../services/api';

export default function AppManagerPanel({ deviceId, onBlockApp }) {
  const [controls, setControls] = useState([]);
  const [loading, setLoading] = useState(true);
  const [newPkg, setNewPkg] = useState('');
  const [adding, setAdding] = useState(false);

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
        const { data } = await appApi.setControl({ deviceId, packageName, controlType: 'BLOCKED' });
        setControls(prev => [...prev.filter(c => c.packageName !== packageName), data]);
      }
    } catch {
      alert('Failed to update app control');
    }
  };

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!newPkg.trim()) return;
    setAdding(true);
    try {
      const { data } = await appApi.setControl({ deviceId, packageName: newPkg.trim(), controlType: 'BLOCKED' });
      setControls(prev => [...prev.filter(c => c.packageName !== newPkg.trim()), data]);
      if (onBlockApp) onBlockApp(newPkg.trim());
      setNewPkg('');
    } catch {
      alert('Failed to block app');
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <div style={s.empty}>Loading app controls…</div>;

  return (
    <div style={s.container}>
      <div style={s.header}>
        <h2 style={s.heading}>App Controls</h2>
        <span style={s.count}>{controls.length}</span>
      </div>

      <form onSubmit={handleAdd} style={s.addRow}>
        <input
          style={s.input}
          placeholder="com.example.app"
          value={newPkg}
          onChange={e => setNewPkg(e.target.value)}
        />
        <button style={{ ...s.addBtn, opacity: adding ? 0.6 : 1 }} type="submit" disabled={adding}>
          Block App
        </button>
      </form>

      {controls.length === 0 ? (
        <div style={s.empty}>No app rules set. All apps are currently allowed.</div>
      ) : (
        <div style={s.list}>
          {controls.map((ctrl, i) => {
            const blocked = ctrl.controlType === 'BLOCKED';
            return (
              <div key={ctrl.id} style={{ ...s.row, borderTop: i === 0 ? 'none' : '1px solid rgba(255,255,255,0.05)' }}>
                <div style={s.rowLeft}>
                  <span style={s.pkg}>{ctrl.packageName}</span>
                  <span style={{ ...s.badge, ...(blocked ? s.badgeBlocked : s.badgeAllowed) }}>
                    {ctrl.controlType}
                  </span>
                </div>
                <button
                  style={{ ...s.toggleBtn, ...(blocked ? s.toggleUnblock : s.toggleBlock) }}
                  onClick={() => toggleBlock(ctrl.packageName, blocked)}
                >
                  {blocked ? 'Unblock' : 'Block'}
                </button>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

const s = {
  container: { padding: 0 },
  header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 24 },
  heading: { fontSize: 28, fontWeight: 600, color: 'var(--text-primary)', margin: 0 },
  count: {
    padding: '4px 12px', borderRadius: 980,
    background: 'var(--bg-primary)',
    fontSize: 15, fontWeight: 600, color: 'var(--text-tertiary)',
  },
  addRow: { display: 'flex', gap: 8, marginBottom: 20 },
  input: {
    flex: 1, padding: '12px 16px',
    background: 'var(--bg-primary)',
    border: '1px solid var(--border-primary)',
    borderRadius: 8, fontSize: 17, color: 'var(--text-primary)',
    fontFamily: 'ui-monospace, monospace',
  },
  addBtn: {
    padding: '12px 20px',
    background: 'var(--accent-primary)', color: '#fff',
    border: 'none', borderRadius: 980,
    fontSize: 17, fontWeight: 600, cursor: 'pointer',
    whiteSpace: 'nowrap',
  },
  list: {
    background: 'var(--bg-secondary)',
    borderRadius: 12, overflow: 'hidden',
    boxShadow: 'var(--shadow) 0px 2px 8px 0px',
  },
  row: {
    display: 'flex', alignItems: 'center',
    justifyContent: 'space-between',
    padding: '16px 20px',
    borderBottom: '1px solid var(--border-primary)',
  },
  rowLeft: { display: 'flex', alignItems: 'center', gap: 12, minWidth: 0, flex: 1 },
  pkg: {
    fontSize: 15, color: 'var(--text-primary)',
    fontFamily: 'ui-monospace, monospace',
    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
  },
  badge: {
    padding: '4px 10px', borderRadius: 980,
    fontSize: 12, fontWeight: 600, flexShrink: 0,
    textTransform: 'uppercase',
  },
  badgeBlocked: { background: 'var(--danger-bg)', color: 'var(--danger)' },
  badgeAllowed: { background: 'var(--success-bg)', color: 'var(--success)' },
  toggleBtn: {
    padding: '8px 16px', borderRadius: 980,
    fontSize: 14, fontWeight: 600, cursor: 'pointer',
    border: 'none', flexShrink: 0,
  },
  toggleUnblock: {
    background: 'var(--bg-primary)', color: 'var(--text-primary)',
  },
  toggleBlock: {
    background: 'var(--danger)', color: '#fff',
  },
  empty: { padding: '40px 24px', color: 'var(--text-tertiary)', fontSize: 17, textAlign: 'center' },
};
