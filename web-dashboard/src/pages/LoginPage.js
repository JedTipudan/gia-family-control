import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();

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
        <div style={s.logoWrap}>
          <img src="/logo.jpg" alt="Gia" style={s.logo} />
        </div>
        <h1 style={s.title}>Gia Family Control</h1>
        <p style={s.sub}>Sign in to your parent dashboard</p>
        <form onSubmit={handleSubmit} style={s.form}>
          <div style={s.field}>
            <label style={s.label}>Email</label>
            <input style={s.input} type="email" placeholder="you@example.com"
              value={email} onChange={e => setEmail(e.target.value)} required />
          </div>
          <div style={s.field}>
            <label style={s.label}>Password</label>
            <input style={s.input} type="password" placeholder="••••••••"
              value={password} onChange={e => setPassword(e.target.value)} required />
          </div>
          {error && (
            <div style={s.errorBox}>
              <span style={s.errorDot} />
              {error}
            </div>
          )}
          <button style={{ ...s.btn, opacity: loading ? 0.6 : 1 }} type="submit" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  );
}

const s = {
  page: {
    display: 'flex', justifyContent: 'center', alignItems: 'center',
    minHeight: '100vh', background: 'var(--bg-primary)',
    fontFamily: "'Inter', -apple-system, system-ui, sans-serif",
  },
  card: {
    width: 380, padding: '40px 36px',
    background: 'var(--bg-elevated)',
    border: '1px solid var(--border-subtle)',
    borderRadius: 12,
    boxShadow: 'var(--shadow) 0px 2px 4px',
  },
  logoWrap: { display: 'flex', justifyContent: 'center', marginBottom: 20 },
  logo: { width: 56, height: 56, borderRadius: '50%', border: '1px solid var(--border-subtle)' },
  title: {
    margin: '0 0 6px', textAlign: 'center',
    fontSize: 22, fontWeight: 510, letterSpacing: '-0.3px',
    color: 'var(--text-primary)',
  },
  sub: {
    margin: '0 0 28px', textAlign: 'center',
    fontSize: 14, fontWeight: 400, color: 'var(--text-tertiary)',
  },
  form: { display: 'flex', flexDirection: 'column', gap: 16 },
  field: { display: 'flex', flexDirection: 'column', gap: 6 },
  label: { fontSize: 13, fontWeight: 510, color: 'var(--text-secondary)', letterSpacing: '-0.13px' },
  input: {
    padding: '10px 14px',
    background: 'var(--bg-secondary)',
    border: '1px solid var(--border-subtle)',
    borderRadius: 6, fontSize: 14, color: 'var(--text-primary)',
    outline: 'none', boxSizing: 'border-box', width: '100%',
    fontFamily: 'inherit',
    transition: 'border-color 0.15s',
  },
  errorBox: {
    display: 'flex', alignItems: 'center', gap: 8,
    padding: '10px 14px',
    background: 'var(--danger-bg)',
    border: '1px solid var(--danger-border)',
    borderRadius: 6, fontSize: 13, color: 'var(--danger)',
  },
  errorDot: {
    width: 6, height: 6, borderRadius: '50%',
    background: 'var(--danger)', flexShrink: 0,
  },
  btn: {
    marginTop: 4, padding: '10px 16px',
    background: 'var(--accent-primary)', color: '#fff',
    border: 'none', borderRadius: 6,
    fontSize: 14, fontWeight: 510, cursor: 'pointer',
    fontFamily: 'inherit', letterSpacing: '-0.13px',
    transition: 'background 0.15s',
  },
};
