(function(){
  const cfg = window.SOV_SUPABASE_CONFIG || window.SUPABASE_CONFIG || {};
  const url = window.SOV_SUPABASE_URL || cfg.url || cfg.SUPABASE_URL;
  const key = window.SOV_SUPABASE_ANON_KEY || cfg.anonKey || cfg.SUPABASE_ANON_KEY;
  const sb = (window.supabase && url && key) ? window.supabase.createClient(url,key,{auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}}) : null;
  const $ = id => document.getElementById(id);
  const status=$('status'), list=$('list'), dialog=$('editorDialog');
  let rows=[];
  const previewRole = (()=>{try{return localStorage.getItem('SOV_PREVIEW_ROLE')||'admin'}catch(e){return 'admin'}})();
  const isAdmin = previewRole === 'admin';
  const STATIC_SEED = [
    {title:'Speleološka ekspedicija Sjeverni Velebit 2026',summary:'SO PDS Velebit poziva na ekspediciju na području Hajdučkih i Rožanskih kukova od 25.7. do 9.8.2026.',body:'Ciljevi ekspedicije su rekognosciranje terena, prikupljanje podataka o poznatim objektima, monitoring leda i nastavak istraživanja u jami Nedam. Prijave su otvorene do 20.7.',image_url:'assets/ekspedicija-sjeverni-velebit-2026-banner.png',pdf_url:'assets/news/ekspedicija-sov-2026.pdf',cta_label:'Prijavi se',cta_url:'https://docs.google.com/forms/d/e/1FAIpQLSfhDMbQJi0Nb6xykRwlYlBng_Hw_BsLga1HMO3Ao-Y1fr3fjg/viewform',published:true,pinned:true,published_at:new Date().toISOString()},
    {title:'Sve što je lijepo kratko traje, osim puta do Velebitaškog duha',summary:'Priča iz speleoškole i puta prema Velebitaškom duhu.',image_url:'https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/05/demonstracija-tehnickog-penjanja_gorana-peric.jpg?fit=1200%2C676&ssl=1',cta_label:'Otvori',cta_url:'novosti/sve-sto-je-lijepo-kratko-traje-osim-puta-do-velebitaskog-duha.html',published:true,pinned:false,published_at:'2026-05-07T10:00:00Z'},
    {title:'Pa po užetu dol’ pa po užetu gor’!',summary:'Školarci se spuštaju u jamu i vraćaju po užetu.',image_url:'https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2026/05/jutarnje-zagrijavanje_gorana-peric.jpg?fit=1200%2C676&ssl=1',cta_label:'Otvori',cta_url:'novosti/pa-po-uzetu-dol-pa-po-uzetu-gor.html',published:true,pinned:false,published_at:'2026-05-05T10:00:00Z'},
    {title:'56. Zagrebačka speleoškola',summary:'Prijave i informacije za speleoškolu.',image_url:'https://i0.wp.com/sovelebit.wordpress.com/wp-content/uploads/2025/04/ponorac-na-stijeni-mikic-t.jpg?fit=1200%2C800&ssl=1',cta_label:'Otvori',cta_url:'novosti/56-zagrebacka-speleoskola.html',published:true,pinned:false,published_at:'2026-01-20T10:00:00Z'}
  ];
  function msg(t){ status.textContent=t||''; }
  function esc(s){return String(s||'').replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
  function dtLocal(v){ if(!v) return ''; const d=new Date(v); if(Number.isNaN(d.getTime())) return ''; d.setMinutes(d.getMinutes()-d.getTimezoneOffset()); return d.toISOString().slice(0,16); }
  function dateText(v){ const d=new Date(v||Date.now()); return Number.isNaN(d.getTime())?'bez datuma':d.toLocaleString('hr-HR'); }
  function norm(s){return String(s||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');}
  async function load(){
    if(!sb){ msg('Supabase nije konfiguriran.'); return; }
    msg('Učitavam postojeće vijesti...');
    const {data,error}=await sb.from('sov_news').select('*').order('pinned',{ascending:false}).order('published_at',{ascending:false}).order('created_at',{ascending:false});
    if(error){ msg('Greška učitavanja vijesti: '+error.message); list.innerHTML='<div class="empty">Ne mogu učitati postojeće vijesti. Provjeri SQL/RLS za sov_news.</div>'; return; }
    rows=data||[]; render(); msg(rows.length ? `Učitano ${rows.length} vijesti.` : 'Nema vijesti u bazi. Klikni + Nova vijest ili Uvezi statičke vijesti.');
  }
  function render(){
    const q=norm($('search').value); const f=$('filter').value;
    let view=rows.filter(n=>{
      if(f==='published' && !n.published) return false;
      if(f==='hidden' && n.published) return false;
      if(f==='pinned' && !n.pinned) return false;
      if(q && !norm([n.title,n.summary,n.body,n.cta_url,n.pdf_url].join(' ')).includes(q)) return false;
      return true;
    });
    list.innerHTML = view.map(card).join('') || '<div class="empty">Nema rezultata.</div>';
    list.querySelectorAll('[data-edit]').forEach(b=>b.onclick=()=>openEdit(rows.find(x=>String(x.id)===String(b.dataset.edit))));
    list.querySelectorAll('[data-toggle]').forEach(b=>b.onclick=()=>togglePublish(rows.find(x=>String(x.id)===String(b.dataset.toggle))));
    list.querySelectorAll('[data-dup]').forEach(b=>b.onclick=()=>duplicate(rows.find(x=>String(x.id)===String(b.dataset.dup))));
    list.querySelectorAll('[data-delete]').forEach(b=>b.onclick=()=>remove(rows.find(x=>String(x.id)===String(b.dataset.delete))));
  }
  function card(n){
    const img=n.image_url||'';
    return `<article class="news-admin-card"><div class="thumb" style="background-image:url('${esc(img)}')"></div><div><h3>${n.pinned?'📌 ':''}${esc(n.title||'Bez naslova')}</h3><p>${esc(n.summary||n.body||'')}</p><div class="meta">${n.published?'Objavljeno':'Skriveno'} · ${dateText(n.published_at||n.created_at)} · ID ${esc(n.id)}</div></div><div class="card-buttons"><button class="btn" data-edit="${esc(n.id)}">Uredi</button><button class="btn secondary" data-toggle="${esc(n.id)}">${n.published?'Sakrij':'Objavi'}</button><button class="btn ghost" data-dup="${esc(n.id)}">Dupliciraj</button>${isAdmin?`<button class="btn danger" data-delete="${esc(n.id)}">Obriši</button>`:''}</div></article>`;
  }
  function openEdit(n){
    $('modalTitle').textContent=n?'Uredi vijest':'Nova vijest';
    $('id').value=n?.id||''; $('title').value=n?.title||''; $('summary').value=n?.summary||''; $('body').value=n?.body||''; $('image_url').value=n?.image_url||''; $('pdf_url').value=n?.pdf_url||''; $('cta_label').value=n?.cta_label||''; $('cta_url').value=n?.cta_url||'';
    $('published').checked=n ? !!n.published : true; $('pinned').checked=n ? !!n.pinned : false; $('published_at').value=dtLocal(n?.published_at||new Date().toISOString()); previewImage();
    if(dialog.showModal) dialog.showModal(); else alert('Browser ne podržava modal; probaj Chrome/Edge.');
  }
  function rowFromForm(){ return {title:$('title').value.trim(),summary:$('summary').value.trim(),body:$('body').value.trim(),image_url:$('image_url').value.trim(),pdf_url:$('pdf_url').value.trim(),cta_label:$('cta_label').value.trim(),cta_url:$('cta_url').value.trim(),published:$('published').checked,pinned:$('pinned').checked,published_at:$('published_at').value?new Date($('published_at').value).toISOString():new Date().toISOString()}; }
  async function save(ev){
    ev.preventDefault(); if(!sb) return msg('Supabase nije konfiguriran.');
    const id=$('id').value; const row=rowFromForm(); if(!row.title) return msg('Naslov je obavezan.');
    msg(id?'Spremam postojeću vijest...':'Spremam novu vijest...');
    let res = id ? await sb.from('sov_news').update(row).eq('id',id).select().single() : await sb.from('sov_news').insert(row).select().single();
    if(res.error){ msg('Greška spremanja: '+res.error.message); return; }
    dialog.close(); await load(); msg('Spremljeno.');
  }
  async function togglePublish(n){ if(!n) return; msg('Mijenjam status...'); const {error}=await sb.from('sov_news').update({published:!n.published}).eq('id',n.id); if(error) return msg('Greška: '+error.message); await load(); }
  async function duplicate(n){ if(!n) return; const copy={...n,title:(n.title||'Vijest')+' — kopija'}; delete copy.id; delete copy.created_at; delete copy.updated_at; msg('Dupliciram...'); const {error}=await sb.from('sov_news').insert(copy); if(error) return msg('Greška: '+error.message); await load(); }
  async function remove(n){ if(!n) return; if(!confirm('Obrisati vijest: '+(n.title||'')+'?')) return; msg('Brišem...'); const {error}=await sb.from('sov_news').delete().eq('id',n.id); if(error) return msg('Greška brisanja: '+error.message); await load(); }
  async function importStatic(){
    if(!confirm('Uvesti osnovne statičke vijesti u bazu ako nedostaju?')) return;
    msg('Uvozim statičke vijesti...'); let added=0;
    for(const n of STATIC_SEED){
      const exists = rows.some(r=>norm(r.title)===norm(n.title));
      if(exists) continue;
      const {error}=await sb.from('sov_news').insert(n); if(error){ msg('Greška uvoza: '+error.message); return; } added++;
    }
    await load(); msg(added ? `Uvezeno ${added} vijesti.` : 'Nema novih za uvoz.');
  }
  function previewImage(){ const v=$('image_url').value.trim(); $('imagePreviewWrap').innerHTML=v?`<img class="preview-img" src="${esc(v)}" alt="preview">`:''; }
  $('newBtn').onclick=()=>openEdit(null); $('refreshBtn').onclick=load; $('saveBtn').onclick=save; $('search').oninput=render; $('filter').onchange=render; $('image_url').oninput=previewImage; $('importStaticBtn').onclick=importStatic;
  document.addEventListener('DOMContentLoaded',load); load();
})();
