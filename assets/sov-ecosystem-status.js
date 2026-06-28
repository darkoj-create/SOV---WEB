(function(){
  async function loadManifest(){
    const fallback = {web_version:'6.1.45-ecosystem-baseline', apk_version:'1.4.28-ecosystem-baseline', backend_contract:'2026.06.27', supabase_project_ref:'ncomefzkuixyfixisrhi', release_channel:'baseline'};
    try { const r = await fetch('./assets/sov-ecosystem-manifest.json', {cache:'no-store'}); return Object.assign(fallback, await r.json()); }
    catch(e){ return fallback; }
  }
  function row(k,v){ return `<div class="sov-row"><span>${k}</span><b>${v || '—'}</b></div>`; }
  document.addEventListener('DOMContentLoaded', async ()=>{
    const m = await loadManifest();
    const el = document.getElementById('sov-ecosystem-status-root');
    if(!el) return;
    el.innerHTML = `
      <section class="sov-card hero"><p class="eyebrow">SOV Ecosystem Baseline</p><h1>System status</h1><p>Kontrolni ekran za web/APK/backend contract. Ovaj build ne mijenja postojeće funkcije.</p></section>
      <section class="sov-grid">
        <div class="sov-card"><h2>Build</h2>${row('Web', m.web_version)}${row('APK target', m.apk_version)}${row('Backend contract', m.backend_contract)}${row('Channel', m.release_channel)}</div>
        <div class="sov-card"><h2>Supabase</h2>${row('Project', m.supabase_project_ref)}${row('Name', m.supabase_project_name)}${row('Region', m.supabase_region)}${row('SQL pack', m.required_sql_pack)}</div>
        <div class="sov-card warn"><h2>Security note</h2><p>Supabase hardening mora ići odvojeno: prvo snapshot, zatim phase1 zatvaranje anon/security-definer rupa. Ne miješati s APK funkcionalnim promjenama.</p></div>
        <div class="sov-card"><h2>Masters</h2>${Object.entries(m.masters||{}).map(([k,v])=>row(k,v)).join('')}</div>
      </section>`;
  });
})();
