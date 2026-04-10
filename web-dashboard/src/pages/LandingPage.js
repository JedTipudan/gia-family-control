import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';

const GITHUB_RELEASE = 'https://github.com/JedTipudan/gia-family-control/releases/latest';
const GCASH_NUMBER   = '0975-591-8109'; // Jed Tipudan

const STEPS = [
  {
    num: '1', title: 'Download the Apps',
    desc: 'Download the Parent APK on your phone and the Child APK on your child\'s phone.',
    icon: '📥',
  },
  {
    num: '2', title: 'Install Both APKs',
    desc: 'Go to Settings → Security → Install unknown apps → allow it. Then open and install each APK.',
    icon: '📱',
  },
  {
    num: '3', title: 'Create Parent Account',
    desc: 'Open the Parent app or this website → Create Account → register as a parent.',
    icon: '👤',
  },
  {
    num: '4', title: 'Setup Child Device',
    desc: 'On the child phone: open Child app → register child account → enter the pair code from parent app.',
    icon: '🔗',
  },
  {
    num: '5', title: 'Grant Permissions',
    desc: 'On child phone: tap Start Monitoring → grant Usage Access → enable Accessibility Service → allow Display Over Apps.',
    icon: '✅',
  },
  {
    num: '6', title: 'Set as Home Launcher',
    desc: 'When prompted, set Gia Family Control as the default home app on the child device.',
    icon: '🏠',
  },
];

const FEATURES = [
  { icon: '📍', title: 'Live Location', desc: 'Track your child\'s real-time GPS location on a map.' },
  { icon: '🔒', title: 'Remote Lock', desc: 'Lock or unlock the child\'s device instantly from anywhere.' },
  { icon: '📱', title: 'App Control', desc: 'Block or hide any app including YouTube, games, and social media.' },
  { icon: '🔕', title: 'Block Notifications', desc: 'Prevent child from accessing the notification panel and quick settings.' },
  { icon: '⏱', title: 'Temp Access', desc: 'Grant temporary access for a set time — device locks automatically when time is up.' },
  { icon: '🆘', title: 'SOS Alert', desc: 'Child can send an emergency SOS alert with their location to the parent.' },
];

export default function LandingPage() {
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);

  const copyGcash = () => {
    navigator.clipboard.writeText(GCASH_NUMBER);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div style={s.page}>
      {/* NAV */}
      <nav style={s.nav}>
        <div style={s.navBrand}>
          <img src="/logo.jpg" alt="Gia" style={s.navLogo} />
          <span style={s.navName}>Gia Family Control</span>
        </div>
        <div style={s.navLinks}>
          <a href="#features" style={s.navLink}>Features</a>
          <a href="#download" style={s.navLink}>Download</a>
          <a href="#setup" style={s.navLink}>Setup Guide</a>
          <a href="#donate" style={s.navLink}>Donate</a>
          <button style={s.navBtn} onClick={() => navigate('/login')}>Parent Login</button>
        </div>
      </nav>

      {/* HERO */}
      <section style={s.hero}>
        <div style={s.heroContent}>
          <div style={s.heroBadge}>🛡️ Free Parental Control App</div>
          <h1 style={s.heroTitle}>Keep Your Child Safe<br />in the Digital World</h1>
          <p style={s.heroSub}>
            Gia Family Control lets parents monitor location, block apps, lock devices,
            and control screen time — all from your phone or browser.
          </p>
          <div style={s.heroBtns}>
            <a href="#download" style={s.heroBtnPrimary}>📥 Download Free</a>
            <button style={s.heroBtnSecondary} onClick={() => navigate('/login')}>Open Dashboard →</button>
          </div>
        </div>
        <div style={s.heroImg}>
          <img src="/logo.jpg" alt="Gia" style={s.heroLogo} />
        </div>
      </section>

      {/* FEATURES */}
      <section id="features" style={s.section}>
        <h2 style={s.sectionTitle}>Everything You Need</h2>
        <p style={s.sectionSub}>Powerful parental controls designed to be simple for parents and effective against tech-savvy kids.</p>
        <div style={s.featGrid}>
          {FEATURES.map(f => (
            <div key={f.title} style={s.featCard}>
              <span style={s.featIcon}>{f.icon}</span>
              <h3 style={s.featTitle}>{f.title}</h3>
              <p style={s.featDesc}>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* DOWNLOAD */}
      <section id="download" style={{ ...s.section, background: 'var(--bg-secondary)' }}>
        <h2 style={s.sectionTitle}>Download</h2>
        <p style={s.sectionSub}>Two apps — one for the parent, one for the child. Both are free.</p>
        <div style={s.dlGrid}>
          <div style={s.dlCard}>
            <span style={s.dlIcon}>👨‍👩‍👧</span>
            <h3 style={s.dlTitle}>Parent App</h3>
            <p style={s.dlDesc}>Install on your phone. Monitor, lock, and control your child's device.</p>
            <a href={GITHUB_RELEASE} target="_blank" rel="noreferrer" style={s.dlBtn}>
              📥 Download Parent APK
            </a>
          </div>
          <div style={s.dlCard}>
            <span style={s.dlIcon}>👦</span>
            <h3 style={s.dlTitle}>Child App</h3>
            <p style={s.dlDesc}>Install on your child's phone. Works silently in the background.</p>
            <a href={GITHUB_RELEASE} target="_blank" rel="noreferrer" style={{ ...s.dlBtn, background: 'rgba(52,211,153,0.15)', color: '#34d399', border: '1px solid rgba(52,211,153,0.3)' }}>
              📥 Download Child APK
            </a>
          </div>
        </div>
        <p style={s.dlNote}>
          ⚠️ Both APKs are on the same GitHub release page. Download the correct one for each device.
        </p>
      </section>

      {/* SETUP GUIDE */}
      <section id="setup" style={s.section}>
        <h2 style={s.sectionTitle}>Setup Guide</h2>
        <p style={s.sectionSub}>Follow these steps to get started in under 10 minutes.</p>
        <div style={s.stepsGrid}>
          {STEPS.map(step => (
            <div key={step.num} style={s.stepCard}>
              <div style={s.stepNum}>{step.icon}</div>
              <div>
                <div style={s.stepTitle}>Step {step.num} — {step.title}</div>
                <div style={s.stepDesc}>{step.desc}</div>
              </div>
            </div>
          ))}
        </div>

        {/* Permissions needed */}
        <div style={s.permBox}>
          <h3 style={s.permTitle}>📋 Permissions Required on Child Device</h3>
          <div style={s.permGrid}>
            {[
              ['Usage Access', 'Required for app monitoring and blocking'],
              ['Accessibility Service', 'Required for remote lock and notification blocking'],
              ['Display Over Other Apps', 'Required for blocking notification panel'],
              ['Device Admin', 'Required to prevent uninstallation'],
              ['Location', 'Required for live GPS tracking'],
            ].map(([name, desc]) => (
              <div key={name} style={s.permItem}>
                <span style={s.permCheck}>✅</span>
                <div>
                  <div style={s.permName}>{name}</div>
                  <div style={s.permDesc}>{desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* DONATE */}
      <section id="donate" style={{ ...s.section, background: 'var(--bg-secondary)' }}>
        <h2 style={s.sectionTitle}>Support This Project ❤️</h2>
        <p style={s.sectionSub}>
          Gia Family Control is completely free. If it helped keep your child safe,
          consider buying me a coffee to keep the servers running!
        </p>
        <div style={s.donateGrid}>
          <div style={{ ...s.donateCard, maxWidth: 320, margin: '0 auto' }}>
            <span style={s.donateIcon}>💙</span>
            <h3 style={s.donateTitle}>GCash</h3>
            <p style={s.donateName}>Jed Tipudan</p>
            <p style={s.donateNum}>{GCASH_NUMBER}</p>
            <button style={s.copyBtn} onClick={copyGcash}>
              {copied ? '✅ Copied!' : '📋 Copy Number'}
            </button>
          </div>
        </div>
        <p style={s.donateNote}>Your donation helps pay for server costs and future development. Thank you! 🙏</p>
      </section>

      {/* FOOTER */}
      <footer style={s.footer}>
        <div style={s.footerContent}>
          <div style={s.footerBrand}>
            <img src="/logo.jpg" alt="Gia" style={s.footerLogo} />
            <span style={s.footerName}>Gia Family Control</span>
          </div>
          <p style={s.footerText}>Free parental control app for Android. Keep your family safe.</p>
          <div style={s.footerLinks}>
            <a href="#features" style={s.footerLink}>Features</a>
            <a href="#download" style={s.footerLink}>Download</a>
            <a href="#setup" style={s.footerLink}>Setup</a>
            <a href="#donate" style={s.footerLink}>Donate</a>
            <button style={{ ...s.footerLink, background: 'none', border: 'none', cursor: 'pointer' }} onClick={() => navigate('/login')}>Login</button>
          </div>
        </div>
      </footer>
    </div>
  );
}

const s = {
  page:       { minHeight: '100vh', background: 'var(--bg-primary)', color: 'var(--text-primary)', fontFamily: 'system-ui, sans-serif' },
  nav:        { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '16px 40px', borderBottom: '1px solid var(--border-primary)', background: 'var(--bg-secondary)', position: 'sticky', top: 0, zIndex: 100 },
  navBrand:   { display: 'flex', alignItems: 'center', gap: 10 },
  navLogo:    { width: 32, height: 32, borderRadius: 8 },
  navName:    { fontSize: 16, fontWeight: 700, color: 'var(--text-primary)' },
  navLinks:   { display: 'flex', alignItems: 'center', gap: 24 },
  navLink:    { fontSize: 14, color: 'var(--text-secondary)', textDecoration: 'none', fontWeight: 500 },
  navBtn:     { padding: '8px 18px', background: 'var(--accent-primary)', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer' },
  hero:       { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '80px 40px', maxWidth: 1100, margin: '0 auto', gap: 40 },
  heroContent:{ flex: 1 },
  heroBadge:  { display: 'inline-block', padding: '6px 14px', background: 'rgba(129,140,248,0.12)', border: '1px solid rgba(129,140,248,0.3)', borderRadius: 980, fontSize: 13, color: '#818cf8', marginBottom: 20 },
  heroTitle:  { fontSize: 48, fontWeight: 800, lineHeight: 1.15, margin: '0 0 20px', color: 'var(--text-primary)', letterSpacing: '-1px' },
  heroSub:    { fontSize: 18, color: 'var(--text-secondary)', lineHeight: 1.6, margin: '0 0 32px', maxWidth: 500 },
  heroBtns:   { display: 'flex', gap: 12, flexWrap: 'wrap' },
  heroBtnPrimary:   { padding: '14px 28px', background: 'var(--accent-primary)', color: '#fff', borderRadius: 10, fontSize: 16, fontWeight: 700, textDecoration: 'none', display: 'inline-block' },
  heroBtnSecondary: { padding: '14px 28px', background: 'var(--bg-tertiary)', color: 'var(--text-primary)', border: '1px solid var(--border-primary)', borderRadius: 10, fontSize: 16, fontWeight: 600, cursor: 'pointer' },
  heroImg:    { flexShrink: 0 },
  heroLogo:   { width: 200, height: 200, borderRadius: 40, boxShadow: '0 20px 60px rgba(0,0,0,0.3)' },
  section:    { padding: '80px 40px', maxWidth: 1100, margin: '0 auto' },
  sectionTitle: { fontSize: 36, fontWeight: 800, textAlign: 'center', margin: '0 0 12px', color: 'var(--text-primary)', letterSpacing: '-0.5px' },
  sectionSub: { fontSize: 16, color: 'var(--text-secondary)', textAlign: 'center', margin: '0 0 48px', maxWidth: 600, marginLeft: 'auto', marginRight: 'auto' },
  featGrid:   { display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 20 },
  featCard:   { padding: 24, background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 14 },
  featIcon:   { fontSize: 32, display: 'block', marginBottom: 12 },
  featTitle:  { fontSize: 16, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 8 },
  featDesc:   { fontSize: 14, color: 'var(--text-secondary)', lineHeight: 1.5 },
  dlGrid:     { display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 24, maxWidth: 700, margin: '0 auto 20px' },
  dlCard:     { padding: 32, background: 'var(--bg-primary)', border: '1px solid var(--border-primary)', borderRadius: 16, textAlign: 'center' },
  dlIcon:     { fontSize: 48, display: 'block', marginBottom: 16 },
  dlTitle:    { fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 8 },
  dlDesc:     { fontSize: 14, color: 'var(--text-secondary)', marginBottom: 24, lineHeight: 1.5 },
  dlBtn:      { display: 'inline-block', padding: '12px 24px', background: 'rgba(129,140,248,0.15)', color: '#818cf8', border: '1px solid rgba(129,140,248,0.3)', borderRadius: 10, fontSize: 15, fontWeight: 700, textDecoration: 'none' },
  dlNote:     { textAlign: 'center', fontSize: 13, color: 'var(--text-tertiary)', marginTop: 16 },
  stepsGrid:  { display: 'flex', flexDirection: 'column', gap: 16, maxWidth: 700, margin: '0 auto 40px' },
  stepCard:   { display: 'flex', alignItems: 'flex-start', gap: 16, padding: 20, background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 12 },
  stepNum:    { fontSize: 28, flexShrink: 0 },
  stepTitle:  { fontSize: 15, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 4 },
  stepDesc:   { fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 },
  permBox:    { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 14, padding: 28, maxWidth: 700, margin: '0 auto' },
  permTitle:  { fontSize: 16, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 16 },
  permGrid:   { display: 'flex', flexDirection: 'column', gap: 12 },
  permItem:   { display: 'flex', alignItems: 'flex-start', gap: 12 },
  permCheck:  { fontSize: 16, flexShrink: 0 },
  permName:   { fontSize: 14, fontWeight: 600, color: 'var(--text-primary)', marginBottom: 2 },
  permDesc:   { fontSize: 12, color: 'var(--text-tertiary)' },
  donateGrid: { display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 24, maxWidth: 600, margin: '0 auto 20px' },
  donateCard: { padding: 32, background: 'var(--bg-primary)', border: '1px solid var(--border-primary)', borderRadius: 16, textAlign: 'center' },
  donateIcon: { fontSize: 48, display: 'block', marginBottom: 12 },
  donateTitle:{ fontSize: 20, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 8 },
  donateNum:  { fontSize: 18, fontWeight: 700, color: 'var(--accent-primary)', marginBottom: 20, fontFamily: 'ui-monospace, monospace' },
  donateName: { fontSize: 14, color: 'var(--text-secondary)', marginBottom: 4 },
  copyBtn:    { padding: '10px 20px', background: 'rgba(129,140,248,0.15)', color: '#818cf8', border: '1px solid rgba(129,140,248,0.3)', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer', width: '100%' },
  paypalBtn:  { display: 'block', padding: '10px 20px', background: 'rgba(251,191,36,0.15)', color: '#fbbf24', border: '1px solid rgba(251,191,36,0.3)', borderRadius: 8, fontSize: 14, fontWeight: 600, textDecoration: 'none', width: '100%', boxSizing: 'border-box' },
  donateNote: { textAlign: 'center', fontSize: 13, color: 'var(--text-tertiary)' },
  footer:     { borderTop: '1px solid var(--border-primary)', padding: '40px', background: 'var(--bg-secondary)' },
  footerContent: { maxWidth: 1100, margin: '0 auto', textAlign: 'center' },
  footerBrand:{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10, marginBottom: 12 },
  footerLogo: { width: 28, height: 28, borderRadius: 6 },
  footerName: { fontSize: 16, fontWeight: 700, color: 'var(--text-primary)' },
  footerText: { fontSize: 13, color: 'var(--text-tertiary)', marginBottom: 16 },
  footerLinks:{ display: 'flex', justifyContent: 'center', gap: 24 },
  footerLink: { fontSize: 13, color: 'var(--text-secondary)', textDecoration: 'none' },
};
