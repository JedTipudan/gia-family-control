import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const { data } = await authApi.login(email, password);
      login(data);
      navigate('/dashboard');
    } catch {
      setError('Invalid email or password');
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.logoContainer}>
          <img src="/logo.jpg" alt="Gia" style={styles.logo} />
        </div>
        <h1 style={styles.title}>Gia Family Control</h1>
        <p style={styles.subtitle}>Secure parental monitoring</p>
        <form onSubmit={handleSubmit} style={styles.form}>
          <input style={styles.input} type="email" placeholder="Email"
            value={email} onChange={e => setEmail(e.target.value)} required />
          <input style={styles.input} type="password" placeholder="Password"
            value={password} onChange={e => setPassword(e.target.value)} required />
          {error && <div style={styles.error}>{error}</div>}
          <button style={styles.btn} type="submit">Sign in</button>
        </form>
      </div>
    </div>
  );
}

const styles = {
  container: { 
    display: 'flex', 
    justifyContent: 'center', 
    alignItems: 'center',
    minHeight: '100vh', 
    background: 'var(--bg-primary)' 
  },
  card: { 
    background: 'var(--bg-elevated)', 
    padding: '48px', 
    borderRadius: 'var(--radius-xl)', 
    border: '1px solid var(--border-primary)',
    boxShadow: 'var(--shadow-lg)',
    width: '400px',
    textAlign: 'center' 
  },
  logoContainer: {
    display: 'flex',
    justifyContent: 'center',
    marginBottom: 'var(--space-6)'
  },
  logo: { 
    width: 64, 
    height: 64, 
    borderRadius: 'var(--radius-lg)',
    border: '1px solid var(--border-secondary)'
  },
  title: { 
    marginBottom: 'var(--space-2)', 
    color: 'var(--text-primary)',
    fontSize: 28,
    fontWeight: 600,
    letterSpacing: 'var(--letter-spacing-tight)'
  },
  subtitle: {
    color: 'var(--text-secondary)',
    fontSize: 14,
    marginBottom: 'var(--space-8)',
    letterSpacing: 'var(--letter-spacing-normal)'
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: 'var(--space-3)'
  },
  input: { 
    width: '100%', 
    padding: '12px 16px', 
    border: '1px solid var(--border-primary)',
    borderRadius: 'var(--radius-md)', 
    fontSize: 14, 
    background: 'var(--bg-secondary)',
    color: 'var(--text-primary)'
  },
  btn: { 
    width: '100%', 
    padding: '12px', 
    background: 'var(--accent-primary)', 
    color: 'var(--text-primary)',
    border: 'none', 
    borderRadius: 'var(--radius-md)', 
    fontSize: 14,
    fontWeight: 500,
    marginTop: 'var(--space-2)'
  },
  error: { 
    color: 'var(--danger)', 
    background: 'var(--danger-subtle)',
    padding: 'var(--space-3)',
    borderRadius: 'var(--radius-sm)',
    fontSize: 13
  },
};
