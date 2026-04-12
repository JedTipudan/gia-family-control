import React, { useState, useCallback, useRef } from 'react';

let _addToast = null;

export function toast(message, type = 'info') {
  _addToast?.({ message, type, id: Date.now() + Math.random() });
}

export function ToastContainer() {
  const [toasts, setToasts] = useState([]);
  const timerRef = useRef({});

  _addToast = useCallback((t) => {
    setToasts(prev => [...prev, t]);
    timerRef.current[t.id] = setTimeout(() => {
      setToasts(prev => prev.filter(x => x.id !== t.id));
    }, 3500);
  }, []);

  const dismiss = (id) => {
    clearTimeout(timerRef.current[id]);
    setToasts(prev => prev.filter(x => x.id !== id));
  };

  if (!toasts.length) return null;

  return (
    <div style={s.container}>
      {toasts.map(t => (
        <div key={t.id} style={{ ...s.toast, ...COLORS[t.type] }} onClick={() => dismiss(t.id)}>
          <span>{ICONS[t.type]} {t.message}</span>
          <button style={s.close}>✕</button>
        </div>
      ))}
    </div>
  );
}

const ICONS  = { success: '✅', error: '❌', warn: '⚠️', info: 'ℹ️' };
const COLORS = {
  success: { background: '#0d2e1f', color: '#34d399', border: '1px solid #1a5c3a' },
  error:   { background: '#3b1212', color: '#f87171', border: '1px solid #5c1a1a' },
  warn:    { background: '#2e2412', color: '#fbbf24', border: '1px solid #5c4a1a' },
  info:    { background: '#12203b', color: '#818cf8', border: '1px solid #1a2e5c' },
};

const s = {
  container: { position: 'fixed', bottom: 24, right: 24, zIndex: 9999, display: 'flex', flexDirection: 'column', gap: 8, maxWidth: 360 },
  toast:     { display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, padding: '12px 16px', borderRadius: 10, fontSize: 14, fontWeight: 500, cursor: 'pointer', boxShadow: '0 4px 20px rgba(0,0,0,0.3)', animation: 'fadeIn 0.2s ease' },
  close:     { background: 'none', border: 'none', color: 'inherit', opacity: 0.6, cursor: 'pointer', fontSize: 14, padding: 0, flexShrink: 0 },
};
