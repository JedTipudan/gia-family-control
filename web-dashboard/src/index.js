import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Global styles
const globalStyle = document.createElement('style');
globalStyle.textContent = `
  @import url('https://fonts.googleapis.com/css2?family=Inter:wght@100..900&display=swap');
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: 'Inter Variable', Inter, -apple-system, system-ui, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    font-feature-settings: "cv01", "ss03";
  }
  input:focus, textarea:focus, button:focus { outline: none; }
`;
document.head.appendChild(globalStyle);

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<React.StrictMode><App /></React.StrictMode>);
