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
  },
  card: {
    width: 400, padding: '48px',
    background: 'var(--bg-secondary)',
    borderRadius: 18,
    boxShadow: 'var(--shadow) 3px 5px 30px 0px',
  },
  logoWrap: { display: 'flex', justifyContent: 'center', marginBottom: 24 },
  logo: { width: 80, height: 80, borderRadius: 18 },
  title: {
    margin: '0 0 8px', textAlign: 'center',
    fontSize: 32, fontWeight: 600, letterSpacing: '-0.5px',
    color: 'var(--text-primary)', lineHeight: 1.1,
  },
  sub: {
    margin: '0 0 32px', textAlign: 'center',
    fontSize: 17, fontWeight: 400, color: 'var(--text-tertiary)',
    lineHeight: 1.47,
  },
  form: { display: 'flex', flexDirection: 'column', gap: 20 },
  field: { display: 'flex', flexDirection: 'column', gap: 8 },
  label: { fontSize: 14, fontWeight: 600, color: 'var(--text-secondary)' },
  input: {
    padding: '12px 16px',
    background: 'var(--bg-primary)',
    border: '1px solid var(--border-primary)',
    borderRadius: 8, fontSize: 17, color: 'var(--text-primary)',
    boxSizing: 'border-box', width: '100%',
    transition: 'border-color 0.2s, box-shadow 0.2s',
  },
  errorBox: {
    display: 'flex', alignItems: 'center', gap: 10,
    padding: '12px 16px',
    background: 'var(--danger-bg)',
    border: '1px solid var(--danger-border)',
    borderRadius: 8, fontSize: 15, color: 'var(--danger)',
  },
  errorDot: {
    width: 8, height: 8, borderRadius: '50%',
    background: 'var(--danger)', flexShrink: 0,
  },
  btn: {
    marginTop: 8, padding: '12px 16px',
    background: 'var(--accent-primary)', color: '#fff',
    border: 'none', borderRadius: 980,
    fontSize: 17, fontWeight: 600, cursor: 'pointer',
    transition: 'background 0.2s, transform 0.1s',
  },
};
