import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail]       = useState('');
  const [password, setPassword] = useState('');
  const [error, setError]       = useState('');
  const [loading, setLoading]   = useState(false);
  const { login } = useAuth();
  const navigate  = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const { data } = await authApi.login(email, password);
      login(data);
      navigate('/dashboard');
    } catch {
      setError('Invalid email or password');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={s.page}>
      <div style={s.card}>
        {/* Logo */}
        <div style={s.logoRow}>
          <div style={s.logoWrap}>
            <img src="/logo.jpg" alt="Gia" style={s.logo} />
          </div>
        </div>

        <h1 style={s.title}>Gia Family Control</h1>
        <p style={s.sub}>Admin &amp; Parent Control Panel</p>

        <form onSubmit={handleSubmit} style={s.form}>
          <div style={s.field}>
            <label style={s.label}>Email</label>
            <input
              style={s.input}
              type="email"
              placeholder="parent@example.com"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
            />
          </div>
          <div style={s.field}>
            <label style={s.label}>Password</label>
            <input
              style={s.input}
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
            />
          </div>

          {error && (
            <div style={s.errorBox}>
              <span style={s.errorDot} />
              {error}
            </div>
          )}

          <button
            style={{ ...s.btn, opacity: loading ? 0.6 : 1 }}
            type="submit"
            disabled={loading}
          >
            {loading ? 'Signing in…' : 'Sign in to Dashboard'}
          </button>
        </form>

        <p style={s.hint}>Parent accounts only. Child devices use the Android app.</p>
      </div>
    </div>
  );
}

const s = {
  page: {
    display: 'flex', justifyContent: 'center', alignItems: 'center',
    minHeight: '100vh', background: 'var(--bg-primary)',
  },
  card: {
    width: 400, padding: '40px 40px 32px',
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border-primary)',
    borderRadius: 16,
  },
  logoRow: { display: 'flex', justifyContent: 'center', marginBottom: 20 },
  logoWrap: { width: 56, height: 56, borderRadius: 14, overflow: 'hidden', border: '1px solid var(--border-primary)' },
  logo: { width: '100%', height: '100%', objectFit: 'cover' },
  title: { margin: '0 0 6px', textAlign: 'center', fontSize: 24, fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.5px' },
  sub: { margin: '0 0 28px', textAlign: 'center', fontSize: 14, color: 'var(--text-tertiary)' },
  form: { display: 'flex', flexDirection: 'column', gap: 16 },
  field: { display: 'flex', flexDirection: 'column', gap: 6 },
  label: { fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)' },
  input: {
    padding: '10px 14px',
    background: 'var(--bg-tertiary)',
    border: '1px solid var(--border-primary)',
    borderRadius: 8, fontSize: 14, color: 'var(--text-primary)',
    width: '100%', boxSizing: 'border-box',
  },
  errorBox: {
    display: 'flex', alignItems: 'center', gap: 8,
    padding: '10px 14px',
    background: 'rgba(239,68,68,0.08)',
    border: '1px solid rgba(239,68,68,0.2)',
    borderRadius: 8, fontSize: 14, color: '#f87171',
  },
  errorDot: { width: 7, height: 7, borderRadius: '50%', background: '#ef4444', flexShrink: 0 },
  btn: {
    marginTop: 4, padding: '11px 16px',
    background: 'var(--accent-primary)', color: '#fff',
    border: 'none', borderRadius: 8,
    fontSize: 14, fontWeight: 600, cursor: 'pointer',
    transition: 'opacity 0.15s',
  },
  hint: { marginTop: 20, textAlign: 'center', fontSize: 12, color: 'var(--text-tertiary)', lineHeight: 1.5 },
};
