(function(){
  'use strict';
  const state = {mode:'loading', items:[], stats:[], types:[], dbReady:false, signedUrlCache:new Map(), sourceLabel:'učitavanje'};
  const monthNames = ['Ostalo','Siječanj','Veljača','Ožujak','Travanj','Svibanj','Lipanj','Srpanj','Kolovoz','Rujan','Listopad','Studeni','Prosinac'];
  const q = document.getElementById('q');
  const yearFilter = document.getElementById('yearFilter');
  const typeFilter = document.getElementById('typeFilter');
  const resetBtn = document.getElementById('resetBtn');
  const content = document.getElementById('archiveContent');
  const statsGrid = document.getElementById('statsGrid');
  const yearGrid = document.getElementById('yearGrid');
  const sourcePill = document.getElementById('sourcePill');
  const dbNotice = document.getElementById('dbNotice');
  function esc(value){return String(value == null ? '' : value).replace(/[&<>"']/g, function(ch){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch];});}
  function norm(value){return String(value || '').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');}
  function formatBytes(bytes){
    const n = Number(bytes || 0);
    if(n >= 1073741824) return (n/1073741824).toFixed(1).replace('.0','') + ' GB';
    if(n >= 1048576) return (n/1048576).toFixed(1).replace('.0','') + ' MB';
    if(n >= 1024) return Math.round(n/1024) + ' KB';
    return n + ' B';
  }
  function extensionFrom(name){
    const m = String(name || '').match(/\.([a-z0-9]+)$/i);
    return m ? m[1].toUpperCase() : 'FILE';
  }
  function normalizeStaticItem(item, packLabel){
    const year = Number(item.year || (item.date ? String(item.date).slice(0,4) : 0));
    const month = Number(item.monthNumber || (item.date ? String(item.date).slice(5,7) : 0) || 0);
    const day = Number(item.day || (item.date ? String(item.date).slice(8,10) : 0) || 0);
    const format = item.format || extensionFrom(item.filename || item.originalFilename || item.path);
    return {
      id:'static-'+norm([packLabel,item.path,item.title].join('-')).replace(/[^a-z0-9]+/g,'-'),
      source:'static',
      title:item.title || item.originalFilename || 'Dokument',
      document_type:item.type || 'zapisnik sastanka',
      document_date:item.date || null,
      year, month, day,
      original_filename:item.originalFilename || item.filename || '',
      path:item.path,
      format,
      size_bytes:Number(item.sizeBytes || 0),
      size_label:item.sizeLabel || formatBytes(item.sizeBytes),
      source_batch:packLabel,
      summary:item.excerpt || '',
      storage_bucket:null,
      storage_path:null
    };
  }
  function normalizeDbItem(item){
    return {
      id:item.id,
      source:'db',
      title:item.title || item.original_filename || 'Dokument',
      document_type:item.document_type || 'zapisnik sastanka',
      document_date:item.document_date || null,
      year:Number(item.year || 0),
      month:Number(item.month || 0),
      day:Number(item.day || 0),
      original_filename:item.original_filename || '',
      path:null,
      format:item.format || extensionFrom(item.original_filename || item.storage_path),
      size_bytes:Number(item.size_bytes || 0),
      size_label:item.size_label || formatBytes(item.size_bytes),
      source_batch:item.source_batch || 'SQL arhiva',
      summary:item.summary || '',
      storage_bucket:item.storage_bucket || 'sov-documents',
      storage_path:item.storage_path || ''
    };
  }
  function hay(item){return norm([item.title,item.original_filename,item.summary,item.year,item.document_type,item.format,item.source_batch].join(' '));}
  function localFiltered(){
    const needle = norm(q.value.trim());
    const yf = yearFilter.value;
    const tf = typeFilter.value;
    return state.items.filter(item => {
      if(yf !== 'all' && String(item.year) !== yf) return false;
      if(tf !== 'all' && item.document_type !== tf) return false;
      if(needle && !hay(item).includes(needle)) return false;
      return true;
    });
  }
  function calcStats(items){
    const years = new Map(); let totalBytes = 0; const types = new Map();
    items.forEach(item => {
      totalBytes += Number(item.size_bytes || 0);
      const y = String(item.year || ''); if(y){ years.set(y, (years.get(y)||0)+1); }
      const t = item.document_type || 'dokument'; types.set(t, (types.get(t)||0)+1);
    });
    return {count:items.length,totalBytes,totalSizeLabel:formatBytes(totalBytes),yearCount:years.size,types};
  }
  function setSource(label, cls){
    state.sourceLabel = label;
    if(sourcePill){ sourcePill.textContent = label; sourcePill.className = 'source-pill ' + (cls || ''); }
  }
  function renderStats(items){
    const s = calcStats(items);
    statsGrid.innerHTML = `
      <div class="stat-card"><strong>${s.count}</strong><span>dokumenata prikazano</span></div>
      <div class="stat-card"><strong>${s.yearCount}</strong><span>godina</span></div>
      <div class="stat-card"><strong>${esc(s.totalSizeLabel)}</strong><span>poznata veličina</span></div>
      <div class="stat-card"><strong>${esc(state.mode === 'db' ? 'SQL' : 'Preview')}</strong><span>izvor podataka</span></div>`;
  }
  function renderFilters(items){
    const years = Array.from(new Set(items.map(i=>i.year).filter(Boolean))).sort((a,b)=>b-a);
    const selectedYear = yearFilter.value;
    yearFilter.innerHTML = '<option value="all">Sve godine</option>' + years.map(y=>`<option value="${esc(y)}">${esc(y)}</option>`).join('');
    if(years.map(String).includes(selectedYear)) yearFilter.value = selectedYear;
    const types = Array.from(new Set(items.map(i=>i.document_type || 'dokument'))).sort((a,b)=>a.localeCompare(b,'hr'));
    const selectedType = typeFilter.value;
    typeFilter.innerHTML = '<option value="all">Svi tipovi</option>' + types.map(t=>`<option value="${esc(t)}">${esc(t)}</option>`).join('');
    if(types.includes(selectedType)) typeFilter.value = selectedType;
    yearGrid.innerHTML = years.map(y=>{
      const c = items.filter(i=>i.year===y).length;
      return `<button class="year-pill" type="button" data-yearjump="${esc(y)}"><strong>${esc(y)}</strong><span>${c} dok.</span></button>`;
    }).join('');
    yearGrid.querySelectorAll('[data-yearjump]').forEach(btn=>{
      btn.addEventListener('click',()=>{ yearFilter.value=btn.dataset.yearjump; render(); window.scrollTo({top:document.querySelector('.toolbar').offsetTop-16,behavior:'smooth'}); });
    });
  }
  function renderItem(item){
    const summary = item.summary ? `<p>${esc(item.summary).slice(0,260)}${item.summary.length>260?'…':''}</p>` : '<p class="muted">Nema sažetka. Dokument se može otvoriti ili preuzeti.</p>';
    const primary = item.source === 'db'
      ? `<button class="primary" type="button" data-open-db="${esc(item.id)}">Otvori</button><button type="button" data-download-db="${esc(item.id)}">Preuzmi</button>`
      : `<a class="primary" href="${esc(item.path)}" target="_blank" rel="noopener">Otvori</a><a href="${esc(item.path)}" download>Preuzmi</a>`;
    return `<article class="minute-card" data-doc-id="${esc(item.id)}">
      <div class="minute-main">
        <div class="file-badge">${esc(item.format || 'FILE')}</div>
        <div>
          <h4>${esc(item.title)}</h4>
          ${summary}
          <div class="minute-meta"><span>${esc(item.document_date || item.year || '')}</span><span>${esc(monthNames[item.month] || 'Ostalo')}</span><span>${esc(item.size_label || '')}</span><span>${esc(item.document_type || 'dokument')}</span><span>${esc(item.source_batch || '')}</span></div>
        </div>
      </div>
      <div class="minute-actions">${primary}</div>
    </article>`;
  }
  function render(){
    const items = localFiltered();
    renderStats(items);
    if(!items.length){ content.innerHTML = '<div class="empty">Nema rezultata za ovaj filter/pretragu.</div>'; return; }
    const byYear = new Map();
    items.forEach(item => { const y = String(item.year || 'Ostalo'); if(!byYear.has(y)) byYear.set(y, []); byYear.get(y).push(item); });
    const years = Array.from(byYear.keys()).sort((a,b)=>Number(b)-Number(a));
    content.innerHTML = years.map(year => {
      const yearItems = byYear.get(year);
      const byMonth = new Map();
      yearItems.forEach(item => { const m=Number(item.month || 0); if(!byMonth.has(m)) byMonth.set(m, []); byMonth.get(m).push(item); });
      const months = Array.from(byMonth.keys()).sort((a,b)=>b-a);
      const monthHtml = months.map(m=>{
        const arr = byMonth.get(m).sort((a,b)=>String(b.document_date||'').localeCompare(String(a.document_date||'')) || a.title.localeCompare(b.title,'hr'));
        return `<section class="month-block"><div class="month-head"><h3>${esc(monthNames[m] || 'Ostalo')}</h3><span>${arr.length} dok.</span></div><div class="minutes-list">${arr.map(renderItem).join('')}</div></section>`;
      }).join('');
      const bytes = yearItems.reduce((sum,i)=>sum+Number(i.size_bytes||0),0);
      return `<section class="year-block" id="godina-${esc(year)}"><div class="year-head"><div><h2>${esc(year)}</h2><p>${yearItems.length} dokumenata u prikazu.</p></div><div class="year-meta"><span class="chip good">${esc(formatBytes(bytes))}</span><span class="chip">${esc(state.sourceLabel)}</span></div></div>${monthHtml}</section>`;
    }).join('');
    bindDbButtons();
  }
  async function signedUrl(item, download){
    if(!item || item.source !== 'db') return null;
    const key = item.id + ':' + (download?'download':'open');
    const cached = state.signedUrlCache.get(key);
    if(cached && cached.expires > Date.now()) return cached.url;
    const sb = window.SOVAuth && SOVAuth.getClient ? SOVAuth.getClient() : null;
    if(!sb) throw new Error('Supabase nije konfiguriran.');
    const opts = download ? {download:item.original_filename || item.title || true} : undefined;
    const {data,error} = await sb.storage.from(item.storage_bucket || 'sov-documents').createSignedUrl(item.storage_path, 3600, opts);
    if(error) throw error;
    state.signedUrlCache.set(key,{url:data.signedUrl,expires:Date.now()+3300*1000});
    return data.signedUrl;
  }
  function bindDbButtons(){
    content.querySelectorAll('[data-open-db],[data-download-db]').forEach(btn=>{
      btn.addEventListener('click', async()=>{
        const id = btn.getAttribute('data-open-db') || btn.getAttribute('data-download-db');
        const item = state.items.find(i=>String(i.id)===String(id));
        const downloading = btn.hasAttribute('data-download-db');
        const original = btn.textContent;
        btn.textContent = 'Pripremam…'; btn.disabled = true;
        try{
          const url = await signedUrl(item, downloading);
          window.open(url, '_blank', 'noopener');
        }catch(err){ alert('Ne mogu otvoriti dokument: '+(err.message || err)); }
        finally{ btn.textContent = original; btn.disabled = false; }
      });
    });
  }
  async function loadStaticPreview(){
    const packs = [
      ['Aktualni 2026','data/zapisnici-2026.json'],
      ['Arhiva 2017-2022','data/zapisnici-2017-2022.json']
    ];
    const items = [];
    for(const [label,url] of packs){
      try{
        const res = await fetch(url,{cache:'no-store'});
        if(!res.ok) throw new Error('HTTP '+res.status);
        const data = await res.json();
        (data.items || []).forEach(item => items.push(normalizeStaticItem(item,label)));
      }catch(err){ console.warn('Static pack not loaded', url, err); }
    }
    state.mode = 'static'; state.items = items; setSource('statički preview', 'preview');
    if(dbNotice) dbNotice.innerHTML = '<strong>Preview mode:</strong> SQL arhiva još nije popunjena ili SQL patch nije pokrenut. Prikazujem samo već integrirane pakete 2017–2022 i 2026. Kad se Supabase baza napuni, ova stranica automatski prelazi na SQL/Storage prikaz.';
    renderFilters(state.items); render();
  }
  async function loadDb(){
    await SOVAuth.ready();
    const sb = SOVAuth.getClient();
    if(!sb) throw new Error('Supabase nije konfiguriran.');
    const {data:stats,error:statsError} = await sb.from('sov_minutes_archive_year_stats').select('*').order('year',{ascending:false});
    if(statsError) throw statsError;
    const {data,error} = await sb.rpc('sov_search_minutes_archive',{p_query:'',p_year:null,p_type:'all',p_limit:500,p_offset:0});
    if(error) throw error;
    const dbItems = (data || []).map(normalizeDbItem);
    if(!dbItems.length) throw new Error('SQL arhiva je dostupna, ali još nema dokumenata.');
    state.mode = 'db'; state.dbReady = true; state.items = dbItems; state.stats = stats || []; setSource('SQL + Storage', 'db');
    if(dbNotice) dbNotice.innerHTML = '<strong>DB mode:</strong> podaci dolaze iz Supabase SQL indeksa, a datoteke iz privatnog Storage bucketa preko signed URL-a. Web ZIP ostaje lagan.';
    renderFilters(state.items); render();
  }
  let dbSearchTimer = null;
  async function refreshDbSearch(){
    if(state.mode !== 'db') { render(); return; }
    clearTimeout(dbSearchTimer);
    dbSearchTimer = setTimeout(async()=>{
      const sb = SOVAuth.getClient();
      const yf = yearFilter.value === 'all' ? null : Number(yearFilter.value);
      const tf = typeFilter.value === 'all' ? 'all' : typeFilter.value;
      const {data,error} = await sb.rpc('sov_search_minutes_archive',{p_query:q.value.trim(),p_year:yf,p_type:tf,p_limit:500,p_offset:0});
      if(error){ console.warn(error); render(); return; }
      state.items = (data || []).map(normalizeDbItem);
      render();
    }, 250);
  }
  function boot(){
    content.innerHTML = '<div class="loading"><span></span><strong>Učitavam arhivu…</strong><em>Prvo provjeravam SQL/Storage, zatim fallback preview.</em></div>';
    statsGrid.innerHTML = '<div class="stat-card"><strong>…</strong><span>učitavanje</span></div>';
    loadDb().catch(err => { console.warn('DB archive unavailable, fallback to static preview:', err); loadStaticPreview(); });
    [q,yearFilter,typeFilter].forEach(el=>el.addEventListener('input', refreshDbSearch));
    resetBtn.addEventListener('click',()=>{q.value=''; yearFilter.value='all'; typeFilter.value='all'; if(state.mode==='db') refreshDbSearch(); else render();});
  }
  boot();
})();
