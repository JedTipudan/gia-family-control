import React, { useState, useEffect } from 'react';
import { appApi } from '../services/api';

export default function AppManagerPanel({ deviceId, onBlockApp }) {
  const [controls, setControls] = useState([]);
  const [loading, setLoading]   = useState(true);
  const [newPkg, setNewPkg]     = useState('');
  const [adding, setAdding]     = useState(false);

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

  return (
    <div style={s.wrap}>
      {/* Header */}
      <div style={s.header}>
        <div>
          <h2 style={s.heading}>App Control</h2>
          <p style={s.sub}>Block or allow apps on the child device</p>
        </div>
        <span style={s.badge}>{controls.length} rules</span>
      </div>

      {/* Add form */}
      <form onSubmit={handleAdd} style={s.addRow}>
        <input
          style={s.input}
          placeholder="Package name (e.g. com.example.app)"
          value={newPkg}
          onChange={e => setNewPkg(e.target.value)}
        />
        <button style={{ ...s.addBtn, opacity: adding ? 0.6 : 1 }} type="submit" disabled={adding}>
          {adding ? '…' : '+ Block App'}
        </button>
      </form>

      {/* Table */}
      {loading ? (
        <div style={s.empty}>Loading app rules…</div>
      ) : controls.length === 0 ? (
        <div style={s.empty}>No rules set — all apps are currently allowed.</div>
      ) : (
        <div style={s.table}>
          <div style={s.tableHead}>
            <span style={{ ...s.th, flex: 1 }}>Package</span>
            <span style={{ ...s.th, width: 100 }}>Status</span>
            <span style={{ ...s.th, width: 100, textAlign: 'right' }}>Action</span>
          </div>
          {controls.map((ctrl, i) => {
            const blocked = ctrl.controlType === 'BLOCKED';
            return (
              <div key={ctrl.id} style={{ ...s.row, borderTop: i === 0 ? 'none' : '1px solid var(--border-primary)' }}>
                <span style={{ ...s.pkg, flex: 1 }}>{ctrl.packageName}</span>
                <span style={{ ...s.pill, ...(blocked ? s.pillBlocked : s.pillAllowed), width: 100 }}>
                  {blocked ? 'Blocked' : 'Allowed'}
                </span>
                <div style={{ width: 100, display: 'flex', justifyContent: 'flex-end' }}>
                  <button
                    style={{ ...s.toggleBtn, ...(blocked ? s.btnUnblock : s.btnBlock) }}
                    onClick={() => toggleBlock(ctrl.packageName, blocked)}
                  >
                    {blocked ? 'Unblock' : 'Block'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

const s = {
  wrap: { display: 'flex', flexDirection: 'column', gap: 20 },
  header: { display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' },
  heading: { fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', margin: '0 0 4px' },
  sub: { fontSize: 13, color: 'var(--text-tertiary)', margin: 0 },
  badge: { padding: '4px 12px', borderRadius: 980, background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', fontSize: 13, fontWeight: 600, color: 'var(--text-tertiary)', flexShrink: 0 },
  addRow: { display: 'flex', gap: 8 },
  input: { flex: 1, padding: '10px 14px', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 8, fontSize: 14, color: 'var(--text-primary)', fontFamily: 'ui-monospace, monospace' },
  addBtn: { padding: '10px 18px', background: 'var(--accent-primary)', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap' },
  table: { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 10, overflow: 'hidden' },
  tableHead: { display: 'flex', alignItems: 'center', padding: '10px 16px', borderBottom: '1px solid var(--border-primary)', background: 'var(--bg-tertiary)' },
  th: { fontSize: 11, fontWeight: 600, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.07em' },
  row: { display: 'flex', alignItems: 'center', padding: '12px 16px' },
  pkg: { fontSize: 13, color: 'var(--text-secondary)', fontFamily: 'ui-monospace, monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  pill: { display: 'inline-flex', alignItems: 'center', justifyContent: 'center', padding: '3px 10px', borderRadius: 980, fontSize: 12, fontWeight: 600 },
  pillBlocked: { background: 'rgba(239,68,68,0.1)', color: '#f87171', border: '1px solid rgba(239,68,68,0.2)' },
  pillAllowed: { background: 'rgba(16,185,129,0.1)', color: '#34d399', border: '1px solid rgba(16,185,129,0.2)' },
  toggleBtn: { padding: '6px 14px', borderRadius: 6, fontSize: 13, fontWeight: 600, cursor: 'pointer', border: 'none' },
  btnUnblock: { background: 'var(--bg-tertiary)', color: 'var(--text-secondary)' },
  btnBlock: { background: 'rgba(239,68,68,0.12)', color: '#f87171' },
  empty: { padding: '48px 24px', color: 'var(--text-tertiary)', fontSize: 14, textAlign: 'center', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 10 },
};
