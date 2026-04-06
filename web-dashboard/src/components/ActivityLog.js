import React, { useState, useEffect } from 'react';
import { commandApi } from '../services/api';

const CMD_COLOR = {
  LOCK:               { bg: 'rgba(239,68,68,0.1)',   color: '#f87171',  border: 'rgba(239,68,68,0.2)' },
  UNLOCK:             { bg: 'rgba(16,185,129,0.1)',  color: '#34d399',  border: 'rgba(16,185,129,0.2)' },
  BLOCK_APP:          { bg: 'rgba(245,158,11,0.1)',  color: '#fbbf24',  border: 'rgba(245,158,11,0.2)' },
  UNBLOCK_APP:        { bg: 'rgba(99,102,241,0.1)',  color: '#818cf8',  border: 'rgba(99,102,241,0.2)' },
  ENABLE_LAUNCHER:    { bg: 'rgba(251,191,36,0.1)',  color: '#fbbf24',  border: 'rgba(251,191,36,0.2)' },
  DISABLE_LAUNCHER:   { bg: 'rgba(107,114,128,0.1)', color: '#9ca3af',  border: 'rgba(107,114,128,0.2)' },
  GRANT_TEMP_ACCESS:  { bg: 'rgba(16,185,129,0.1)',  color: '#34d399',  border: 'rgba(16,185,129,0.2)' },
  SOS:                { bg: 'rgba(236,72,153,0.1)',  color: '#f472b6',  border: 'rgba(236,72,153,0.2)' },
  SET_PIN:            { bg: 'rgba(94,92,230,0.1)',   color: '#a5b4fc',  border: 'rgba(94,92,230,0.2)' },
};

const STATUS_COLOR = { SENT: '#34d399', PENDING: '#fbbf24', FAILED: '#f87171' };

export default function ActivityLog({ deviceId }) {
  const [logs, setLogs]       = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    commandApi.getHistory(deviceId)
      .then(({ data }) => setLogs(data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [deviceId]);

  return (
    <div style={s.wrap}>
      <div style={s.header}>
        <div>
          <h2 style={s.heading}>Activity Log</h2>
          <p style={s.sub}>All commands sent to the child device</p>
        </div>
        <span style={s.badge}>{logs.length} entries</span>
      </div>

      {loading ? (
        <div style={s.empty}>Loading activity…</div>
      ) : logs.length === 0 ? (
        <div style={s.empty}>No activity recorded yet.</div>
      ) : (
        <div style={s.table}>
          <div style={s.tableHead}>
            <span style={{ ...s.th, width: 160 }}>Command</span>
            <span style={{ ...s.th, flex: 1 }}>Details</span>
            <span style={{ ...s.th, width: 90 }}>Status</span>
            <span style={{ ...s.th, width: 160, textAlign: 'right' }}>Time</span>
          </div>
          {logs.map((log, i) => {
            const cs = CMD_COLOR[log.commandType] || { bg: 'rgba(255,255,255,0.05)', color: '#6b7280', border: 'rgba(255,255,255,0.08)' };
            return (
              <div key={log.id} style={{ ...s.row, borderTop: i === 0 ? 'none' : '1px solid var(--border-primary)' }}>
                <div style={{ width: 160 }}>
                  <span style={{ ...s.cmdBadge, background: cs.bg, color: cs.color, border: `1px solid ${cs.border}` }}>
                    {log.commandType}
                  </span>
                </div>
                <span style={{ ...s.detail, flex: 1 }}>
                  {log.packageName || log.metadata || '—'}
                </span>
                <span style={{ ...s.status, width: 90, color: STATUS_COLOR[log.status] || '#6b7280' }}>
                  {log.status}
                </span>
                <span style={{ ...s.time, width: 160, textAlign: 'right' }}>
                  {new Date(log.sentAt).toLocaleString()}
                </span>
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
  table: { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 10, overflow: 'hidden' },
  tableHead: { display: 'flex', alignItems: 'center', padding: '10px 16px', borderBottom: '1px solid var(--border-primary)', background: 'var(--bg-tertiary)' },
  th: { fontSize: 11, fontWeight: 600, color: 'var(--text-tertiary)', textTransform: 'uppercase', letterSpacing: '0.07em' },
  row: { display: 'flex', alignItems: 'center', padding: '12px 16px', gap: 0 },
  cmdBadge: { display: 'inline-block', padding: '3px 8px', borderRadius: 6, fontSize: 11, fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.05em' },
  detail: { fontSize: 13, color: 'var(--text-tertiary)', fontFamily: 'ui-monospace, monospace', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', paddingRight: 12 },
  status: { fontSize: 13, fontWeight: 600 },
  time: { fontSize: 12, color: 'var(--text-tertiary)' },
  empty: { padding: '48px 24px', color: 'var(--text-tertiary)', fontSize: 14, textAlign: 'center', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 10 },
};
