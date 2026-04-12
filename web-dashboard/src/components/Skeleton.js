import React from 'react';

export default function Skeleton({ width = '100%', height = 16, radius = 6, style = {} }) {
  return (
    <div style={{
      width, height,
      borderRadius: radius,
      background: 'var(--bg-tertiary)',
      backgroundImage: 'linear-gradient(90deg, var(--bg-tertiary) 25%, var(--border-primary) 50%, var(--bg-tertiary) 75%)',
      backgroundSize: '200% 100%',
      animation: 'shimmer 1.4s infinite',
      flexShrink: 0,
      ...style,
    }} />
  );
}

// Inject keyframes once
if (typeof document !== 'undefined' && !document.getElementById('skeleton-style')) {
  const el = document.createElement('style');
  el.id = 'skeleton-style';
  el.textContent = `@keyframes shimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} } @keyframes fadeIn { from{opacity:0;transform:translateY(8px)} to{opacity:1;transform:translateY(0)} }`;
  document.head.appendChild(el);
}
