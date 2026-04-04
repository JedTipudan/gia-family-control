import React, { useState, useEffect } from 'react';
import { commandApi } from '../services/api';

const CMD_STYLE = {
  LOCK:        { bg: 'rgba(239,68,68,0.12)',   color: '#f87171',  border: 'rgba(239,68,68,0.2)' },
  UNLOCK:      { bg: 'rgba(16,185,129,0.12)',  color: '#34d399',  border: 'rgba(16,185,129,0.2)' },
  BLOCK_APP:   { bg: 'rgba(245,158,11,0.12)',  color: '#fbbf24',  border: 'rgba(245,158,11,0.2)' },
  UNBLOCK_APP: { bg: 'rgba(99,102,241,0.12)',  color: '#818cf8',  border: 'rgba(99,102,241,0.2)' },
  SOS:         { bg: 'rgba(236,72,153,0.12)',  color: '#f472b6',  border: 'rgba(236,72,153,0.2)' },
  EMERGENCY:   { bg: 'rgba(220,38,38,0.15)',   color: '#fca5a5',  border: 'rgba(220,38,38,0.25)' },
};

const STATUS_COLOR = { SENT: '#10b981', PENDING: '#f59e0b', FAILED: '#ef4444' };

export default function ActivityLog({ deviceId }) {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    commandApi.getHistory(deviceId)
      .then(({ data }) => setLogs(data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [deviceId]);

  if (loading) return <div style={s.empty}>Loading activity…</div>;

  return (
    <div style={s.container}>
      <div style={s.header}>
        <h2 style={s.heading}>Activity Log</h2>
        <span style={s.count}>{logs.length}</span>
      </div>
      {logs.length === 0 ? (
        <div style={s.empty}>No activity recorded yet.</div>
      ) : (
        <div style={s.list}>
          {logs.map((log, i) => {
            const cs = CMD_STYLE[log.commandType] || { bg: 'rgba(255,255,255,0.05)', color: '#8a8f98', border: 'rgba(255,255,255,0.08)' };
            return (
              <div key={log.id} style={{ ...s.row, borderTop: i === 0 ? 'none' : '1px solid rgba(255,255,255,0.05)' }}>
                <span style={{ ...s.badge, background: cs.bg, color: cs.color, borderColor: cs.border }}>
                  {log.commandType}
                </span>
                {log.packageName && (
                  <span style={s.pkg}>{log.packageName}</span>
                )}
                <span style={{ ...s.status, color: STATUS_COLOR[log.status] || '#8a8f98' }}>
                  {log.status}
                </span>
                <span style={s.time}>{new Date(log.sentAt).toLocaleString()}</span>
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
  list: {
    background: 'var(--bg-secondary)',
    borderRadius: 12,
    overflow: 'hidden',
    boxShadow: 'var(--shadow) 0px 2px 8px 0px',
  },
  row: {
    display: 'flex', alignItems: 'center', gap: 16,
    padding: '16px 20px',
    borderBottom: '1px solid var(--border-primary)',
  },
  badge: {
    padding: '4px 10px', borderRadius: 980,
    fontSize: 12, fontWeight: 600,
    textTransform: 'uppercase', flexShrink: 0,
  },
  pkg: { fontSize: 15, color: 'var(--text-tertiary)', fontFamily: 'ui-monospace, monospace', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  status: { fontSize: 14, fontWeight: 600, flexShrink: 0 },
  time: { marginLeft: 'auto', fontSize: 14, color: 'var(--text-quaternary)', flexShrink: 0 },
  empty: { padding: '40px 24px', color: 'var(--text-tertiary)', fontSize: 17, textAlign: 'center' },
};
