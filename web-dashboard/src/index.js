import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './styles/linear-theme.css';

const globalStyle = document.createElement('style');
globalStyle.textContent = `
  /* ── Responsive layout classes ── */

  /* Mobile ≤ 768px */
  @media (max-width: 768px) {
    .gia-shell {
      flex-direction: column !important;
      height: auto !important;
      min-height: 100vh !important;
    }
    .gia-sidebar {
      width: 100% !important;
      height: auto !important;
      flex-direction: row !important;
      border-right: none !important;
      border-top: 1px solid var(--border-primary) !important;
      overflow-x: auto !important;
      overflow-y: hidden !important;
      order: 2;
      flex-shrink: 0 !important;
    }
    .gia-main { order: 1; overflow: auto !important; }
    .gia-brand { display: none !important; }
    .gia-nav {
      flex-direction: row !important;
      padding: 4px 6px !important;
      gap: 2px !important;
      flex: 1;
    }
    .gia-nav-item {
      flex-direction: column !important;
      padding: 6px 8px !important;
      font-size: 10px !important;
      gap: 2px !important;
      white-space: nowrap !important;
      min-width: 52px;
      justify-content: center;
      align-items: center;
    }
    .gia-nav-icon { width: auto !important; }
    .gia-nav-label { font-size: 10px !important; }
    .gia-qc { display: none !important; }
    .gia-collapse-btn { display: none !important; }
    .gia-side-footer {
      flex-direction: row !important;
      padding: 4px 8px !important;
      margin-top: 0 !important;
      border-top: none !important;
      border-left: 1px solid var(--border-primary) !important;
      flex-shrink: 0;
    }
    .gia-side-user { display: none !important; }
    .gia-topbar {
      padding: 0 12px !important;
      height: 48px !important;
    }
    .gia-content { padding: 12px !important; }
    .gia-overview-grid {
      grid-template-columns: repeat(2, 1fr) !important;
    }
    .gia-overview-map { grid-column: 1 / -1 !important; }
    .gia-controls-card { grid-column: 1 / -1 !important; }
    .gia-login-card {
      width: calc(100vw - 32px) !important;
      max-width: 420px !important;
      padding: 24px 20px !important;
    }
    .gia-pair-row {
      grid-template-columns: 1fr !important;
    }
    .gia-device-row { flex-wrap: wrap !important; gap: 8px !important; }
    .gia-device-stats { flex-wrap: wrap !important; gap: 4px !important; }
    .gia-topbar-chips span:nth-child(n+3) { display: none !important; }
    .gia-mini-map { min-height: 200px !important; }
    .gia-full-map { min-height: 300px !important; }
    .gia-ctrl-grid { padding: 10px !important; gap: 6px !important; }
    .gia-ctrl-btn { padding: 9px 12px !important; font-size: 13px !important; }
  }

  /* Tablet 769–1024px */
  @media (min-width: 769px) and (max-width: 1024px) {
    .gia-overview-grid {
      grid-template-columns: repeat(2, 1fr) !important;
    }
    .gia-overview-map { grid-column: 1 / -1 !important; }
    .gia-controls-card { grid-column: 1 / -1 !important; }
    .gia-sidebar { width: 180px !important; }
  }
`;
document.head.appendChild(globalStyle);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<React.StrictMode><App /></React.StrictMode>);
