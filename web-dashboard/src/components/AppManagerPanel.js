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
        <span style={s.heading}>App Controls</span>
        <span style={s.count}>{controls.length} rules</span>
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
  container: {
    padding: 24,
    fontFamily: "'Inter Variable', Inter, -apple-system, sans-serif",
    fontFeatureSettings: '"cv01","ss03"',
  },
  header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 },
  heading: { fontSize: 15, fontWeight: 590, color: '#f7f8f8', letterSpacing: '-0.165px' },
  count: {
    padding: '2px 8px', borderRadius: 9999,
    background: 'rgba(255,255,255,0.05)',
    border: '1px solid rgba(255,255,255,0.08)',
    fontSize: 12, fontWeight: 510, color: '#62666d',
  },
  addRow: { display: 'flex', gap: 8, marginBottom: 16 },
  input: {
    flex: 1, padding: '9px 14px',
    background: 'rgba(255,255,255,0.02)',
    border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: 6, fontSize: 13, color: '#f7f8f8',
    fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
    outline: 'none',
  },
  addBtn: {
    padding: '9px 16px',
    background: '#5e6ad2', color: '#fff',
    border: 'none', borderRadius: 6,
    fontSize: 13, fontWeight: 510, cursor: 'pointer',
    fontFamily: 'inherit', whiteSpace: 'nowrap',
  },
  list: {
    background: 'rgba(255,255,255,0.02)',
    border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: 8, overflow: 'hidden',
  },
  row: {
    display: 'flex', alignItems: 'center',
    justifyContent: 'space-between',
    padding: '11px 16px',
  },
  rowLeft: { display: 'flex', alignItems: 'center', gap: 10, minWidth: 0 },
  pkg: {
    fontSize: 13, color: '#d0d6e0',
    fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace',
    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
  },
  badge: {
    padding: '2px 7px', borderRadius: 4,
    fontSize: 11, fontWeight: 590, flexShrink: 0,
    textTransform: 'uppercase', letterSpacing: '0.02em',
    border: '1px solid',
  },
  badgeBlocked: { background: 'rgba(239,68,68,0.1)', color: '#f87171', borderColor: 'rgba(239,68,68,0.2)' },
  badgeAllowed: { background: 'rgba(16,185,129,0.1)', color: '#34d399', borderColor: 'rgba(16,185,129,0.2)' },
  toggleBtn: {
    padding: '5px 14px', borderRadius: 6,
    fontSize: 12, fontWeight: 510, cursor: 'pointer',
    border: '1px solid', fontFamily: 'inherit', flexShrink: 0,
  },
  toggleUnblock: {
    background: 'rgba(255,255,255,0.04)', color: '#d0d6e0',
    borderColor: 'rgba(255,255,255,0.08)',
  },
  toggleBlock: {
    background: 'rgba(239,68,68,0.1)', color: '#f87171',
    borderColor: 'rgba(239,68,68,0.2)',
  },
  empty: { padding: '32px 24px', color: '#62666d', fontSize: 14, textAlign: 'center' },
};
