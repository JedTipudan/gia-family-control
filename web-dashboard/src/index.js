import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Global styles with CSS variables for dark/light themes
const globalStyle = document.createElement('style');
globalStyle.textContent = `
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@100..900&display=swap');
  
  :root[data-theme="dark"] {
    --bg-primary: #000000;
    --bg-secondary: #1c1c1e;
    --bg-elevated: #2c2c2e;
    --bg-card: #2c2c2e;
    --border-primary: rgba(255,255,255,0.1);
    --border-subtle: rgba(255,255,255,0.08);
    --text-primary: #ffffff;
    --text-secondary: rgba(255,255,255,0.8);
    --text-tertiary: rgba(255,255,255,0.6);
    --text-quaternary: rgba(255,255,255,0.48);
    --accent-primary: #0071e3;
    --accent-hover: #0077ed;
    --accent-pressed: #006edb;
    --success: #30d158;
    --success-bg: rgba(48,209,88,0.15);
    --success-border: rgba(48,209,88,0.3);
    --danger: #ff453a;
    --danger-bg: rgba(255,69,58,0.15);
    --danger-border: rgba(255,69,58,0.3);
    --warning: #ff9f0a;
    --warning-bg: rgba(255,159,10,0.15);
    --warning-border: rgba(255,159,10,0.3);
    --shadow: rgba(0,0,0,0.5);
  }
  
  :root[data-theme="light"] {
    --bg-primary: #f5f5f7;
    --bg-secondary: #ffffff;
    --bg-elevated: #ffffff;
    --bg-card: #ffffff;
    --border-primary: rgba(0,0,0,0.1);
    --border-subtle: rgba(0,0,0,0.08);
    --text-primary: #1d1d1f;
    --text-secondary: rgba(0,0,0,0.8);
    --text-tertiary: rgba(0,0,0,0.6);
    --text-quaternary: rgba(0,0,0,0.48);
    --accent-primary: #0071e3;
    --accent-hover: #0077ed;
    --accent-pressed: #006edb;
    --success: #34c759;
    --success-bg: rgba(52,199,89,0.15);
    --success-border: rgba(52,199,89,0.3);
    --danger: #ff3b30;
    --danger-bg: rgba(255,59,48,0.15);
    --danger-border: rgba(255,59,48,0.3);
    --warning: #ff9500;
    --warning-bg: rgba(255,149,0,0.15);
    --warning-border: rgba(255,149,0,0.3);
    --shadow: rgba(0,0,0,0.22);
  }
  
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'SF Pro Display', 'Helvetica Neue', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    background: var(--bg-primary);
    color: var(--text-primary);
    transition: background 0.3s ease, color 0.3s ease;
  }
  input:focus, textarea:focus, button:focus { 
    outline: 2px solid var(--accent-primary);
    outline-offset: 2px;
  }
`;
document.head.appendChild(globalStyle);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<React.StrictMode><App /></React.StrictMode>);
