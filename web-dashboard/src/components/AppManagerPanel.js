import React, { useState, useEffect } from 'react';
import { appApi, commandApi } from '../services/api';

export default function AppManagerPanel({ deviceId }) {
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

  // Group by packageName so we can show both block + hide state per app
  const appMap = {};
  controls.forEach(c => {
    if (!appMap[c.packageName]) appMap[c.packageName] = { packageName: c.packageName, blocked: false, hidden: false };
    if (c.controlType === 'BLOCKED') appMap[c.packageName].blocked = true;
    if (c.controlType === 'HIDDEN')  appMap[c.packageName].hidden  = true;
  });
  const apps = Object.values(appMap);

  const toggleBlock = async (packageName, currentlyBlocked) => {
    try {
      if (currentlyBlocked) {
        await appApi.removeControl(deviceId, packageName);
        setControls(prev => prev.filter(c => !(c.packageName === packageName && c.controlType === 'BLOCKED')));
        await commandApi.sendApp(deviceId, 'UNBLOCK_APP', packageName);
      } else {
        const { data } = await appApi.setControl({ deviceId, packageName, controlType: 'BLOCKED' });
        setControls(prev => [...prev.filter(c => !(c.packageName === packageName && c.controlType === 'BLOCKED')), data]);
        await commandApi.sendApp(deviceId, 'BLOCK_APP', packageName);
      }
    } catch { alert('Failed to update block'); }
  };

  const toggleHide = async (packageName, currentlyHidden) => {
    try {
      if (currentlyHidden) {
        await appApi.removeControl(deviceId, packageName);
        setControls(prev => prev.filter(c => !(c.packageName === packageName && c.controlType === 'HIDDEN')));
        await commandApi.sendApp(deviceId, 'UNHIDE_APP', packageName);
      } else {
        const { data } = await appApi.setControl({ deviceId, packageName, controlType: 'HIDDEN' });
        setControls(prev => [...prev.filter(c => !(c.packageName === packageName && c.controlType === 'HIDDEN')), data]);
        await commandApi.sendApp(deviceId, 'HIDE_APP', packageName);
      }
    } catch { alert('Failed to update hide'); }
  };

  const handleAdd = async (e) => {
    e.preventDefault();
    if (!newPkg.trim()) return;
    setAdding(true);
    try {
      const { data } = await appApi.setControl({ deviceId, packageName: newPkg.trim(), controlType: 'BLOCKED' });
      setControls(prev => [...prev.filter(c => !(c.packageName === newPkg.trim() && c.controlType === 'BLOCKED')), data]);
      await commandApi.sendApp(deviceId, 'BLOCK_APP', newPkg.trim());
      setNewPkg('');
    } catch { alert('Failed to block app'); }
    finally { setAdding(false); }
  };

  return (
    <div style={s.wrap}>
      <div style={s.header}>
        <div>
          <h2 style={s.heading}>App Control</h2>
          <p style={s.sub}>Block or hide apps on the child device</p>
        </div>
        <span style={s.badge}>{apps.length} apps</span>
      </div>

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

      {loading ? (
        <div style={s.empty}>Loading app rules…</div>
      ) : apps.length === 0 ? (
        <div style={s.empty}>No rules set — all apps are currently allowed and visible.</div>
      ) : (
        <div style={s.table}>
          <div style={s.tableHead}>
            <span style={{ ...s.th, flex: 1 }}>Package</span>
            <span style={{ ...s.th, width: 90, textAlign: 'center' }}>Block</span>
            <span style={{ ...s.th, width: 90, textAlign: 'center' }}>Hide</span>
          </div>
          {apps.map((app, i) => (
            <div key={app.packageName} style={{ ...s.row, borderTop: i === 0 ? 'none' : '1px solid var(--border-primary)' }}>
              <span style={{ ...s.pkg, flex: 1 }}>{app.packageName}</span>

              {/* Block toggle */}
              <div style={{ width: 90, display: 'flex', justifyContent: 'center' }}>
                <button
                  style={{ ...s.toggleBtn, ...(app.blocked ? s.btnActive : s.btnInactive) }}
                  onClick={() => toggleBlock(app.packageName, app.blocked)}
                >
                  {app.blocked ? '🚫 On' : 'Off'}
                </button>
              </div>

              {/* Hide toggle */}
              <div style={{ width: 90, display: 'flex', justifyContent: 'center' }}>
                <button
                  style={{ ...s.toggleBtn, ...(app.hidden ? s.btnHideActive : s.btnInactive) }}
                  onClick={() => toggleHide(app.packageName, app.hidden)}
                >
                  {app.hidden ? '👁 On' : 'Off'}
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const s = {
  wrap:        { display: 'flex', flexDirection: 'column', gap: 20 },
  header:      { display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between' },
  heading:     { fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', margin: '0 0 4px' },
  sub:         { fontSize: 13, color: 'var(--text-tertiary)', margin: 0 },
  badge:       { padding: '4px 12px', borderRadius: 980, background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', fontSize: 13, fontWeight: 600, color: 'var(--text-tertiary)', flexShrink: 0 },
  addRow:      { display: 'flex', gap: 8 },
  input:       { flex: 1, padding: '10px 14px', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 8, fontSize: 14, color: 'var(--text-primary)', fontFamily: 'ui-monospace, monospace' },
  addBtn:      { padding: '10px 18px', background: 'var(--accent-primary)', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap' },
  table:       { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 10, overflow: 'hidden' },
  tableHead:   { display: 'flex', alignItems: 'center', padding: '10px 16px', borderBottom: '1px solid var(--border-primary)', background: 'var(--bg-tertiary)' },
  th:          { fontSize: 11, fontWeight: 600, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.07em' },
  row:         { display: 'flex', alignItems: 'center', padding: '12px 16px' },
  pkg:         { fontSize: 13, color: 'var(--text-secondary)', fontFamily: 'ui-monospace, monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  toggleBtn:   { padding: '5px 12px', borderRadius: 6, fontSize: 12, fontWeight: 600, cursor: 'pointer', border: 'none', minWidth: 60 },
  btnActive:   { background: 'rgba(239,68,68,0.12)', color: '#f87171' },
  btnHideActive:{ background: 'rgba(251,191,36,0.12)', color: '#fbbf24' },
  btnInactive: { background: 'var(--bg-tertiary)', color: 'var(--text-tertiary)' },
  empty:       { padding: '48px 24px', color: 'var(--text-tertiary)', fontSize: 14, textAlign: 'center', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 10 },
};
