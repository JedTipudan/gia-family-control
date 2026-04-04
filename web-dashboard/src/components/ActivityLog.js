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
        <span style={s.heading}>Activity Log</span>
        <span style={s.count}>{logs.length} events</span>
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
  list: {
    background: 'rgba(255,255,255,0.02)',
    border: '1px solid rgba(255,255,255,0.08)',
    borderRadius: 8,
    overflow: 'hidden',
  },
  row: {
    display: 'flex', alignItems: 'center', gap: 12,
    padding: '11px 16px',
  },
  badge: {
    padding: '2px 8px', borderRadius: 4,
    border: '1px solid', fontSize: 11, fontWeight: 590,
    letterSpacing: '0.02em', textTransform: 'uppercase', flexShrink: 0,
  },
  pkg: { fontSize: 13, color: '#8a8f98', fontFamily: 'ui-monospace, "SF Mono", Menlo, monospace', flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  status: { fontSize: 12, fontWeight: 510, flexShrink: 0 },
  time: { marginLeft: 'auto', fontSize: 12, color: '#62666d', flexShrink: 0 },
  empty: { padding: '32px 24px', color: '#62666d', fontSize: 14, textAlign: 'center' },
};
