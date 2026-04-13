import React, { useState, useEffect } from 'react';
import { scheduleApi } from '../services/api';
import { toast } from './Toast';

const DAYS = ['MON','TUE','WED','THU','FRI','SAT','SUN'];
const DAY_LABELS = { MON:'Mon', TUE:'Tue', WED:'Wed', THU:'Thu', FRI:'Fri', SAT:'Sat', SUN:'Sun' };

const EMPTY = { deviceId: null, label: 'Bedtime', lockTime: '20:00', unlockTime: '06:00', days: '', isActive: true };

export default function SchedulePage({ deviceId }) {
  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [modal, setModal]         = useState(null); // null | { mode:'create'|'edit', data }

  const load = async () => {
    try {
      const { data } = await scheduleApi.list();
      setSchedules(data);
    } catch { toast('Failed to load schedules', 'error'); }
    finally { setLoading(false); }
  };

  useEffect(() => { load(); }, []);

  const openCreate = () => setModal({ mode: 'create', data: { ...EMPTY, deviceId } });
  const openEdit   = (s)  => setModal({ mode: 'edit',   data: { ...s } });
  const closeModal = ()   => setModal(null);

  const save = async () => {
    const { mode, data } = modal;
    try {
      if (mode === 'create') await scheduleApi.create(data);
      else                   await scheduleApi.update(data.id, data);
      toast(mode === 'create' ? 'Schedule created' : 'Schedule updated', 'success');
      closeModal();
      load();
    } catch (err) {
      const msg = err?.response?.data?.message ?? 'Save failed';
      toast(msg, 'error');
    }
  };

  const remove = async (id) => {
    if (!window.confirm('Delete this schedule?')) return;
    try {
      await scheduleApi.delete(id);
      toast('Schedule deleted', 'success');
      load();
    } catch { toast('Delete failed', 'error'); }
  };

  const toggleActive = async (s) => {
    try {
      await scheduleApi.update(s.id, { ...s, isActive: !s.isActive });
      load();
    } catch { toast('Update failed', 'error'); }
  };

  const setField = (key, val) => setModal(m => ({ ...m, data: { ...m.data, [key]: val } }));

  const toggleDay = (day) => {
    const current = modal.data.days ? modal.data.days.split(',').filter(Boolean) : [];
    const next = current.includes(day) ? current.filter(d => d !== day) : [...current, day];
    setField('days', next.join(','));
  };

  const selectedDays = (days) => days ? days.split(',').filter(Boolean) : [];

  const fmt = (t) => {
    if (!t) return '--';
    const [h, m] = t.split(':');
    const hr = parseInt(h);
    return `${hr % 12 || 12}:${m} ${hr < 12 ? 'AM' : 'PM'}`;
  };

  return (
    <div style={s.wrap}>
      <div style={s.header}>
        <div>
          <h2 style={s.title}>Scheduled Lock</h2>
          <p style={s.sub}>Automatically lock and unlock the device at set times.</p>
        </div>
        <button style={s.addBtn} onClick={openCreate}>+ New Schedule</button>
      </div>

      {loading ? (
        <div style={s.empty}>Loading…</div>
      ) : schedules.length === 0 ? (
        <div style={s.emptyCard}>
          <span style={{ fontSize: 48, display: 'block', marginBottom: 12 }}>⏰</span>
          <p style={{ fontWeight: 700, fontSize: 16, marginBottom: 6, color: 'var(--text-primary)' }}>No schedules yet</p>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', marginBottom: 20 }}>Create a schedule to automatically lock the device at bedtime.</p>
          <button style={s.addBtn} onClick={openCreate}>+ Create Schedule</button>
        </div>
      ) : (
        <div style={s.list}>
          {schedules.map(sc => (
            <div key={sc.id} style={{ ...s.card, opacity: sc.isActive ? 1 : 0.55 }}>
              <div style={s.cardLeft}>
                <div style={s.cardLabel}>{sc.label || 'Schedule'}</div>
                <div style={s.cardTimes}>
                  <span style={s.lockTime}>🔒 {fmt(sc.lockTime)}</span>
                  <span style={s.arrow}>→</span>
                  <span style={s.unlockTime}>🔓 {fmt(sc.unlockTime)}</span>
                </div>
                <div style={s.cardDays}>
                  {selectedDays(sc.days).length === 0
                    ? <span style={s.dayChip}>Every day</span>
                    : selectedDays(sc.days).map(d => <span key={d} style={s.dayChip}>{DAY_LABELS[d]}</span>)
                  }
                </div>
              </div>
              <div style={s.cardRight}>
                <button style={{ ...s.toggle, background: sc.isActive ? 'rgba(52,211,153,0.15)' : 'var(--bg-tertiary)', color: sc.isActive ? '#34d399' : 'var(--text-tertiary)', border: `1px solid ${sc.isActive ? 'rgba(52,211,153,0.3)' : 'var(--border-primary)'}` }}
                  onClick={() => toggleActive(sc)}>
                  {sc.isActive ? 'ON' : 'OFF'}
                </button>
                <button style={s.editBtn} onClick={() => openEdit(sc)}>✏️</button>
                <button style={s.delBtn}  onClick={() => remove(sc.id)}>🗑️</button>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* MODAL */}
      {modal && (
        <div style={s.overlay} onClick={closeModal}>
          <div style={s.modal} onClick={e => e.stopPropagation()}>
            <div style={s.modalHeader}>
              <span style={s.modalTitle}>{modal.mode === 'create' ? '+ New Schedule' : 'Edit Schedule'}</span>
              <button style={s.closeBtn} onClick={closeModal}>✕</button>
            </div>

            <div style={s.field}>
              <label style={s.label}>Label</label>
              <input style={s.input} value={modal.data.label} onChange={e => setField('label', e.target.value)} placeholder="e.g. Bedtime" />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
              <div style={s.field}>
                <label style={s.label}>🔒 Lock Time</label>
                <input style={s.input} type="time" value={modal.data.lockTime} onChange={e => setField('lockTime', e.target.value)} />
              </div>
              <div style={s.field}>
                <label style={s.label}>🔓 Unlock Time</label>
                <input style={s.input} type="time" value={modal.data.unlockTime} onChange={e => setField('unlockTime', e.target.value)} />
              </div>
            </div>

            <div style={s.field}>
              <label style={s.label}>Days <span style={{ color: 'var(--text-tertiary)', fontWeight: 400 }}>(leave empty = every day)</span></label>
              <div style={s.daysRow}>
                {DAYS.map(d => {
                  const active = selectedDays(modal.data.days).includes(d);
                  return (
                    <button key={d} style={{ ...s.dayBtn, background: active ? 'var(--accent-subtle)' : 'var(--bg-tertiary)', color: active ? 'var(--accent-primary)' : 'var(--text-tertiary)', border: `1px solid ${active ? 'var(--accent-glow)' : 'var(--border-primary)'}` }}
                      onClick={() => toggleDay(d)}>
                      {DAY_LABELS[d]}
                    </button>
                  );
                })}
              </div>
            </div>

            <div style={s.field}>
              <label style={s.label}>Status</label>
              <div style={{ display: 'flex', gap: 8 }}>
                {[true, false].map(v => (
                  <button key={String(v)} style={{ ...s.dayBtn, flex: 1, background: modal.data.isActive === v ? 'var(--accent-subtle)' : 'var(--bg-tertiary)', color: modal.data.isActive === v ? 'var(--accent-primary)' : 'var(--text-tertiary)', border: `1px solid ${modal.data.isActive === v ? 'var(--accent-glow)' : 'var(--border-primary)'}` }}
                    onClick={() => setField('isActive', v)}>
                    {v ? '✅ Active' : '⏸ Paused'}
                  </button>
                ))}
              </div>
            </div>

            <button style={s.saveBtn} onClick={save}>
              {modal.mode === 'create' ? 'Create Schedule' : 'Save Changes'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}

const s = {
  wrap:        { padding: 0 },
  header:      { display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', marginBottom: 24, flexWrap: 'wrap', gap: 12 },
  title:       { fontSize: 18, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 4 },
  sub:         { fontSize: 13, color: 'var(--text-secondary)' },
  addBtn:      { padding: '9px 18px', background: 'var(--accent-primary)', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer' },
  empty:       { textAlign: 'center', color: 'var(--text-tertiary)', padding: 40 },
  emptyCard:   { textAlign: 'center', padding: '48px 24px', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 16 },
  list:        { display: 'flex', flexDirection: 'column', gap: 12 },
  card:        { display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '18px 20px', background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 14, gap: 12, flexWrap: 'wrap' },
  cardLeft:    { flex: 1 },
  cardLabel:   { fontSize: 15, fontWeight: 700, color: 'var(--text-primary)', marginBottom: 6 },
  cardTimes:   { display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 },
  lockTime:    { fontSize: 14, fontWeight: 600, color: '#f87171' },
  unlockTime:  { fontSize: 14, fontWeight: 600, color: '#34d399' },
  arrow:       { fontSize: 14, color: 'var(--text-tertiary)' },
  cardDays:    { display: 'flex', gap: 6, flexWrap: 'wrap' },
  dayChip:     { padding: '3px 10px', background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', borderRadius: 980, fontSize: 12, color: 'var(--text-secondary)' },
  cardRight:   { display: 'flex', alignItems: 'center', gap: 8 },
  toggle:      { padding: '6px 14px', borderRadius: 8, fontSize: 13, fontWeight: 700, cursor: 'pointer' },
  editBtn:     { background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', borderRadius: 8, padding: '6px 10px', cursor: 'pointer', fontSize: 14 },
  delBtn:      { background: 'rgba(248,113,113,0.1)', border: '1px solid rgba(248,113,113,0.25)', borderRadius: 8, padding: '6px 10px', cursor: 'pointer', fontSize: 14 },
  overlay:     { position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.6)', backdropFilter: 'blur(6px)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 20 },
  modal:       { background: 'var(--bg-secondary)', border: '1px solid var(--border-primary)', borderRadius: 18, padding: 28, width: '100%', maxWidth: 460, display: 'flex', flexDirection: 'column', gap: 16 },
  modalHeader: { display: 'flex', alignItems: 'center', justifyContent: 'space-between' },
  modalTitle:  { fontSize: 17, fontWeight: 700, color: 'var(--text-primary)' },
  closeBtn:    { background: 'none', border: 'none', color: 'var(--text-tertiary)', fontSize: 18, cursor: 'pointer', padding: 4 },
  field:       { display: 'flex', flexDirection: 'column', gap: 6 },
  label:       { fontSize: 13, fontWeight: 600, color: 'var(--text-secondary)' },
  input:       { padding: '10px 12px', background: 'var(--bg-tertiary)', border: '1px solid var(--border-primary)', borderRadius: 8, fontSize: 14, color: 'var(--text-primary)', width: '100%' },
  daysRow:     { display: 'flex', gap: 6, flexWrap: 'wrap' },
  dayBtn:      { padding: '7px 12px', borderRadius: 8, fontSize: 13, fontWeight: 600, cursor: 'pointer' },
  saveBtn:     { padding: '12px 0', background: 'linear-gradient(135deg,#6366f1,#a855f7)', color: '#fff', border: 'none', borderRadius: 10, fontSize: 15, fontWeight: 700, cursor: 'pointer', marginTop: 4 },
};
