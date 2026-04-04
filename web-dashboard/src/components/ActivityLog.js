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
    alignItems: 'center', 
    gap: 12, 
    padding: '10px 16px',
    background: 'var(--bg-elevated)', 
    borderRadius: 'var(--radius-md)',
    border: '1px solid var(--border-primary)'
  },
  badge: { 
    padding: '4px 10px', 
    borderRadius: 'var(--radius-sm)', 
    color: 'var(--text-primary)', 
    fontSize: 11,
    fontWeight: 500,
    letterSpacing: '0.02em',
    textTransform: 'uppercase'
  },
  status: { 
    color: 'var(--text-tertiary)', 
    fontSize: 12 
  },
  time: { 
    marginLeft: 'auto', 
    color: 'var(--text-tertiary)', 
    fontSize: 11,
    fontFamily: 'monospace'
  },
};
