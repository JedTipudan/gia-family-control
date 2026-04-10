import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

const GITHUB_RELEASE  = 'https://github.com/JedTipudan/gia-family-control/releases/latest';
const GITHUB_API      = 'https://api.github.com/repos/JedTipudan/gia-family-control/releases/latest';
const DOWNLOAD_PARENT = 'https://github.com/JedTipudan/gia-family-control/releases/latest/download/GiaParentControl.apk';
const DOWNLOAD_CHILD  = 'https://github.com/JedTipudan/gia-family-control/releases/latest/download/GiaFamilyControl-Child.apk';
const GCASH_NUMBER    = '0975-591-8109';

const STEPS = [
  { num:'1', icon:'📥', title:'Download the Apps',       desc:'Download the Parent APK on your phone and the Child APK on your child\'s phone.' },
  { num:'2', icon:'📱', title:'Install Both APKs',        desc:'Go to Settings → Security → Install unknown apps → allow it. Then open and install each APK.' },
  { num:'3', icon:'👤', title:'Create Parent Account',    desc:'Open the Parent app or this website → Create Account → register as a parent.' },
  { num:'4', icon:'🔗', title:'Setup Child Device',       desc:'On the child phone: open Child app → register child account → enter the pair code from parent app.' },
  { num:'5', icon:'✅', title:'Grant Permissions',        desc:'On child phone: tap Start Monitoring → grant Usage Access → enable Accessibility Service → allow Display Over Apps.' },
  { num:'6', icon:'🏠', title:'Set as Home Launcher',     desc:'When prompted, set Gia Family Control as the default home app on the child device.' },
];

const FEATURES = [
  { icon:'📍', title:'Live Location',         desc:'Track your child\'s real-time GPS location on a map.' },
  { icon:'🔒', title:'Remote Lock',           desc:'Lock or unlock the child\'s device instantly from anywhere.' },
  { icon:'📱', title:'App Control',           desc:'Block or hide any app including YouTube, games, and social media.' },
  { icon:'🔕', title:'Block Notifications',   desc:'Prevent child from accessing the notification panel and quick settings.' },
  { icon:'⏱',  title:'Temp Access',           desc:'Grant temporary access for a set time — device locks automatically when time is up.' },
  { icon:'🆘', title:'SOS Alert',             desc:'Child can send an emergency SOS alert with their location to the parent.' },
];

const PERMS = [
  ['Usage Access',           'Required for app monitoring and blocking'],
  ['Accessibility Service',  'Required for remote lock and notification blocking'],
  ['Display Over Other Apps','Required for blocking notification panel'],
  ['Device Admin',           'Required to prevent uninstallation'],
  ['Location',               'Required for live GPS tracking'],
];

function useIsMobile() {
  const [mobile, setMobile] = useState(window.innerWidth <= 768);
  useEffect(() => {
    const fn = () => setMobile(window.innerWidth <= 768);
    window.addEventListener('resize', fn);
    return () => window.removeEventListener('resize', fn);
  }, []);
  return mobile;
}

export default function LandingPage() {
  const navigate = useNavigate();
  const isMobile = useIsMobile();
  const [copied, setCopied]             = useState(false);
  const [latestVersion, setLatestVersion] = useState(null);
  const [releaseNotes, setReleaseNotes]   = useState('');
  const [menuOpen, setMenuOpen]           = useState(false);

  useEffect(() => {
    fetch(GITHUB_API)
      .then(r => r.json())
      .then(d => { setLatestVersion(d.tag_name); setReleaseNotes(d.body || ''); })
      .catch(() => {});
  }, []);

  const copyGcash = () => {
    navigator.clipboard.writeText(GCASH_NUMBER);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const navLinks = (
    <>
      <a href="#features" style={r.navLink} onClick={() => setMenuOpen(false)}>Features</a>
      <a href="#download" style={r.navLink} onClick={() => setMenuOpen(false)}>Download</a>
      <a href="#setup"    style={r.navLink} onClick={() => setMenuOpen(false)}>Setup Guide</a>
      <a href="#donate"   style={r.navLink} onClick={() => setMenuOpen(false)}>Donate</a>
      <button style={r.navBtn} onClick={() => { setMenuOpen(false); navigate('/login'); }}>Parent Login</button>
    </>
  );

  return (
    <div style={r.page}>

      {/* NAV */}
      <nav style={r.nav}>
        <div style={r.navBrand}>
          <img src="/logo.jpg" alt="Gia" style={r.navLogo} />
          <span style={r.navName}>Gia Family Control</span>
        </div>
        {isMobile ? (
          <button style={r.hamburger} onClick={() => setMenuOpen(v => !v)}>☰</button>
        ) : (
          <div style={r.navLinks}>{navLinks}</div>
        )}
      </nav>

      {/* MOBILE MENU */}
      {isMobile && menuOpen && (
        <div style={r.mobileMenu}>{navLinks}</div>
      )}

      {/* HERO */}
      <section style={{ ...r.hero, flexDirection: isMobile ? 'column' : 'row', padding: isMobile ? '48px 20px' : '80px 40px', textAlign: isMobile ? 'center' : 'left' }}>
        <div style={r.heroContent}>
          <div style={r.heroBadge}>🛡️ Free Parental Control App</div>
          <h1 style={{ ...r.heroTitle, fontSize: isMobile ? 32 : 48 }}>
            Keep Your Child Safe<br />in the Digital World
          </h1>
          <p style={{ ...r.heroSub, fontSize: isMobile ? 15 : 18 }}>
            Gia Family Control lets parents monitor location, block apps, lock devices,
            and control screen time — all from your phone or browser.
          </p>
          <div style={{ ...r.heroBtns, justifyContent: isMobile ? 'center' : 'flex-start' }}>
            <a href="#download" style={r.heroBtnPrimary}>📥 Download Free</a>
            <button style={r.heroBtnSecondary} onClick={() => navigate('/login')}>Open Dashboard →</button>
          </div>
        </div>
        {!isMobile && (
          <div style={r.heroImg}>
            <img src="/logo.jpg" alt="Gia" style={r.heroLogo} />
          </div>
        )}
      </section>

      {/* FEATURES */}
      <section id="features" style={{ ...r.section, padding: isMobile ? '48px 20px' : '80px 40px' }}>
        <h2 style={r.sectionTitle}>Everything You Need</h2>
        <p style={r.sectionSub}>Powerful parental controls designed to be simple for parents and effective against tech-savvy kids.</p>
        <div style={{ ...r.featGrid, gridTemplateColumns: isMobile ? '1fr 1fr' : 'repeat(3,1fr)' }}>
          {FEATURES.map(f => (
            <div key={f.title} style={r.featCard}>
              <span style={r.featIcon}>{f.icon}</span>
              <h3 style={r.featTitle}>{f.title}</h3>
              <p style={r.featDesc}>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* DOWNLOAD */}
      <section id="download" style={{ ...r.section, background:'var(--bg-secondary)', padding: isMobile ? '48px 20px' : '80px 40px' }}>
        <h2 style={r.sectionTitle}>Download</h2>
        <p style={r.sectionSub}>Two apps — one for the parent, one for the child. Both are free.</p>
        {latestVersion && (
          <div style={r.versionBadge}>
            🆕 Latest: <strong>{latestVersion}</strong>
            {releaseNotes && !isMobile && <span style={r.releaseNotes}> — {releaseNotes.split('\n')[0]}</span>}
          </div>
        )}
        <div style={{ ...r.dlGrid, gridTemplateColumns: isMobile ? '1fr' : 'repeat(2,1fr)', maxWidth: isMobile ? '100%' : 700 }}>
          <div style={r.dlCard}>
            <span style={r.dlIcon}>👨‍👩‍👧</span>
            <h3 style={r.dlTitle}>Parent App</h3>
            <p style={r.dlDesc}>Install on your phone. Monitor, lock, and control your child's device.</p>
            <a href={DOWNLOAD_PARENT} style={r.dlBtn}>📥 Download Parent APK</a>
          </div>
          <div style={r.dlCard}>
            <span style={r.dlIcon}>👦</span>
            <h3 style={r.dlTitle}>Child App</h3>
            <p style={r.dlDesc}>Install on your child's phone. Works openly with child's knowledge.</p>
            <a href={DOWNLOAD_CHILD} style={{ ...r.dlBtn, background:'rgba(52,211,153,0.15)', color:'#34d399', border:'1px solid rgba(52,211,153,0.3)' }}>
              📥 Download Child APK
            </a>
          </div>
        </div>
        <p style={r.dlNote}>⚠️ If download doesn't start automatically, tap and hold the button → Open in browser.</p>
      </section>

      {/* SETUP GUIDE */}
      <section id="setup" style={{ ...r.section, padding: isMobile ? '48px 20px' : '80px 40px' }}>
        <h2 style={r.sectionTitle}>Setup Guide</h2>
        <p style={r.sectionSub}>Follow these steps to get started in under 10 minutes.</p>
        <div style={r.stepsGrid}>
          {STEPS.map(step => (
            <div key={step.num} style={r.stepCard}>
              <div style={r.stepNum}>{step.icon}</div>
              <div>
                <div style={r.stepTitle}>Step {step.num} — {step.title}</div>
                <div style={r.stepDesc}>{step.desc}</div>
              </div>
            </div>
          ))}
        </div>
        <div style={{ ...r.permBox, padding: isMobile ? 20 : 28 }}>
          <h3 style={r.permTitle}>📋 Permissions Required on Child Device</h3>
          <div style={r.permGrid}>
            {PERMS.map(([name, desc]) => (
              <div key={name} style={r.permItem}>
                <span style={r.permCheck}>✅</span>
                <div>
                  <div style={r.permName}>{name}</div>
                  <div style={r.permDesc}>{desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* DONATE */}
      <section id="donate" style={{ ...r.section, background:'var(--bg-secondary)', padding: isMobile ? '48px 20px' : '80px 40px' }}>
        <h2 style={r.sectionTitle}>Support This Project ❤️</h2>
        <p style={r.sectionSub}>
          Gia Family Control is completely free. If it helped keep your child safe,
          consider buying me a coffee to keep the servers running!
        </p>
        <div style={{ maxWidth: 320, margin: '0 auto 20px' }}>
          <div style={r.donateCard}>
            <span style={r.donateIcon}>💙</span>
            <h3 style={r.donateTitle}>GCash</h3>
            <p style={r.donateName}>Jed Tipudan</p>
            <p style={r.donateNum}>{GCASH_NUMBER}</p>
            <button style={r.copyBtn} onClick={copyGcash}>
              {copied ? '✅ Copied!' : '📋 Copy Number'}
            </button>
          </div>
        </div>
        <p style={r.donateNote}>Your donation helps pay for server costs and future development. Thank you! 🙏</p>
      </section>

      {/* FOOTER */}
      <footer style={r.footer}>
        <div style={r.footerContent}>
          <div style={r.footerBrand}>
            <img src="/logo.jpg" alt="Gia" style={r.footerLogo} />
            <span style={r.footerName}>Gia Family Control</span>
          </div>
          <p style={r.footerText}>Free parental control app for Android. Keep your family safe.</p>
          <div style={{ ...r.footerLinks, flexWrap:'wrap', gap: isMobile ? 12 : 24 }}>
            <a href="#features" style={r.footerLink}>Features</a>
            <a href="#download" style={r.footerLink}>Download</a>
            <a href="#setup"    style={r.footerLink}>Setup</a>
            <a href="#donate"   style={r.footerLink}>Donate</a>
            <button style={{ ...r.footerLink, background:'none', border:'none', cursor:'pointer' }} onClick={() => navigate('/login')}>Login</button>
          </div>
        </div>
      </footer>
    </div>
  );
}

const r = {
  page:        { minHeight:'100vh', background:'var(--bg-primary)', color:'var(--text-primary)', fontFamily:'system-ui,sans-serif' },
  nav:         { display:'flex', alignItems:'center', justifyContent:'space-between', padding:'14px 20px', borderBottom:'1px solid var(--border-primary)', background:'var(--bg-secondary)', position:'sticky', top:0, zIndex:100 },
  navBrand:    { display:'flex', alignItems:'center', gap:10 },
  navLogo:     { width:32, height:32, borderRadius:8 },
  navName:     { fontSize:15, fontWeight:700, color:'var(--text-primary)' },
  navLinks:    { display:'flex', alignItems:'center', gap:20 },
  navLink:     { fontSize:14, color:'var(--text-secondary)', textDecoration:'none', fontWeight:500 },
  navBtn:      { padding:'8px 16px', background:'var(--accent-primary)', color:'#fff', border:'none', borderRadius:8, fontSize:14, fontWeight:600, cursor:'pointer' },
  hamburger:   { background:'none', border:'none', fontSize:22, color:'var(--text-primary)', cursor:'pointer', padding:4 },
  mobileMenu:  { display:'flex', flexDirection:'column', gap:0, background:'var(--bg-secondary)', borderBottom:'1px solid var(--border-primary)', padding:'8px 20px 16px', zIndex:99 },
  hero:        { display:'flex', alignItems:'center', justifyContent:'space-between', maxWidth:1100, margin:'0 auto', gap:40 },
  heroContent: { flex:1 },
  heroBadge:   { display:'inline-block', padding:'6px 14px', background:'rgba(129,140,248,0.12)', border:'1px solid rgba(129,140,248,0.3)', borderRadius:980, fontSize:13, color:'#818cf8', marginBottom:16 },
  heroTitle:   { fontWeight:800, lineHeight:1.15, margin:'0 0 16px', color:'var(--text-primary)', letterSpacing:'-1px' },
  heroSub:     { color:'var(--text-secondary)', lineHeight:1.6, margin:'0 0 28px', maxWidth:500 },
  heroBtns:    { display:'flex', gap:12, flexWrap:'wrap' },
  heroBtnPrimary:   { padding:'13px 24px', background:'var(--accent-primary)', color:'#fff', borderRadius:10, fontSize:15, fontWeight:700, textDecoration:'none', display:'inline-block' },
  heroBtnSecondary: { padding:'13px 24px', background:'var(--bg-tertiary)', color:'var(--text-primary)', border:'1px solid var(--border-primary)', borderRadius:10, fontSize:15, fontWeight:600, cursor:'pointer' },
  heroImg:     { flexShrink:0 },
  heroLogo:    { width:180, height:180, borderRadius:36, boxShadow:'0 20px 60px rgba(0,0,0,0.3)' },
  section:     { maxWidth:1100, margin:'0 auto' },
  sectionTitle:{ fontSize:28, fontWeight:800, textAlign:'center', margin:'0 0 10px', color:'var(--text-primary)', letterSpacing:'-0.5px' },
  sectionSub:  { fontSize:15, color:'var(--text-secondary)', textAlign:'center', margin:'0 0 36px', maxWidth:560, marginLeft:'auto', marginRight:'auto', lineHeight:1.6 },
  featGrid:    { display:'grid', gap:16 },
  featCard:    { padding:20, background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:14 },
  featIcon:    { fontSize:28, display:'block', marginBottom:10 },
  featTitle:   { fontSize:15, fontWeight:700, color:'var(--text-primary)', marginBottom:6 },
  featDesc:    { fontSize:13, color:'var(--text-secondary)', lineHeight:1.5 },
  versionBadge:{ textAlign:'center', padding:'10px 20px', background:'rgba(52,211,153,0.1)', border:'1px solid rgba(52,211,153,0.3)', borderRadius:8, fontSize:14, color:'#34d399', marginBottom:24 },
  releaseNotes:{ color:'var(--text-tertiary)', fontSize:13 },
  dlGrid:      { display:'grid', gap:20, margin:'0 auto 20px' },
  dlCard:      { padding:28, background:'var(--bg-primary)', border:'1px solid var(--border-primary)', borderRadius:16, textAlign:'center' },
  dlIcon:      { fontSize:44, display:'block', marginBottom:14 },
  dlTitle:     { fontSize:18, fontWeight:700, color:'var(--text-primary)', marginBottom:8 },
  dlDesc:      { fontSize:13, color:'var(--text-secondary)', marginBottom:20, lineHeight:1.5 },
  dlBtn:       { display:'inline-block', padding:'12px 20px', background:'rgba(129,140,248,0.15)', color:'#818cf8', border:'1px solid rgba(129,140,248,0.3)', borderRadius:10, fontSize:14, fontWeight:700, textDecoration:'none', width:'100%', boxSizing:'border-box' },
  dlNote:      { textAlign:'center', fontSize:13, color:'var(--text-tertiary)', marginTop:16 },
  stepsGrid:   { display:'flex', flexDirection:'column', gap:14, maxWidth:700, margin:'0 auto 32px' },
  stepCard:    { display:'flex', alignItems:'flex-start', gap:14, padding:18, background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:12 },
  stepNum:     { fontSize:26, flexShrink:0 },
  stepTitle:   { fontSize:14, fontWeight:700, color:'var(--text-primary)', marginBottom:4 },
  stepDesc:    { fontSize:13, color:'var(--text-secondary)', lineHeight:1.5 },
  permBox:     { background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:14, maxWidth:700, margin:'0 auto' },
  permTitle:   { fontSize:15, fontWeight:700, color:'var(--text-primary)', marginBottom:14 },
  permGrid:    { display:'flex', flexDirection:'column', gap:12 },
  permItem:    { display:'flex', alignItems:'flex-start', gap:10 },
  permCheck:   { fontSize:15, flexShrink:0 },
  permName:    { fontSize:13, fontWeight:600, color:'var(--text-primary)', marginBottom:2 },
  permDesc:    { fontSize:12, color:'var(--text-tertiary)' },
  donateCard:  { padding:28, background:'var(--bg-primary)', border:'1px solid var(--border-primary)', borderRadius:16, textAlign:'center' },
  donateIcon:  { fontSize:44, display:'block', marginBottom:12 },
  donateTitle: { fontSize:18, fontWeight:700, color:'var(--text-primary)', marginBottom:6 },
  donateName:  { fontSize:13, color:'var(--text-secondary)', marginBottom:4 },
  donateNum:   { fontSize:18, fontWeight:700, color:'var(--accent-primary)', marginBottom:18, fontFamily:'ui-monospace,monospace' },
  copyBtn:     { padding:'10px 20px', background:'rgba(129,140,248,0.15)', color:'#818cf8', border:'1px solid rgba(129,140,248,0.3)', borderRadius:8, fontSize:14, fontWeight:600, cursor:'pointer', width:'100%' },
  donateNote:  { textAlign:'center', fontSize:13, color:'var(--text-tertiary)' },
  footer:      { borderTop:'1px solid var(--border-primary)', padding:'32px 20px', background:'var(--bg-secondary)' },
  footerContent:{ maxWidth:1100, margin:'0 auto', textAlign:'center' },
  footerBrand: { display:'flex', alignItems:'center', justifyContent:'center', gap:10, marginBottom:10 },
  footerLogo:  { width:26, height:26, borderRadius:6 },
  footerName:  { fontSize:15, fontWeight:700, color:'var(--text-primary)' },
  footerText:  { fontSize:13, color:'var(--text-tertiary)', marginBottom:14 },
  footerLinks: { display:'flex', justifyContent:'center' },
  footerLink:  { fontSize:13, color:'var(--text-secondary)', textDecoration:'none' },
};
