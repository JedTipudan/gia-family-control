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
        <img src="/logo.jpg" alt="Gia" style={styles.logo} />
        <h2 style={styles.title}>Gia Family Control</h2>
        <form onSubmit={handleSubmit}>
          <input style={styles.input} type="email" placeholder="Email"
            value={email} onChange={e => setEmail(e.target.value)} required />
          <input style={styles.input} type="password" placeholder="Password"
            value={password} onChange={e => setPassword(e.target.value)} required />
          {error && <p style={styles.error}>{error}</p>}
          <button style={styles.btn} type="submit">Login</button>
        </form>
      </div>
    </div>
  );
}

const styles = {
  container: { display: 'flex', justifyContent: 'center', alignItems: 'center',
    minHeight: '100vh', background: '#f0f2f5' },
  card: { background: '#fff', padding: 40, borderRadius: 12, boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
    width: 360, textAlign: 'center' },
  logo: { width: 80, height: 80, borderRadius: '50%', marginBottom: 16 },
  title: { marginBottom: 24, color: '#1a1a2e' },
  input: { width: '100%', padding: '12px 16px', marginBottom: 16, border: '1px solid #ddd',
    borderRadius: 8, fontSize: 14, boxSizing: 'border-box' },
  btn: { width: '100%', padding: '12px', background: '#4f46e5', color: '#fff',
    border: 'none', borderRadius: 8, fontSize: 16, cursor: 'pointer' },
  error: { color: '#ef4444', marginBottom: 12 },
};
