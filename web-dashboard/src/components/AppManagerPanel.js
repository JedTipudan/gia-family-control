import React, { useState, useEffect } from 'react';
import { appApi, commandApi } from '../services/api';

export default function AppManagerPanel({ deviceId }) {
  const [installedApps, setInstalledApps] = useState([]);
  const [controls, setControls]           = useState([]);
  const [loading, setLoading]             = useState(true);
  const [search, setSearch]               = useState('');
  const [newPkg, setNewPkg]               = useState('');
  const [adding, setAdding]               = useState(false);
  const [showSystem, setShowSystem]       = useState(false);

  const loadAll = async () => {
    try {
      const [appsRes, ctrlRes] = await Promise.all([
        appApi.getInstalled(deviceId),
        appApi.getControls(deviceId),
      ]);
      setInstalledApps(appsRes.data || []);
      setControls(ctrlRes.data || []);
    } catch {}
    finally { setLoading(false); }
  };

  useEffect(() => { loadAll(); }, [deviceId]);

  // Build sets for quick lookup
  const blockedPkgs = new Set(controls.filter(c => c.controlType === 'BLOCKED').map(c => c.packageName));
  const hiddenPkgs  = new Set(controls.filter(c => c.controlType === 'HIDDEN').map(c => c.packageName));

  // Merge: installed apps + any manually added packages not in installed list
  const manualPkgs = controls
    .map(c => c.packageName)
    .filter(pkg => !installedApps.find(a => a.packageName === pkg));

  const allApps = [
    ...installedApps.map(a => ({ packageName: a.packageName, appName: a.appName, isSystem: a.isSystem })),
    ...manualPkgs.map(pkg => ({ packageName: pkg, appName: null, isSystem: false })),
  ];

  const filtered = allApps.filter(a => {
    if (!showSystem && a.isSystem) return false;
    return !search ||
      a.packageName.toLowerCase().includes(search.toLowerCase()) ||
      (a.appName && a.appName.toLowerCase().includes(search.toLowerCase()));
  });

  const toggleBlock = async (packageName, currentlyBlocked) => {
    try {
      if (currentlyBlocked) {
        await appApi.removeControl(deviceId, packageName, 'BLOCKED');
        setControls(prev => prev.filter(c => !(c.packageName === packageName && c.controlType === 'BLOCKED')));
        await commandApi.sendApp(deviceId, 'UNBLOCK_APP', packageName);
      } else {
        const { data } = await appApi.setControl({ deviceId, packageName, controlType: 'BLOCKED' });
        setControls(prev => [...prev.filter(c => !(c.packageName === packageName && c.controlType === 'BLOCKED')), data]);
        await commandApi.sendApp(deviceId, 'BLOCK_APP', packageName);
      }
    } catch (e) { alert('Failed to update block: ' + (e?.response?.data?.message || e?.message || 'error')); }
  };

  const toggleHide = async (packageName, currentlyHidden) => {
    try {
      if (currentlyHidden) {
        await appApi.removeControl(deviceId, packageName, 'HIDDEN');
        setControls(prev => prev.filter(c => !(c.packageName === packageName && c.controlType === 'HIDDEN')));
        await commandApi.sendApp(deviceId, 'UNHIDE_APP', packageName);
      } else {
        const { data } = await appApi.setControl({ deviceId, packageName, controlType: 'HIDDEN' });
        setControls(prev => [...prev.filter(c => !(c.packageName === packageName && c.controlType === 'HIDDEN')), data]);
        await commandApi.sendApp(deviceId, 'HIDE_APP', packageName);
      }
    } catch (e) { alert('Failed to update hide: ' + (e?.response?.data?.message || e?.message || 'error')); }
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
          <p style={s.sub}>
            {installedApps.length > 0
              ? `${installedApps.length} installed · ${blockedPkgs.size} blocked · ${hiddenPkgs.size} hidden`
              : 'Block or hide apps on the child device'}
          </p>
        </div>
        <span style={s.badge}>{filtered.length}</span>
      </div>

      {/* Search + System toggle */}
      <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
        <input
          style={{ ...s.input, flex: 1 }}
          placeholder="Search by app name or package…"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <button
          style={{ ...s.toggleBtn, ...(showSystem ? s.btnBlocked : s.btnOff), whiteSpace: 'nowrap', padding: '10px 14px' }}
          onClick={() => setShowSystem(v => !v)}
          title="Toggle system apps"
        >
          {showSystem ? '⚙️ Hide System' : '⚙️ Show System'}
        </button>
      </div>

      {/* Add by package name */}
      <form onSubmit={handleAdd} style={s.addRow}>
        <input
          style={s.input}
          placeholder="Package name to block (e.g. com.example.app)"
          value={newPkg}
          onChange={e => setNewPkg(e.target.value)}
        />
        <button style={{ ...s.addBtn, opacity: adding ? 0.6 : 1 }} type="submit" disabled={adding}>
          {adding ? '…' : '+ Block'}
        </button>
      </form>

      {loading ? (
        <div style={s.empty}>Loading apps…</div>
      ) : filtered.length === 0 ? (
        <div style={s.empty}>
          {installedApps.length === 0
            ? 'No apps synced yet. Child must tap "Start Monitoring" first.'
            : 'No apps match your search.'}
        </div>
      ) : (
        <div style={s.table}>
          <div style={s.tableHead}>
            <span style={{ ...s.th, flex: 1 }}>App</span>
            <span style={{ ...s.th, width: 80, textAlign: 'center' }}>Block</span>
            <span style={{ ...s.th, width: 80, textAlign: 'center' }}>Hide</span>
          </div>
          {filtered.map((app, i) => {
            const blocked = blockedPkgs.has(app.packageName);
            const hidden  = hiddenPkgs.has(app.packageName);
            return (
              <div key={app.packageName} style={{ ...s.row, borderTop: i === 0 ? 'none' : '1px solid var(--border-primary)' }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  {app.appName && (
                    <div style={s.appName}>{app.appName}</div>
                  )}
                  <div style={s.pkg}>{app.packageName}</div>
                </div>
                <div style={{ width: 80, display: 'flex', justifyContent: 'center' }}>
                  <button
                    style={{ ...s.toggleBtn, ...(blocked ? s.btnBlocked : s.btnOff) }}
                    onClick={() => toggleBlock(app.packageName, blocked)}
                  >
                    {blocked ? '🚫 On' : 'Off'}
                  </button>
                </div>
                <div style={{ width: 80, display: 'flex', justifyContent: 'center' }}>
                  <button
                    style={{ ...s.toggleBtn, ...(hidden ? s.btnHidden : s.btnOff) }}
                    onClick={() => toggleHide(app.packageName, hidden)}
                  >
                    {hidden ? '👁 On' : 'Off'}
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
  wrap:      { display: 'flex', flexDirection: 'column', gap: 12 },
  header:    { display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 4 },
  heading:   { fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', margin: '0 0 4px' },
  sub:       { fontSize: 13, color: 'var(--text-tertiary)', margin: 0 },
  badge:     { padding: '4px 12px', borderRadius: 980, background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', fontSize: 13, fontWeight: 600, color: 'var(--text-tertiary)', flexShrink: 0 },
  input:     { width: '100%', padding: '10px 14px', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 8, fontSize: 14, color: 'var(--text-primary)', fontFamily: 'ui-monospace, monospace', boxSizing: 'border-box' },
  addRow:    { display: 'flex', gap: 8 },
  addBtn:    { padding: '10px 18px', background: 'var(--accent-primary)', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer', whiteSpace: 'nowrap' },
  table:     { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 10, overflow: 'hidden' },
  tableHead: { display: 'flex', alignItems: 'center', padding: '10px 16px', borderBottom: '1px solid var(--border-primary)', background: 'var(--bg-tertiary)' },
  th:        { fontSize: 11, fontWeight: 600, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.07em' },
  row:       { display: 'flex', alignItems: 'center', padding: '10px 16px' },
  appName:   { fontSize: 13, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 2 },
  pkg:       { fontSize: 11, color: 'var(--text-tertiary)', fontFamily: 'ui-monospace, monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  toggleBtn: { padding: '5px 10px', borderRadius: 6, fontSize: 12, fontWeight: 600, cursor: 'pointer', border: 'none', minWidth: 56 },
  btnBlocked:{ background: 'rgba(239,68,68,0.12)', color: '#f87171' },
  btnHidden: { background: 'rgba(251,191,36,0.12)', color: '#fbbf24' },
  btnOff:    { background: 'var(--bg-tertiary)', color: 'var(--text-tertiary)' },
  empty:     { padding: '48px 24px', color: 'var(--text-tertiary)', fontSize: 14, textAlign: 'center', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 10 },
};
