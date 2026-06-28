(function(){
  'use strict';
  const BUILD = '6.1.45v-system-status-wow-admin';
  const state = {checks:[], snapshot:null, started:performance.now()};
  const $ = (sel, root=document) => root.querySelector(sel);
  const $$ = (sel, root=document) => Array.from(root.querySelectorAll(sel));
  const fmt = new Intl.NumberFormat('hr-HR');
  function esc(v){ return String(v ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c])); }
  function log(line){ const el=$('#sov-status-log'); if(el){ el.textContent += `[${new Date().toLocaleTimeString('hr-HR')}] ${line}\n`; el.scrollTop = el.scrollHeight; } }
  function ms(n){ return `${Math.round(n)} ms`; }
  function statusClass(s){ return s === 'ok' ? 'ok' : (s === 'bad' ? 'bad' : 'warn'); }
  function setText(id, val){ const el = $(id); if(el){ el.textContent = val; el.classList.remove('sov-skeleton'); } }
  function setChip(id, label, status){ const el=$(id); if(!el) return; el.className = `sov-status-chip ${statusClass(status)}`; el.textContent = label; }
  function markScore(status, title, subtitle){ const dot=$('#overall-dot'); if(dot) dot.className = `sov-score-dot ${statusClass(status)}`; setText('#overall-title', title); setText('#overall-subtitle', subtitle); setChip('#overall-chip', title, status); }
  async function timeout(promise, limitMs, label){
    let timer;
    const killer = new Promise((_, reject) => { timer = setTimeout(() => reject(new Error(`${label} timeout ${limitMs}ms`)), limitMs); });
    try{ return await Promise.race([promise, killer]); }
    finally{ clearTimeout(timer); }
  }
  async function measure(name, fn, critical=false){
    const started = performance.now();
    try{
      const data = await timeout(Promise.resolve().then(fn), 8500, name);
      const elapsed = performance.now() - started;
      state.checks.push({name,status:'ok',ms:elapsed,critical});
      log(`OK ${name} (${ms(elapsed)})`);
      return {ok:true,data,ms:elapsed};
    }catch(error){
      const elapsed = performance.now() - started;
      state.checks.push({name,status: critical ? 'bad' : 'warn',ms:elapsed,critical,error:String(error && error.message || error)});
      log(`${critical ? 'ERROR' : 'WARN'} ${name}: ${error && error.message ? error.message : error}`);
      return {ok:false,error,ms:elapsed};
    }
  }
  function renderChecks(){
    const tbody = $('#checks-body'); if(!tbody) return;
    tbody.innerHTML = state.checks.map(c => `
      <tr>
        <td><b>${esc(c.name)}</b>${c.error ? `<div class="sov-muted">${esc(c.error)}</div>` : ''}</td>
        <td>${c.critical ? 'Da' : 'Ne'}</td>
        <td><span class="sov-status-chip ${statusClass(c.status)}">${c.status === 'ok' ? 'OK' : c.status === 'bad' ? 'Error' : 'Warning'}</span></td>
        <td>${ms(c.ms)}</td>
      </tr>`).join('');
  }
  async function countRows(sb, table, label, critical=false){
    return measure(label || table, async () => {
      const {count,error} = await sb.from(table).select('*', {count:'exact', head:true});
      if(error) throw error;
      return count || 0;
    }, critical);
  }
  async function selectOne(sb, table, label, critical=false){
    return measure(label || table, async () => {
      const {data,error} = await sb.from(table).select('*').limit(1).maybeSingle();
      if(error) throw error;
      return data || null;
    }, critical);
  }
  function safeNumber(v){ const n = Number(v); return Number.isFinite(n) ? n : null; }
  function renderSnapshot(profile, localManifest, live){
    const manifest = live.manifest || localManifest || {};
    const catalogManifest = live.catalogManifest || {};
    const armoryCatalog = safeNumber(live.catalogCount?.data ?? catalogManifest.grouped_row_count);
    const requests = safeNumber(live.requestsCount?.data);
    const pending = safeNumber(live.pendingRequests?.data);
    const loans = safeNumber(live.loansCount?.data);
    const noRls = safeNumber(live.tablesWithoutRls?.data);
    const openPolicies = safeNumber(live.openPolicies?.data);
    const hardening = safeNumber(live.hardeningRows?.data);
    setText('#user-line', profile ? `${profile.full_name || profile.email || 'Admin'} · ${profile.role || 'admin'}` : 'Nije učitano');
    setText('#web-version', BUILD);
    setText('#backend-contract', manifest.backend_contract || 'nije upisano');
    setText('#apk-target', manifest.apk_version || manifest.apk_target || 'nije upisano');
    setText('#project-ref', window.SOV_SUPABASE_URL ? String(window.SOV_SUPABASE_URL).replace('https://','').replace('.supabase.co','') : 'nije konfigurirano');
    setText('#kpi-catalog', armoryCatalog == null ? '—' : fmt.format(armoryCatalog));
    setText('#kpi-requests', requests == null ? '—' : fmt.format(requests));
    setText('#kpi-pending', pending == null ? '—' : fmt.format(pending));
    setText('#kpi-loans', loans == null ? '—' : fmt.format(loans));
    setText('#catalog-version', catalogManifest.catalog_version || '—');
    setText('#catalog-changed', catalogManifest.last_changed_at ? new Date(catalogManifest.last_changed_at).toLocaleString('hr-HR') : '—');
    setText('#requests-changed', catalogManifest.requests_changed_at ? new Date(catalogManifest.requests_changed_at).toLocaleString('hr-HR') : '—');
    setText('#security-rls', noRls == null ? '—' : fmt.format(noRls));
    setText('#security-policies', openPolicies == null ? '—' : fmt.format(openPolicies));
    setText('#security-log', hardening == null ? '—' : fmt.format(hardening));
    const criticalBad = state.checks.some(c => c.critical && c.status !== 'ok');
    const warn = state.checks.some(c => !c.critical && c.status !== 'ok') || (noRls != null && noRls > 0) || (pending != null && pending > 0);
    if(criticalBad){ markScore('bad','Problem','Kritična provjera nije prošla.'); }
    else if(warn){ markScore('warn','Warning','Sustav radi, ali ima stvari za provjeru.'); }
    else { markScore('ok','OK','Web, auth i baza odgovaraju.'); }
    const payload = {build:BUILD, checked_at:new Date().toISOString(), profile, manifest, catalogManifest, armory:{catalog:armoryCatalog,requests,pending,loans}, security:{tables_without_rls:noRls,open_policies:openPolicies,hardening_log_rows:hardening}, checks:state.checks};
    const raw=$('#raw-json'); if(raw) raw.textContent = JSON.stringify(payload,null,2);
  }
  async function init(){
    $('#refresh-status')?.addEventListener('click', () => location.reload());
    log(`Pokrećem ${BUILD}`);
    setText('#web-version', BUILD);
    let localManifest = null;
    await measure('Lokalni ecosystem manifest', async () => {
      const r = await fetch('/assets/sov-ecosystem-manifest.json', {cache:'no-store'});
      if(!r.ok) throw new Error(`HTTP ${r.status}`);
      localManifest = await r.json();
      return localManifest;
    });
    const configured = !!(window.SOV_SUPABASE_URL && window.SOV_SUPABASE_ANON_KEY && window.supabase && window.supabase.createClient);
    if(!configured){
      setChip('#auth-chip','Nema Supabase configa','bad');
      markScore('bad','Nema konfiguracije','Supabase config ili JS client nije učitan.');
      renderChecks(); renderSnapshot(null, localManifest, {}); return;
    }
    const sb = (window.SOVAuth && window.SOVAuth.getClient) ? window.SOVAuth.getClient() : window.supabase.createClient(window.SOV_SUPABASE_URL, window.SOV_SUPABASE_ANON_KEY);
    let session = null, profile = null;
    const sessionCheck = await measure('Auth session', async () => {
      const {data,error} = await sb.auth.getSession();
      if(error) throw error;
      if(!data.session) throw new Error('Nema aktivne prijave');
      session = data.session;
      return data.session.user.email;
    }, true);
    if(!sessionCheck.ok){
      setChip('#auth-chip','Nije prijavljen','bad');
      markScore('bad','Prijava potrebna','Otvori login i ponovno provjeri status.');
      $('#login-card')?.classList.remove('sov-hidden');
      renderChecks(); renderSnapshot(null, localManifest, {}); return;
    }
    setChip('#auth-chip','Prijavljen','ok');
    const profileCheck = await measure('Profil i rola', async () => {
      if(window.SOVAuth && window.SOVAuth.getProfile){ return await window.SOVAuth.getProfile(true); }
      const {data,error} = await sb.from('profiles').select('*').eq('id', session.user.id).maybeSingle();
      if(error) throw error;
      return data;
    }, true);
    profile = profileCheck.data || {email:session.user.email, role:'user'};
    const role = String(profile.role || '').toLowerCase();
    const isAdmin = ['admin','webmaster'].includes(role) || String(session.user.email||'').toLowerCase() === 'darko.jeras@gmail.com';
    if(!isAdmin){
      setChip('#auth-chip','Nema ovlasti','bad');
      markScore('bad','Nema ovlasti','Status sustava je samo za Admin/Webmaster.');
      $('#denied-card')?.classList.remove('sov-hidden');
      renderChecks(); renderSnapshot(profile, localManifest, {}); return;
    }
    const live = {};
    live.manifest = (await selectOne(sb,'sov_ecosystem_manifest_current','Live ecosystem manifest',true)).data;
    live.catalogManifest = (await selectOne(sb,'sov_equipment_catalog_manifest','Oružarstvo katalog manifest',true)).data;
    live.catalogCount = await countRows(sb,'sov_equipment_app_catalog_grouped','Katalog opreme count',true);
    live.itemsCount = await countRows(sb,'equipment_items','Equipment items count');
    live.requestsCount = await countRows(sb,'equipment_requests','Zahtjevi opreme count');
    live.pendingRequests = await measure('Pending zahtjevi opreme', async () => {
      const {count,error} = await sb.from('equipment_requests').select('*', {count:'exact',head:true}).eq('status','pending');
      if(error) throw error; return count || 0;
    });
    live.loansCount = await countRows(sb,'equipment_loans','Aktivne posudbe count');
    live.loanItemsCount = await countRows(sb,'equipment_loan_items','Stavke posudbi count');
    live.profilesCount = await countRows(sb,'profiles','Korisnici/profili count');
    live.pendingProfiles = await measure('Korisnici čekaju odobrenje', async () => {
      const {count,error} = await sb.from('profiles').select('*',{count:'exact',head:true}).eq('status','pending');
      if(error) throw error; return count || 0;
    });
    live.hardeningRows = await countRows(sb,'sov_security_hardening_log','Security hardening log rows');
    // Browser-safe status: detailed RLS/open-policy audit needs server-side RPC, so this field stays informational.
    live.tablesWithoutRls = {ok:true,data:null};
    live.openPolicies = {ok:true,data:null};
    renderChecks();
    renderSnapshot(profile, localManifest, live);
    setText('#last-refresh', new Date().toLocaleString('hr-HR'));
    setChip('#db-chip','Supabase OK','ok');
  }
  document.addEventListener('DOMContentLoaded', init);
})();
