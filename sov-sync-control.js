// SOV Web v5.34 — lightweight sync control helper
(function () {
  const KEY = 'sov_sync_control_state_v1';
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
    const label = failed ? `${failed} greška / ${pending} čeka` : pending ? `${pending} čeka sync` : 'Sync miran';
    el.textContent = label;
    el.dataset.syncState = failed ? 'failed' : pending ? 'pending' : 'ok';
  }
  window.SOV_SYNC_CONTROL = { loadState, saveState, renderSyncBadge };
})();
