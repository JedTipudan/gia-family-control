import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTheme } from '../context/ThemeContext';

const GITHUB_API      = 'https://api.github.com/repos/JedTipudan/gia-family-control/releases/latest';
const DOWNLOAD_PARENT = 'https://github.com/JedTipudan/gia-family-control/releases/latest/download/GiaParentControl.apk';
const DOWNLOAD_CHILD  = 'https://github.com/JedTipudan/gia-family-control/releases/latest/download/GiaFamilyControl-Child.apk';
const GCASH_NUMBER    = '0975-591-8109';
const FB_GROUP        = 'https://www.facebook.com/groups/2466290300469414';
const INSTALL_VIDEO   = 'https://www.youtube.com/watch?v=I-hzPOM08f4';

const SOCIAL = [
  { icon: '📘', label: 'Facebook',  href: 'https://www.facebook.com/i11181999',    color: '#1877f2' },
  { icon: '▶️',  label: 'YouTube',   href: 'https://www.youtube.com/@parokyanijed', color: '#ff0000' },
  { icon: '📸', label: 'Instagram', href: 'https://www.instagram.com/jedtipudan_', color: '#e1306c' },
];

const FEATURES = [
  { icon: '📍', title: 'Live Location',        desc: "Track your child's real-time GPS location on a map." },
  { icon: '🔒', title: 'Remote Lock',          desc: "Lock or unlock the child's device instantly from anywhere." },
  { icon: '📱', title: 'App Control',          desc: 'Block or hide any app including YouTube, games, and social media.' },
  { icon: '🔕', title: 'Block Notifications',  desc: 'Prevent child from accessing the notification panel and quick settings.' },
  { icon: '⏱',  title: 'Temp Access',          desc: 'Grant temporary access for a set time — device locks automatically when time is up.' },
  { icon: '🆘', title: 'SOS Alert',            desc: "Child can send an emergency SOS alert with their location to the parent." },
];

const STEPS = [
  { num:'1', icon:'📥', title:'Download the Apps',    desc:"Download the Parent APK on your phone and the Child APK on your child's phone." },
  { num:'2', icon:'📱', title:'Install Both APKs',     desc:'Go to Settings → Security → Install unknown apps → allow it. Then open and install each APK.' },
  { num:'3', icon:'👤', title:'Create Parent Account', desc:'Open the Parent app or this website → Create Account → register as a parent.' },
  { num:'4', icon:'🔗', title:'Setup Child Device',    desc:'On the child phone: open Child app → register child account → enter the pair code from parent app.' },
  { num:'5', icon:'✅', title:'Grant Permissions',     desc:'On child phone: tap Start Monitoring → grant Usage Access → enable Accessibility Service → allow Display Over Apps.' },
  { num:'6', icon:'🏠', title:'Set as Home Launcher',  desc:'When prompted, set Gia Family Control as the default home app on the child device.' },
];

const PERMS = [
  ['Usage Access',            'Required for app monitoring and blocking'],
  ['Accessibility Service',   'Required for remote lock and notification blocking'],
  ['Display Over Other Apps', 'Required for blocking notification panel'],
  ['Device Admin',            'Required to prevent uninstallation'],
  ['Location',                'Required for live GPS tracking'],
];

function useIsMobile() {
  const [m, setM] = useState(window.innerWidth <= 768);
  useEffect(() => {
    const fn = () => setM(window.innerWidth <= 768);
    window.addEventListener('resize', fn);
    return () => window.removeEventListener('resize', fn);
  }, []);
  return m;
}

export default function LandingPage() {
  const navigate = useNavigate();
  const isMobile = useIsMobile();
  const { isDark, toggleTheme } = useTheme();
  const [copied, setCopied]           = useState(false);
  const [latestVersion, setLatestVersion] = useState(null);
  const [menuOpen, setMenuOpen]       = useState(false);

  useEffect(() => {
    fetch(GITHUB_API)
      .then(r => r.json())
      .then(d => setLatestVersion(d.tag_name))
      .catch(() => {});
  }, []);

  const copyGcash = () => {
    navigator.clipboard.writeText(GCASH_NUMBER);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const close = () => setMenuOpen(false);

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-primary)', color: 'var(--text-primary)', fontFamily: 'var(--font-family)' }}>

      {/* ── NAV ── */}
      <nav style={n.nav}>
        <div style={n.brand}>
          <img src="/logo.jpg" alt="Gia" style={n.logo} />
          <span style={n.name}>Gia Family Control</span>
        </div>
        {!isMobile && (
          <div style={n.links}>
            {['features','download','setup','community','donate'].map(id => (
              <a key={id} href={`#${id}`} style={n.link}>{id.charAt(0).toUpperCase()+id.slice(1)}</a>
            ))}
          </div>
        )}
        <div style={{ display:'flex', alignItems:'center', gap:8 }}>
          <button style={n.themeBtn} onClick={toggleTheme} title="Toggle theme">
            {isDark ? '☀️' : '🌙'}
          </button>
          {!isMobile && (
            <button style={n.loginBtn} onClick={() => navigate('/login')}>Parent Login</button>
          )}
          {isMobile && (
            <button style={n.hamburger} onClick={() => setMenuOpen(v => !v)}>☰</button>
          )}
        </div>
      </nav>

      {/* mobile menu */}
      {isMobile && menuOpen && (
        <div style={n.mobileMenu}>
          {['features','download','setup','community','donate'].map(id => (
            <a key={id} href={`#${id}`} style={n.mobileLink} onClick={close}>
              {id.charAt(0).toUpperCase()+id.slice(1)}
            </a>
          ))}
          <button style={{ ...n.loginBtn, width:'100%', marginTop:8 }} onClick={() => { close(); navigate('/login'); }}>
            Parent Login
          </button>
        </div>
      )}

      {/* ── HERO ── */}
      <section style={{ ...sec.wrap, padding: isMobile ? '72px 20px 60px' : '100px 40px 80px', textAlign:'center', position:'relative', overflow:'hidden' }}>
        {/* glow blobs */}
        <div style={{ position:'absolute', top:'-10%', left:'20%', width:500, height:500, borderRadius:'50%', background:'radial-gradient(circle, rgba(99,102,241,0.15) 0%, transparent 70%)', pointerEvents:'none' }} />
        <div style={{ position:'absolute', top:'10%', right:'15%', width:400, height:400, borderRadius:'50%', background:'radial-gradient(circle, rgba(168,85,247,0.1) 0%, transparent 70%)', pointerEvents:'none' }} />

        <div style={{ position:'relative', maxWidth:760, margin:'0 auto' }}>
          <div style={hero.badge}>🛡️ Free &amp; Open Source Parental Control</div>
          <h1 style={{ fontSize: isMobile ? 36 : 60, fontWeight:800, lineHeight:1.1, letterSpacing:'-2px', margin:'20px 0 24px' }}>
            Keep Your Child Safe{' '}
            <span style={{ background:'linear-gradient(135deg,#6366f1,#a855f7,#ec4899)', WebkitBackgroundClip:'text', WebkitTextFillColor:'transparent', backgroundClip:'text' }}>
              in the Digital World
            </span>
          </h1>
          <p style={{ fontSize: isMobile ? 16 : 19, color:'var(--text-secondary)', lineHeight:1.7, maxWidth:580, margin:'0 auto 36px' }}>
            Monitor location, block apps, lock devices, and control screen time — all from your phone or browser.
          </p>
          <div style={{ display:'flex', gap:12, justifyContent:'center', flexWrap:'wrap' }}>
            <a href="#download" style={hero.btnPrimary}>📥 Download Free</a>
            <button style={hero.btnSecondary} onClick={() => navigate('/login')}>Open Dashboard →</button>
          </div>
          {latestVersion && (
            <div style={{ marginTop:24, fontSize:13, color:'var(--text-tertiary)' }}>
              Latest release: <span style={{ color:'var(--accent-primary)', fontWeight:600 }}>{latestVersion}</span>
            </div>
          )}
        </div>
      </section>

      {/* ── FEATURES ── */}
      <section id="features" style={{ ...sec.wrap, padding: isMobile ? '60px 20px' : '80px 40px' }}>
        <SectionHeader title="Everything You Need" sub="Powerful parental controls designed to be simple for parents and effective against tech-savvy kids." />
        <div style={{ display:'grid', gridTemplateColumns: isMobile ? '1fr 1fr' : 'repeat(3,1fr)', gap:16, maxWidth:1000, margin:'0 auto' }}>
          {FEATURES.map(f => (
            <div key={f.title} style={card.feat}>
              <span style={{ fontSize:32, display:'block', marginBottom:14 }}>{f.icon}</span>
              <h3 style={{ fontSize:15, fontWeight:700, marginBottom:8, color:'var(--text-primary)' }}>{f.title}</h3>
              <p style={{ fontSize:13, color:'var(--text-secondary)', lineHeight:1.6 }}>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── DOWNLOAD ── */}
      <section id="download" style={{ background:'var(--bg-secondary)', padding: isMobile ? '60px 20px' : '80px 40px' }}>
        <div style={sec.wrap}>
          <SectionHeader title="Download" sub="Two apps — one for the parent, one for the child. Both are free." />
          <div style={{ display:'grid', gridTemplateColumns: isMobile ? '1fr' : 'repeat(2,1fr)', gap:20, maxWidth:700, margin:'0 auto 24px' }}>
            <DownloadCard
              icon="👨‍👩‍👧" title="Parent App"
              desc="Install on your phone. Monitor, lock, and control your child's device."
              href={DOWNLOAD_PARENT} label="📥 Download Parent APK"
              color="#6366f1"
            />
            <DownloadCard
              icon="👦" title="Child App"
              desc="Install on your child's phone. Works openly with child's knowledge."
              href={DOWNLOAD_CHILD} label="📥 Download Child APK"
              color="#34d399"
            />
          </div>
          <p style={{ textAlign:'center', fontSize:13, color:'var(--text-tertiary)', marginBottom:24 }}>
            ⚠️ If download doesn't start, tap and hold → Open in browser.
          </p>
          <div style={card.warn}>
            <div style={{ fontSize:15, fontWeight:700, color:'#fbbf24', marginBottom:8 }}>🛡️ Play Protect Warning — How to Install</div>
            <p style={{ fontSize:13, color:'var(--text-secondary)', lineHeight:1.6, marginBottom:14 }}>
              Google Play Protect may show a warning. This is normal for apps not yet on the Play Store. The app is <strong>safe</strong> — it is a transparent parental control app.
            </p>
            <div style={{ display:'flex', gap:10, flexWrap:'wrap', justifyContent:'center' }}>
              <a href={INSTALL_VIDEO} target="_blank" rel="noreferrer" style={{ ...hero.btnSecondary, fontSize:13, padding:'9px 18px', color:'#fbbf24', borderColor:'rgba(251,191,36,0.3)' }}>🎥 Watch Install Video</a>
              <a href={FB_GROUP} target="_blank" rel="noreferrer" style={{ ...hero.btnSecondary, fontSize:13, padding:'9px 18px', color:'#34d399', borderColor:'rgba(52,211,153,0.3)' }}>👍 Join Facebook Group</a>
            </div>
          </div>
        </div>
      </section>

      {/* ── SETUP ── */}
      <section id="setup" style={{ ...sec.wrap, padding: isMobile ? '60px 20px' : '80px 40px' }}>
        <SectionHeader title="Setup Guide" sub="Follow these steps to get started in under 10 minutes." />
        <div style={{ maxWidth:700, margin:'0 auto 36px', borderRadius:16, overflow:'hidden', border:'1px solid var(--border-primary)' }}>
          <div style={{ position:'relative', paddingBottom:'56.25%', height:0 }}>
            <iframe src="https://www.youtube.com/embed/I-hzPOM08f4" title="Tutorial"
              frameBorder="0" allowFullScreen
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
              style={{ position:'absolute', top:0, left:0, width:'100%', height:'100%' }} />
          </div>
        </div>
        <div style={{ display:'flex', flexDirection:'column', gap:12, maxWidth:700, margin:'0 auto 28px' }}>
          {STEPS.map(step => (
            <div key={step.num} style={card.step}>
              <span style={{ fontSize:28, flexShrink:0 }}>{step.icon}</span>
              <div>
                <div style={{ fontSize:14, fontWeight:700, color:'var(--text-primary)', marginBottom:4 }}>Step {step.num} — {step.title}</div>
                <div style={{ fontSize:13, color:'var(--text-secondary)', lineHeight:1.5 }}>{step.desc}</div>
              </div>
            </div>
          ))}
        </div>
        <div style={{ ...card.base, maxWidth:700, margin:'0 auto', padding: isMobile ? 20 : 28 }}>
          <h3 style={{ fontSize:15, fontWeight:700, marginBottom:16, color:'var(--text-primary)' }}>📋 Permissions Required on Child Device</h3>
          <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
            {PERMS.map(([name, desc]) => (
              <div key={name} style={{ display:'flex', gap:10, alignItems:'flex-start' }}>
                <span style={{ fontSize:15, flexShrink:0 }}>✅</span>
                <div>
                  <div style={{ fontSize:13, fontWeight:600, color:'var(--text-primary)', marginBottom:2 }}>{name}</div>
                  <div style={{ fontSize:12, color:'var(--text-tertiary)' }}>{desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── COMMUNITY ── */}
      <section id="community" style={{ background:'var(--bg-secondary)', padding: isMobile ? '60px 20px' : '80px 40px' }}>
        <div style={sec.wrap}>
          <SectionHeader title="Join Our Community 👥" sub="Get help, report bugs, share feedback, and stay updated with new features!" />
          <div style={{ maxWidth:480, margin:'0 auto', textAlign:'center' }}>
            <div style={{ ...card.base, padding:36 }}>
              <span style={{ fontSize:52, display:'block', marginBottom:16 }}>👥</span>
              <h3 style={{ fontSize:20, fontWeight:700, marginBottom:10, color:'var(--text-primary)' }}>Gia Family Control — Beta Testers</h3>
              <p style={{ fontSize:14, color:'var(--text-secondary)', lineHeight:1.7, marginBottom:24 }}>
                Connect with other parents, watch tutorials, and help make the app better for everyone!
              </p>
              <a href={FB_GROUP} target="_blank" rel="noreferrer" style={{ display:'inline-block', padding:'13px 32px', background:'#1877f2', color:'#fff', borderRadius:10, fontSize:15, fontWeight:700, textDecoration:'none', marginBottom:20 }}>
                👍 Join Facebook Group
              </a>
              <div style={{ display:'flex', gap:10, justifyContent:'center', flexWrap:'wrap' }}>
                {SOCIAL.map(s => (
                  <a key={s.label} href={s.href} target="_blank" rel="noreferrer"
                    style={{ display:'inline-flex', alignItems:'center', gap:6, padding:'8px 14px', borderRadius:8, fontSize:13, fontWeight:600, textDecoration:'none', background: s.color+'22', color: s.color, border:`1px solid ${s.color}44` }}>
                    {s.icon} {s.label}
                  </a>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── DONATE ── */}
      <section id="donate" style={{ ...sec.wrap, padding: isMobile ? '60px 20px' : '80px 40px' }}>
        <SectionHeader title="Support This Project ❤️" sub="Gia Family Control is completely free. If it helped keep your child safe, consider buying me a coffee!" />
        <div style={{ maxWidth:320, margin:'0 auto 20px' }}>
          <div style={{ ...card.base, padding:32, textAlign:'center' }}>
            <span style={{ fontSize:44, display:'block', marginBottom:14 }}>💙</span>
            <h3 style={{ fontSize:18, fontWeight:700, marginBottom:6, color:'var(--text-primary)' }}>GCash</h3>
            <p style={{ fontSize:13, color:'var(--text-secondary)', marginBottom:4 }}>Jed Tipudan</p>
            <p style={{ fontSize:20, fontWeight:700, color:'var(--accent-primary)', marginBottom:20, fontFamily:'ui-monospace,monospace' }}>{GCASH_NUMBER}</p>
            <button style={{ width:'100%', padding:'11px 0', background:'var(--accent-subtle)', color:'var(--accent-primary)', border:'1px solid var(--accent-glow)', borderRadius:8, fontSize:14, fontWeight:600, cursor:'pointer' }} onClick={copyGcash}>
              {copied ? '✅ Copied!' : '📋 Copy Number'}
            </button>
          </div>
        </div>
        <p style={{ textAlign:'center', fontSize:13, color:'var(--text-tertiary)' }}>Your donation helps pay for server costs and future development. Thank you! 🙏</p>
      </section>

      {/* ── FOOTER ── */}
      <footer style={{ borderTop:'1px solid var(--border-primary)', padding:'32px 20px', background:'var(--bg-secondary)' }}>
        <div style={{ maxWidth:1000, margin:'0 auto', textAlign:'center' }}>
          <div style={{ display:'flex', alignItems:'center', justifyContent:'center', gap:10, marginBottom:10 }}>
            <img src="/logo.jpg" alt="Gia" style={{ width:26, height:26, borderRadius:6 }} />
            <span style={{ fontSize:15, fontWeight:700, color:'var(--text-primary)' }}>Gia Family Control</span>
          </div>
          <p style={{ fontSize:13, color:'var(--text-tertiary)', marginBottom:16 }}>Free parental control app for Android. Keep your family safe.</p>
          <div style={{ display:'flex', gap:10, justifyContent:'center', flexWrap:'wrap', marginBottom:16 }}>
            {SOCIAL.map(s => (
              <a key={s.label} href={s.href} target="_blank" rel="noreferrer"
                style={{ display:'inline-flex', alignItems:'center', gap:6, padding:'7px 14px', borderRadius:8, fontSize:13, fontWeight:600, textDecoration:'none', background: s.color+'22', color: s.color, border:`1px solid ${s.color}44` }}>
                {s.icon} {s.label}
              </a>
            ))}
          </div>
          <div style={{ display:'flex', justifyContent:'center', gap:20, flexWrap:'wrap' }}>
            {['features','download','setup','community','donate'].map(id => (
              <a key={id} href={`#${id}`} style={{ fontSize:13, color:'var(--text-secondary)', textDecoration:'none' }}>
                {id.charAt(0).toUpperCase()+id.slice(1)}
              </a>
            ))}
            <button style={{ fontSize:13, color:'var(--text-secondary)', background:'none', border:'none', cursor:'pointer', padding:0 }} onClick={() => navigate('/login')}>Login</button>
          </div>
        </div>
      </footer>
    </div>
  );
}

function SectionHeader({ title, sub }) {
  return (
    <div style={{ textAlign:'center', marginBottom:40 }}>
      <h2 style={{ fontSize:32, fontWeight:800, letterSpacing:'-1px', marginBottom:12, color:'var(--text-primary)' }}>{title}</h2>
      <p style={{ fontSize:16, color:'var(--text-secondary)', maxWidth:520, margin:'0 auto', lineHeight:1.7 }}>{sub}</p>
    </div>
  );
}

function DownloadCard({ icon, title, desc, href, label, color }) {
  return (
    <div style={{ ...card.base, padding:28, textAlign:'center' }}>
      <span style={{ fontSize:44, display:'block', marginBottom:14 }}>{icon}</span>
      <h3 style={{ fontSize:18, fontWeight:700, marginBottom:8, color:'var(--text-primary)' }}>{title}</h3>
      <p style={{ fontSize:13, color:'var(--text-secondary)', marginBottom:20, lineHeight:1.5 }}>{desc}</p>
      <a href={href} style={{ display:'block', padding:'12px 0', background: color+'22', color, border:`1px solid ${color}44`, borderRadius:10, fontSize:14, fontWeight:700, textDecoration:'none' }}>
        {label}
      </a>
    </div>
  );
}

const sec = {
  wrap: { maxWidth:1100, margin:'0 auto' },
};

const n = {
  nav:       { display:'flex', alignItems:'center', justifyContent:'space-between', padding:'14px 24px', borderBottom:'1px solid var(--border-primary)', background:'var(--bg-secondary)', position:'sticky', top:0, zIndex:100, backdropFilter:'blur(12px)' },
  brand:     { display:'flex', alignItems:'center', gap:10 },
  logo:      { width:32, height:32, borderRadius:8 },
  name:      { fontSize:15, fontWeight:700, color:'var(--text-primary)' },
  links:     { display:'flex', alignItems:'center', gap:24 },
  link:      { fontSize:14, color:'var(--text-secondary)', textDecoration:'none', fontWeight:500 },
  themeBtn:  { background:'var(--bg-tertiary)', border:'1px solid var(--border-primary)', borderRadius:8, padding:'6px 10px', fontSize:16, cursor:'pointer', lineHeight:1 },
  loginBtn:  { padding:'8px 18px', background:'var(--accent-primary)', color:'#fff', border:'none', borderRadius:8, fontSize:14, fontWeight:600, cursor:'pointer' },
  hamburger: { background:'none', border:'none', fontSize:22, color:'var(--text-primary)', cursor:'pointer', padding:4 },
  mobileMenu:{ background:'var(--bg-secondary)', borderBottom:'1px solid var(--border-primary)', padding:'12px 24px 20px', display:'flex', flexDirection:'column', gap:4 },
  mobileLink:{ padding:'10px 0', fontSize:15, color:'var(--text-secondary)', textDecoration:'none', fontWeight:500, borderBottom:'1px solid var(--border-primary)' },
};

const hero = {
  badge:       { display:'inline-block', padding:'6px 16px', background:'var(--accent-subtle)', border:'1px solid var(--accent-glow)', borderRadius:980, fontSize:13, color:'var(--accent-primary)', fontWeight:600 },
  btnPrimary:  { padding:'14px 28px', background:'linear-gradient(135deg,#6366f1,#a855f7)', color:'#fff', borderRadius:10, fontSize:15, fontWeight:700, textDecoration:'none', display:'inline-block', boxShadow:'0 4px 20px rgba(99,102,241,0.4)' },
  btnSecondary:{ padding:'14px 28px', background:'var(--bg-secondary)', color:'var(--text-primary)', border:'1px solid var(--border-primary)', borderRadius:10, fontSize:15, fontWeight:600, cursor:'pointer', textDecoration:'none', display:'inline-block' },
};

const card = {
  base: { background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:16 },
  feat: { padding:24, background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:16, transition:'border-color 0.2s' },
  step: { display:'flex', alignItems:'flex-start', gap:14, padding:18, background:'var(--bg-secondary)', border:'1px solid var(--border-primary)', borderRadius:12 },
  warn: { maxWidth:700, margin:'0 auto', padding:24, background:'rgba(251,191,36,0.06)', border:'1px solid rgba(251,191,36,0.25)', borderRadius:14, textAlign:'center' },
};
