import React, { useState, useEffect } from 'react';
import { commandApi } from '../services/api';

const TYPE_COLORS = {
  LOCK: '#ef4444', UNLOCK: '#22c55e', BLOCK_APP: '#f59e0b',
  UNBLOCK_APP: '#3b82f6', SOS: '#ec4899', EMERGENCY: '#dc2626',
};

export default function ActivityLog({ deviceId }) {
  const [logs, setLogs] = useState([]);

  useEffect(() => {
    commandApi.getHistory(deviceId)
      .then(({ data }) => setLogs(data))
      .catch(() => {});
  }, [deviceId]);

  return (
    <div style={styles.container}>
      <h3 style={styles.heading}>Activity Log</h3>
      {logs.length === 0 ? (
        <p style={styles.empty}>No activity recorded yet.</p>
      ) : (
        <div style={styles.list}>
          {logs.map(log => (
            <div key={log.id} style={styles.item}>
              <span style={{...styles.badge, background: TYPE_COLORS[log.commandType] || '#64748b'}}>
                {log.commandType}
              </span>
              <span style={styles.status}>{log.status}</span>
              <span style={styles.time}>{new Date(log.sentAt).toLocaleString()}</span>
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
  empty: { color: '#64748b' },
  list: { display: 'flex', flexDirection: 'column', gap: 8 },
  item: { display: 'flex', alignItems: 'center', gap: 12, padding: '10px 16px',
    background: '#fff', borderRadius: 8, boxShadow: '0 1px 4px rgba(0,0,0,0.08)' },
  badge: { padding: '3px 10px', borderRadius: 12, color: '#fff', fontSize: 12, fontWeight: 'bold' },
  status: { color: '#64748b', fontSize: 13 },
  time: { marginLeft: 'auto', color: '#94a3b8', fontSize: 12 },
};
