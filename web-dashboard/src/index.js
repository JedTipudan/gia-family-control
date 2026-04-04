import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Global styles with CSS variables for dark/light themes
const globalStyle = document.createElement('style');
globalStyle.textContent = `
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@100..900&display=swap');
  
  :root[data-theme="dark"] {
    --bg-primary: #08090a;
    --bg-secondary: #0f1011;
    --bg-elevated: #16171a;
    --bg-card: #16171a;
    --border-primary: #1f2023;
    --border-subtle: rgba(255,255,255,0.08);
    --text-primary: #f7f8f8;
    --text-secondary: #d0d6e0;
    --text-tertiary: #8a8f98;
    --text-quaternary: #62666d;
    --accent-primary: #5e6ad2;
    --accent-hover: #6b76db;
    --success: #10b981;
    --success-bg: rgba(16,185,129,0.12);
    --success-border: rgba(16,185,129,0.25);
    --danger: #ef4444;
    --danger-bg: rgba(239,68,68,0.12);
    --danger-border: rgba(239,68,68,0.2);
    --shadow: rgba(0,0,0,0.4);
  }
  
  :root[data-theme="light"] {
    --bg-primary: #ffffff;
    --bg-secondary: #f6f9fc;
    --bg-elevated: #ffffff;
    --bg-card: #ffffff;
    --border-primary: #e5edf5;
    --border-subtle: #e5edf5;
    --text-primary: #061b31;
    --text-secondary: #273951;
    --text-tertiary: #64748d;
    --text-quaternary: #8a96a3;
    --accent-primary: #533afd;
    --accent-hover: #4434d4;
    --success: #15be53;
    --success-bg: rgba(21,190,83,0.2);
    --success-border: rgba(21,190,83,0.4);
    --danger: #ea2261;
    --danger-bg: rgba(234,34,97,0.1);
    --danger-border: rgba(234,34,97,0.3);
    --shadow: rgba(50,50,93,0.25);
  }
  
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: 'Inter', -apple-system, system-ui, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    background: var(--bg-primary);
    color: var(--text-primary);
    transition: background 0.2s, color 0.2s;
  }
  input:focus, textarea:focus, button:focus { outline: none; }
`;
document.head.appendChild(globalStyle);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<React.StrictMode><App /></React.StrictMode>);
