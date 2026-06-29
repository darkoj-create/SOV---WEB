// SOV Web v5.35 — sync/audit/device lightweight helper
(function () {
  const KEY = 'sov_sync_control_state_v2';
  function loadState() {
    try { return JSON.parse(localStorage.getItem(KEY) || '{}'); } catch (_) { return {}; }
  }
  function saveState(next) {
    const merged = Object.assign({}, loadState(), next, { updatedAt: new Date().toISOString() });
    localStorage.setItem(KEY, JSON.stringify(merged));
    return merged;
  }
  function renderSyncBadge(target) {
    const el = typeof target === 'string' ? document.querySelector(target) : target;
    if (!el) return;
    const s = loadState();
    const pending = Number(s.pending || 0);
    const failed = Number(s.failed || 0);
    const devices = Number(s.devices || 0);
    const label = failed ? `${failed} greška / ${pending} čeka` : pending ? `${pending} čeka sync` : devices ? `${devices} uređaj(a) aktivno` : 'Sync miran';
    el.textContent = label;
    el.dataset.syncState = failed ? 'failed' : pending ? 'pending' : 'ok';
  }
  function tableRows(rows) {
    if (!rows || !rows.length) return '<tr><td colspan="5" class="muted">Nema zapisa.</td></tr>';
    return rows.map(r => `<tr><td>${escapeHtml(r.created_at || r.last_seen_at || '')}</td><td>${escapeHtml(r.actor_email || r.device_label || '')}</td><td>${escapeHtml(r.action || r.platform || '')}</td><td>${escapeHtml(r.entity_type || r.app_version || '')}</td><td>${escapeHtml(r.status || r.sync_state || '')}</td></tr>`).join('');
  }
  function escapeHtml(v){ return String(v ?? '').replace(/[&<>"]/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','\"':'&quot;'}[c] || c)); }
  window.SOV_SYNC_CONTROL = { loadState, saveState, renderSyncBadge, tableRows };
})();
