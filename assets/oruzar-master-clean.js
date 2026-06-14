(function(){
  const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));
  const norm=s=>String(s||'').trim();
  const strip=s=>norm(s).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
  function countInt(v, fallback=0){
    if(v===null||v===undefined||v==='') v=fallback;
    let raw=String(v).trim().replace(',','.');
    if(/^\d{1,2}[./]\d{4}$/.test(raw)||/^\d{1,2}[./]\d{1,2}[./]\d{2,4}$/.test(raw)) raw=String(fallback??0);
    let n=Number(raw); if(!Number.isFinite(n)) n=Number(fallback); if(!Number.isFinite(n)) return 0;
    return Math.max(0,Math.round(n));
  }
  function statusKey(s){
    const x=strip(s||'pending');
    if(['requested','pending','novo','zatrazeno','za izdati','za_izdati','approved','prepared','reserved'].some(k=>x.includes(k))) return 'requested';
    if(['issued','izdano','vani'].some(k=>x.includes(k))) return 'issued';
    if(['partial_return','partial','djelomicno','djelomicni'].some(k=>x.includes(k))) return 'partial_return';
    if(['returned','vraceno','vraćeno','closed'].some(k=>x.includes(k))) return 'returned';
    if(['cancelled','canceled','rejected','otkazano','odbijeno'].some(k=>x.includes(k))) return 'cancelled';
    return x||'requested';
  }
  function statusLabel(s){return ({requested:'za izdati',issued:'izdano vani',partial_return:'djelomično vraćeno',returned:'vraćeno',cancelled:'zatvoreno'})[statusKey(s)]||String(s||'za izdati')}
  function statusBadge(s){const k=statusKey(s); if(k==='issued'||k==='returned') return 'ok'; if(k==='requested'||k==='partial_return') return 'warn'; return 'bad'}
  function toast(m){let t=document.getElementById('cmToast'); if(!t){t=document.createElement('div');t.id='cmToast';t.className='cm-toast';document.body.appendChild(t)} t.textContent=m;t.classList.add('show');clearTimeout(t._to);t._to=setTimeout(()=>t.classList.remove('show'),2300)}
  // v6.1.39a: category/subcategory display naming layer; raw DB names remain stable.
  function categoryName(row,type){
    const raw=norm(row.category_name||row.main_category||row.category||row.xls_category||row.raw_category||(type==='rope'?'Užad':'Ostalo'))||'Ostalo';
    return raw==='Užeta' ? 'Užad' : raw;
  }
  function subcategoryName(row){return norm(row.subcategory||row.raw_subcategory||row.group||row.display_subcategory||'Ostalo')||'Ostalo'}
  function displayName(row,type){return norm(row.display_name||row.name||row.item_name||row.model||row.sku||(type==='rope'?'Uže':'Artikl'))||'Artikl'}
  function qtext(row){return strip([row.search_text,row.display_name,row.name,row.item_name,row.category_name,row.category,row.main_category,row.subcategory,row.sku,row.model,row.manufacturer,row.internal_note,row.note].join(' '))}
  // UI FIX v6.1.35: tolerantna pretraga za inventuru/inventar (tipfeleri, dijakritika, djelomični naziv)
  function wordsOf(s){return strip(s).split(/[^a-z0-9]+/).filter(w=>w.length>1)}
  function levLimited(a,b,limit){
    a=String(a||''); b=String(b||'');
    if(a===b)return 0; if(Math.abs(a.length-b.length)>limit)return limit+1;
    const prev=new Array(b.length+1); const cur=new Array(b.length+1);
    for(let j=0;j<=b.length;j++)prev[j]=j;
    for(let i=1;i<=a.length;i++){
      cur[0]=i; let best=cur[0];
      for(let j=1;j<=b.length;j++){
        const cost=a.charCodeAt(i-1)===b.charCodeAt(j-1)?0:1;
        cur[j]=Math.min(prev[j]+1,cur[j-1]+1,prev[j-1]+cost);
        if(cur[j]<best)best=cur[j];
      }
      if(best>limit)return limit+1;
      for(let j=0;j<=b.length;j++)prev[j]=cur[j];
    }
    return prev[b.length];
  }
  function fuzzyToken(q,w){
    if(!q||!w)return false;
    if(w.includes(q)||q.includes(w))return true;
    if(q.length<3)return false;
    const limit=q.length<=4?1:(q.length<=8?2:3);
    if(Math.abs(q.length-w.length)>limit)return false;
    return levLimited(q,w,limit)<=limit;
  }
  function rowMatchesQuery(row,query){
    const q=strip(query); if(!q)return true;
    const parts=wordsOf(q); if(!parts.length)return true;
    const hay=row.search||''; if(hay.includes(q))return true;
    const words=wordsOf(hay);
    return parts.every(part=>hay.includes(part)||words.some(w=>fuzzyToken(part,w)));
  }
  function iconFor(t){
    t=strip(t);
    if(t.includes('srt')||t.includes('osobni'))return '🧗';
    if(t.includes('uzad')||t.includes('uze')||t.includes('rope'))return '🪢';
    if(t.includes('sidrist')||t.includes('sidri')||t.includes('opremanj')||t.includes('spit')||t.includes('fix')||t.includes('ploc'))return '⚓';
    if(t.includes('spas')||t.includes('cisto')||t.includes('paw')||t.includes('kolot')||t.includes('nosil'))return '🛟';
    if(t.includes('prosir')||t.includes('busil')||t.includes('svrd')||t.includes('regul'))return '⛏️';
    if(t.includes('mjer')||t.includes('crtan')||t.includes('dokument')||t.includes('topofil')||t.includes('kompas')||t.includes('busol'))return '📐';
    if(t.includes('rasvjet')||t.includes('elektr')||t.includes('komunik')||t.includes('foto')||t.includes('dron'))return '🔦';
    if(t.includes('logor')||t.includes('ekspedic')||t.includes('kuhinj')||t.includes('kamp'))return '⛺';
    if(t.includes('medic')||t.includes('prva pomoc'))return '🧰';
    if(t.includes('alpin')||t.includes('penjac'))return '⛰️';
    if(t.includes('ronil'))return '🤿';
    if(t.includes('alat')||t.includes('odrzav')||t.includes('radion'))return '🧰';
    return '📦'
  }

  function injectCategoryMetaCss(){
    if(document.getElementById('armory-category-meta-v6138-css'))return;
    const css=document.createElement('style'); css.id='armory-category-meta-v6138-css';
    css.textContent=`.cat-meta-shell{border:1px solid rgba(220,255,235,.14);border-radius:24px;background:linear-gradient(135deg,rgba(215,246,111,.08),rgba(255,255,255,.035));padding:14px;margin:0 0 14px;display:grid;gap:10px}.cat-meta-top{display:flex;justify-content:space-between;gap:12px;align-items:flex-start}.cat-meta-title{display:flex;gap:12px;align-items:center}.cat-meta-bigico{width:54px;height:54px;border-radius:18px;display:grid;place-items:center;border:1px solid rgba(220,255,235,.16);background:rgba(0,0,0,.18);font-size:31px}.cat-meta-title h3{margin:0;font-size:22px;letter-spacing:-.03em}.cat-meta-title p{margin:3px 0 0;color:#9fb3ad}.cat-meta-form{display:grid;grid-template-columns:86px minmax(180px,1fr) auto;gap:9px;align-items:start}.cat-icon-input{min-width:0!important;width:86px!important;text-align:center!important;font-size:28px!important;line-height:1!important}.cat-note-input{min-height:58px!important;resize:vertical!important;font-family:inherit!important}.cat-note-preview{display:block!important;margin-top:8px!important;color:#d8e9e3!important;border-top:1px solid rgba(255,255,255,.08);padding-top:8px;white-space:pre-wrap}.cat-tile-note{margin-top:9px!important;padding:8px 10px!important;border:1px solid rgba(215,246,111,.18);border-radius:14px;background:rgba(215,246,111,.055);color:#dbeee6!important;display:-webkit-box!important;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.cat-tile-clean .cat-tile-edit-hint{font-size:11px!important;color:#d7f66f!important;margin-top:9px!important}@media(max-width:720px){.cat-meta-top{display:grid}.cat-meta-form{grid-template-columns:1fr}.cat-icon-input{width:100%!important}.cat-meta-bigico{width:48px;height:48px;font-size:28px}}.cat-tile-clean{display:flex!important;flex-direction:column!important;gap:8px!important;min-height:172px!important}.cat-tile-clean .ico{width:56px!important;height:56px!important;min-width:56px!important;border-radius:18px!important;display:grid!important;place-items:center!important;font-size:30px!important;line-height:1!important;margin:0 0 6px!important;background:rgba(0,0,0,.18)!important;border:1px solid rgba(255,255,255,.13)!important;overflow:hidden!important}.cat-tile-clean b{font-size:clamp(17px,1.4vw,20px)!important;line-height:1.12!important;word-break:normal!important;overflow-wrap:break-word!important;hyphens:auto!important;display:block!important;max-width:100%!important}.cat-tile-clean small{font-size:12px!important;line-height:1.28!important}.cat-meta-bigico{flex:0 0 auto!important;overflow:hidden!important;line-height:1!important}.cat-meta-title{min-width:0!important}.cat-meta-title h3{overflow-wrap:anywhere!important;line-height:1.08!important}.cat-icon-input{height:56px!important;min-height:56px!important;padding:4px 8px!important;display:block!important;overflow:hidden!important}`;
    document.head.appendChild(css);
  }
  function categoryMeta(name){
    const key=strip(name);
    const cats=((STATE.data&&STATE.data.categories)||[]);
    return cats.find(c=>strip(c.name)===key||strip(c.display_name)===key||strip(c.short_name)===key)||{};
  }
  function displayCategoryName(name){const m=categoryMeta(name); return norm(m.display_name||m.short_name||name)||'Ostalo'}
  function displayCategoryShort(name){const m=categoryMeta(name); return norm(m.short_name||m.display_name||name)||'Ostalo'}
  function subcategoryMeta(cat,sub){
    const ck=strip(cat), sk=strip(sub);
    const rows=((STATE.data&&STATE.data.subcategory_meta)||[]);
    return rows.find(x=>strip(x.category_name)===ck && strip(x.subcategory_name)===sk)||{};
  }
  function displaySubcategoryName(cat,sub){const m=subcategoryMeta(cat,sub); return norm(m.display_name||m.short_name||sub)||'Ostalo'}
  function subcategoryIcon(cat,sub){const m=subcategoryMeta(cat,sub); return norm(m.icon)||iconFor(sub)}
  function categoryIcon(name){return norm(categoryMeta(name).icon)||iconFor(name)}
  function categoryNote(name){return norm(categoryMeta(name).note||'')}
  function categoryTile(c,rs){
    const note=categoryNote(c), label=displayCategoryName(c);
    return `<button class="cat-tile cat-tile-clean" onclick="CleanArmory.pickCat('${esc(c)}')"><span class="ico">${esc(categoryIcon(c))}</span><b>${esc(label)}</b><small>${esc(rs.length)} stavki · otvori podkategorije</small>${note?`<small class="cat-tile-note">📝 ${esc(note)}</small>`:`<small class="cat-tile-edit-hint">Klikni kategoriju za ikonicu i napomenu</small>`}</button>`;
  }
  function categoryMetaPanel(cat){
    const meta=categoryMeta(cat);
    const icon=norm(meta.icon)||iconFor(cat);
    const note=norm(meta.note||'');
    const label=displayCategoryName(cat);
    const raw=label!==cat?`<small class="muted">Interno: ${esc(cat)}</small>`:'';
    return `<section class="cat-meta-shell"><div class="cat-meta-top"><div class="cat-meta-title"><span class="cat-meta-bigico">${esc(icon)}</span><div><h3>${esc(label)}</h3>${raw}<p>Ikonica i napomena kategorije. Ostaje trajno u Supabaseu i vidi se u Inventaru i Inventuri.</p>${note?`<div class="cat-note-preview">📝 ${esc(note)}</div>`:''}</div></div></div><form class="cat-meta-form" onsubmit="CleanArmory.saveCategoryMeta(event,'${esc(cat)}')"><input class="cm-input cat-icon-input" id="catIconInput" maxlength="4" value="${esc(icon)}" title="Ikonica kategorije"><textarea class="cm-input cat-note-input" id="catNoteInput" placeholder="Napomena za ovu kategoriju: što provjeriti, gdje stoji, posebna pravila...">${esc(note)}</textarea><button class="cm-btn primary">Spremi kategoriju</button></form></section>`;
  }
  function categoryPriority(c){
    const x=strip(c), xd=strip(displayCategoryName(c));
    const order=['osobni srt komplet','osobna oprema','uzad','sidrista i opremanje','tehnicko spasavanje i cisto podzemlje','prosirivanje i regulirana oprema','mjerenje crtanje i dokumentacija','rasvjeta elektronika i komunikacija','logor ekspedicija i kuhinja','medicinska oprema','alpinisticka i penjacka oprema','ronilacka oprema','alat i odrzavanje','ostalo'];
    let exact=order.indexOf(x); if(exact<0) exact=order.indexOf(xd);
    if(exact>=0) return exact;
    const i=order.findIndex(k=>x.includes(k)||k.includes(x)||xd.includes(k)||k.includes(xd)); return i<0?999:i;
  }


  let STATE={data:null,liveData:null,rows:[],cat:null,sub:null,query:'',requests:[],reqSource:'none',snapshots:[],catalogMode:(localStorage.getItem('sov_armory_catalog_mode')||'live')};

  function currentCatalogMode(){return STATE.catalogMode||'live'}
  function selectedSnapshotId(){const m=currentCatalogMode(); return m&&m.startsWith('snapshot:')?m.slice(9):'';}
  async function loadSnapshots(){
    try{
      if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadCatalogSnapshots){
        STATE.snapshots=await SOVArmoryDB.loadCatalogSnapshots()||[];
      }
    }catch(e){console.warn('[armory v6.1.39c] snapshots failed',e); STATE.snapshots=STATE.snapshots||[];}
    return STATE.snapshots||[];
  }
  async function applyCatalogMode(liveData){
    STATE.liveData=liveData;
    await loadSnapshots();
    const sid=selectedSnapshotId();
    if(sid && window.SOVArmoryDB&&SOVArmoryDB.loadCatalogSnapshot){
      try{
        const snap=preferRawCatalogForMaster(await SOVArmoryDB.loadCatalogSnapshot(sid));
        if(snap && rowCountFromData(snap)>=20){ STATE.data=snap; STATE.source=(snap.summary&&snap.summary.source)||'Snapshot'; STATE.rows=makeRows(STATE.data); return STATE.data; }
      }catch(e){console.warn('[armory v6.1.39c] snapshot load failed',e); toast('Snapshot se nije mogao učitati; vraćam live bazu.'); STATE.catalogMode='live'; localStorage.setItem('sov_armory_catalog_mode','live');}
    }
    STATE.data=liveData; STATE.source='Supabase live · DB gate v6.1.39c'; STATE.rows=makeRows(STATE.data); return STATE.data;
  }
  function catalogSelectorPanel(){
    const snaps=STATE.snapshots||[]; const mode=currentCatalogMode();
    const opts=['<option value="live" '+(mode==='live'?'selected':'')+'>Aktivna baza / live stanje</option>'].concat(snaps.map(s=>`<option value="snapshot:${esc(s.id)}" ${mode===('snapshot:'+s.id)?'selected':''}>${esc(s.name||'Snapshot')} · ${esc((s.created_at||'').slice(0,10))} · ${esc(s.item_count||'')} stavki</option>`)).join('');
    const snap=snaps.find(s=>'snapshot:'+s.id===mode);
    const label=mode==='live'?'Trenutno radiš na aktivnoj bazi. Snapshoti su restore pointovi prije nove inventure.':`Pregledavaš snapshot: ${(snap&&snap.name)||'stara baza'}. Uredi/restore ne radi iz snapshot viewa.`;
    return `<section class="catalog-mode-panel"><div><b>📚 Baza inventara</b><p>${esc(label)}</p></div><div class="catalog-mode-tools"><select class="cm-input" onchange="CleanArmory.selectCatalogMode(this.value)">${opts}</select><button class="cm-btn" onclick="CleanArmory.createCatalogSnapshotUi()">+ Spremi novu bazu / snapshot</button><button class="cm-btn" onclick="CleanArmory.reloadCatalogMode()">Osvježi</button></div></section>`;
  }
  function injectCatalogModeCss(){
    if(document.getElementById('armory-catalog-mode-v6139c-css'))return;
    const css=document.createElement('style'); css.id='armory-catalog-mode-v6139c-css';
    css.textContent=`.catalog-mode-panel{border:1px solid rgba(141,216,255,.18);background:linear-gradient(135deg,rgba(141,216,255,.08),rgba(215,246,111,.04));border-radius:22px;padding:13px 14px;margin:0 0 14px;display:flex;justify-content:space-between;gap:12px;align-items:center}.catalog-mode-panel b{display:block;font-size:15px;color:#eef7f3}.catalog-mode-panel p{margin:3px 0 0;color:#aebfba;font-size:12px;line-height:1.35}.catalog-mode-tools{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}.catalog-mode-tools select{min-width:270px}@media(max-width:760px){.catalog-mode-panel{display:grid}.catalog-mode-tools{display:grid;grid-template-columns:1fr}.catalog-mode-tools select{min-width:0;width:100%}}`;
    document.head.appendChild(css);
  }
  async function selectCatalogMode(value){ STATE.catalogMode=value||'live'; localStorage.setItem('sov_armory_catalog_mode',STATE.catalogMode); STATE.cat=null; STATE.sub=null; STATE.data=null; await loadData(true); renderInventory(); renderMaster(); toast(STATE.catalogMode==='live'?'Aktivna baza':'Snapshot prikaz'); }
  async function reloadCatalogMode(){ STATE.data=null; await loadData(true); renderInventory(); renderMaster(); await renderLoans(); toast('Osvježeno'); }
  async function createCatalogSnapshotUi(){
    const name=(prompt('Naziv nove baze/snapshota:', 'Snapshot oružarstva '+new Date().toISOString().slice(0,10))||'').trim(); if(!name)return;
    const desc=(prompt('Opis / napomena za snapshot:', 'Prije nove inventure')||'').trim();
    try{ if(!window.SOVArmoryDB||!SOVArmoryDB.createCatalogSnapshot) throw new Error('Nema snapshot helpera'); await SOVArmoryDB.createCatalogSnapshot(name, desc); await loadSnapshots(); renderInventory(); toast('Snapshot spremljen'); }catch(e){console.warn('[armory v6.1.39c] create snapshot failed',e); toast('Greška kod spremanja snapshota'); }
  }

  async function loadStaticCatalog(){
    try{
      const res=await fetch('data/oruzarstvo-data.json',{cache:'no-store'});
      if(!res.ok) throw new Error('static json HTTP '+res.status);
      return await res.json();
    }catch(e){
      console.warn('[armory master v5.48.1] static catalog failed',e);
      return {items:[],ropes:[],pieces:[],categories:[]};
    }
  }
  function rowCountFromData(d){
    if(!d) return 0;
    return (Array.isArray(d.items)?d.items.length:0)+(Array.isArray(d.ropes)?d.ropes.length:0)+(Array.isArray(d.pieces)?d.pieces.length:0)+(Array.isArray(d.raw_app_catalog)?d.raw_app_catalog.length:0);
  }
  function preferRawCatalogForMaster(d){
    if(d && Array.isArray(d.raw_app_catalog) && d.raw_app_catalog.length){
      // Oružar Master must inspect the real/raw rows. The grouped catalog is good for members,
      // but master inventory and inventura need item-level rows.
      return {...d, items:d.raw_app_catalog, ropes:[], pieces:[]};
    }
    return d;
  }
  function applyLiveCatalog(live){
    const liveRows=rowCountFromData(live);
    if(!live || liveRows<20) return false;
    STATE.data=live; STATE.source='supabase-cache/live'; STATE.liveRows=liveRows; STATE.rows=makeRows(STATE.data);
    try{ renderKpis(); renderMaster(); renderInventory(); }catch(e){console.warn('[armory master v5.48.1] live rerender failed',e)}
    return true;
  }
  async function refreshLiveInBackground(){
    if(STATE._liveRefreshStarted) return;
    STATE._liveRefreshStarted=true;
    setTimeout(async()=>{
      try{
        if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadAllData){
          const live=preferRawCatalogForMaster(await SOVArmoryDB.loadAllData());
          applyLiveCatalog(live);
        }
      }catch(e){console.warn('[armory master v5.48.1] background live catalog failed',e)}
    },250);
  }
  function dbLoadingHtml(){
    return `<div class="cm-panel armory-db-loading"><div class="armory-db-pills"><span class="armory-db-pill warn">DB gate v6.1.6</span><span class="armory-db-pill">bez cachea · bez statike</span></div><h2>Čekam da se oružarstvo napuni iz baze…</h2><div class="armory-db-bar" aria-hidden="true"></div><p>Inventar, kategorije i inventura su sakriveni dok Supabase ne vrati stvarni XLS canonical katalog. Nema lokalnog JSON/cache prikaza i nema djelomičnih podataka.</p></div>`;
  }
  function injectDbGateCss(){
    if(document.getElementById('armory-master-db-gate-v616-css'))return;
    const css=document.createElement('style'); css.id='armory-master-db-gate-v616-css';
    css.textContent=`.armory-db-loading{border:1px solid rgba(215,246,111,.25)!important;border-radius:26px!important;padding:24px!important;background:linear-gradient(135deg,rgba(215,246,111,.10),rgba(141,216,255,.055))!important;box-shadow:0 18px 60px rgba(0,0,0,.25)!important;display:grid!important;gap:13px!important;color:#eef7f3!important}.armory-db-loading h2{margin:0!important;font-size:24px!important;letter-spacing:-.03em!important}.armory-db-loading p{margin:0!important;color:#b9cbc5!important;line-height:1.55!important}.armory-db-bar{height:11px;border-radius:999px;background:rgba(255,255,255,.10);overflow:hidden;position:relative}.armory-db-bar:before{content:"";position:absolute;inset:0;width:42%;border-radius:999px;background:linear-gradient(90deg,#d7f66f,#7ff0b2,#8dd8ff);animation:armoryDbLoad 1.25s ease-in-out infinite}.armory-db-pills{display:flex;gap:8px;flex-wrap:wrap}.armory-db-pill{border:1px solid rgba(255,255,255,.13);background:rgba(255,255,255,.055);border-radius:999px;padding:7px 10px;font-size:12px;font-weight:950;color:#dce9e4}.armory-db-pill.warn{border-color:rgba(255,211,107,.3);background:rgba(255,211,107,.1);color:#ffe7a6}@keyframes armoryDbLoad{0%{transform:translateX(-100%)}50%{transform:translateX(70%)}100%{transform:translateX(250%)}}`;
    document.head.appendChild(css);
  }
  function renderDbLoading(){
    injectDbGateCss();
    ['masterRoot','inventoryRoot'].forEach(id=>{const el=document.getElementById(id); if(el)el.innerHTML=dbLoadingHtml();});
  }
  function scheduleDbRetry(){
    if(STATE._dbRetryTimer) return;
    STATE._dbRetryTimer=setTimeout(async()=>{
      STATE._dbRetryTimer=null;
      STATE.data=null;
      await loadData(true);
      if(STATE.rows && STATE.rows.length){renderMaster(); renderInventory();}
      else scheduleDbRetry();
    },4500);
  }
  async function loadData(force=false){
    if(STATE.data && !force) return STATE.data;
    renderDbLoading();
    try{
      if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadAllData){
        const live=preferRawCatalogForMaster(await SOVArmoryDB.loadAllData({force:true,strictLive:true}));
        const liveRows=rowCountFromData(live);
        if(live && liveRows>=20){
          STATE.liveRows=liveRows; STATE.staticRows=0;
          await applyCatalogMode(live);
          return STATE.data;
        }
      }
    }catch(e){console.warn('[armory master v6.1.6] strict Supabase catalog failed',e)}
    STATE.data={items:[],ropes:[],pieces:[],categories:[]}; STATE.source='Čekam Supabase bazu'; STATE.liveRows=0; STATE.staticRows=0; STATE.rows=[];
    scheduleDbRetry();
    return STATE.data;
  }
  function makeRows(d){
    const out=[];
    (d.items||[]).forEach((r,i)=>out.push(row(r,'item',i)));
    (d.ropes||[]).forEach((r,i)=>out.push(row(r,'rope',i)));
    (d.pieces||[]).forEach((r,i)=>out.push(row(r,'piece',i)));
    return out.filter(r=>r.name).sort((a,b)=>(categoryPriority(a.category)-categoryPriority(b.category))||a.category.localeCompare(b.category,'hr')||a.subcategory.localeCompare(b.subcategory,'hr')||a.name.localeCompare(b.name,'hr'));
  }
  function row(r,type,i){
    let qty=countInt(r.total_qty ?? r.quantity ?? r.total ?? (type==='item'?0:1),0);
    let av=countInt(r.available_qty ?? r.available ?? qty, qty);
    let loan=countInt(r.loaned ?? r.loaned_qty ?? Math.max(0,qty-av),0);
    if(type==='rope'||type==='piece'){
      qty=countInt(r.quantity ?? 1,1);
      av=/posu|vani|rashod|otpis|izgubl/i.test(String(r.status||''))?0:countInt(r.available ?? 1,1);
      loan=/posu|vani/i.test(String(r.status||''))?1:Math.max(0,qty-av);
    }
    const qtyLabel=norm(r.quantity_label||r.total_qty_label||r.original_quantity_text||'');
    const avLabel=norm(r.available_label||r.available_qty_label||'');
    const unit=norm(r.unit||'kom')||'kom';
    const needsCount=/provjer|prebroj|prazno polje/i.test([r.status,r.availability,qtyLabel,avLabel,r.internal_note,r.note].join(' '));
    const cat=categoryName(r,type), sub=subcategoryName(r);
    const displaySearch=strip([displayCategoryName(cat),displaySubcategoryName(cat,sub),categoryMeta(cat).search_terms,subcategoryMeta(cat,sub).search_terms].join(' '));
    return {raw:r,type,id:String(r.app_id||r.source_id||r.legacy_id||r.catalog_id||r.sku||r.id||`${type}-${i}`),name:displayName(r,type),category:cat,rawCategory:cat,subcategory:sub,rawSubcategory:sub,qty,av,loan,qtyLabel,avLabel,unit,needsCount,lastInventoryDate:norm(r.last_inventory_date||''),sourceXlsRow:r.source_xls_row||null,location:norm(r.location_name||r.location||''),status:norm(r.status||r.availability||'aktivno'),minimum:countInt(r.minimum??r.threshold??r.min_quantity??0,0),search:[qtext(r),displaySearch].join(' '),variants:countInt(r.variant_count||1,1)};
  }
  function filtered(){
    let rows=STATE.rows;
    if(STATE.query){rows=rows.filter(r=>rowMatchesQuery(r,STATE.query));}
    if(STATE.locFilter){rows=rows.filter(r=>norm(r.location||'')===STATE.locFilter);}
    return rows;
  }
  function locList(){ return (((STATE.data&&STATE.data.locations)||[]).slice()).sort((a,b)=>String(a.name).localeCompare(String(b.name),'hr')); }
  function locFilterHtml(){
    const locs=locList(); if(!locs.length) return '';
    const cur=STATE.locFilter||'';
    const opts=['<option value="">📍 Sve lokacije</option>'].concat(locs.map(l=>`<option value="${esc(l.name)}" ${l.name===cur?'selected':''}>${esc(l.name)}</option>`)).join('');
    return `<select class="cm-input cm-loc-filter" style="max-width:240px;width:auto" onchange="CleanArmory.pickLoc(this.value)" title="Filtriraj po lokaciji">${opts}</select>`;
  }
  function locationSelectHtml(current){
    current=norm(current||'Oružarstvo - Klaićeva');
    const locs=locList().filter(l=>['Oružarstvo - Klaićeva','Krasno'].includes(l.name));
    const list=locs.length?locs:[{name:'Oružarstvo - Klaićeva'},{name:'Krasno'}];
    const known=list.some(l=>l.name===current);
    const value=known?current:'Oružarstvo - Klaićeva';
    const opts=list.map(l=>`<option value="${esc(l.name)}" ${l.name===value?'selected':''}>${esc(l.name)}</option>`).join('');
    return `<select class="cm-input" id="itemLoc" data-prev="${esc(value)}">${opts}</select>`;
  }
  async function onLocChange(sel){
    if(!sel) return;
    if(sel.value==='__new__'){
      const name=(window.prompt('Naziv nove lokacije:')||'').trim();
      if(!name){ sel.value=sel.dataset.prev||''; return; }
      try{
        if(window.SOVArmoryDB&&SOVArmoryDB.createLocation){
          const loc=await SOVArmoryDB.createLocation(name);
          const nm=(loc&&loc.name)||name;
          STATE.data=STATE.data||{}; STATE.data.locations=STATE.data.locations||[];
          if(!STATE.data.locations.some(l=>l.name===nm)) STATE.data.locations.push({id:loc&&loc.id,name:nm,type:(loc&&loc.type)||'storage'});
          const placeholder=[...sel.options].find(o=>o.value==='__new__');
          const opt=document.createElement('option'); opt.value=nm; opt.textContent=nm;
          sel.insertBefore(opt, placeholder); sel.value=nm;
          toast('Lokacija dodana: '+nm);
        } else { toast('Ne mogu dodati lokaciju (offline).'); sel.value=sel.dataset.prev||''; }
      }catch(e){ console.warn('createLocation', e); toast('Greška kod dodavanja lokacije.'); sel.value=sel.dataset.prev||''; }
    }
    sel.dataset.prev=sel.value;
  }
  function pickLoc(v){ STATE.locFilter=v||null; renderInventory(); }
  function categories(rows=filtered()){const m=new Map(); rows.forEach(r=>{const c=r.category||'Ostalo'; if(!m.has(c))m.set(c,[]); m.get(c).push(r)}); return [...m.entries()].sort((a,b)=>(categoryPriority(a[0])-categoryPriority(b[0]))||a[0].localeCompare(b[0],'hr'));}
  function subcategories(cat){const m=new Map(); filtered().filter(r=>r.category===cat).forEach(r=>{const s=r.subcategory||'Ostalo'; if(!m.has(s))m.set(s,[]); m.get(s).push(r)}); return [...m.entries()].sort((a,b)=>a[0].localeCompare(b[0],'hr'));}
  function renderKpis(){const el=document.getElementById('cmKpis'); if(el){el.innerHTML=''; el.style.display='none';}}
  function bindSearch(cb){const q=document.getElementById('cmSearch'); if(q){q.value=STATE.query; q.oninput=()=>{STATE.query=q.value; STATE.cat=null; STATE.sub=null; cb();}}}

  function renderMaster(){
    const root=document.getElementById('masterRoot'); if(!root)return; renderKpis();
    if(!STATE.rows || !STATE.rows.length){renderDbLoading(); return;}
    const reqs=STATE.requests||[];
    const requested=reqs.filter(r=>statusKey(r.status)==='requested').length;
    const issued=reqs.filter(r=>['issued','partial_return'].includes(statusKey(r.status))).length;
    const low=STATE.rows.filter(r=>r.minimum&&r.av<=r.minimum).length;
    root.innerHTML=`<div class="cm-grid"><a class="cm-card" href="oruzar-master-posudbe.html"><span class="ico">↔️</span><h2>Posudba</h2><p><b>${requested}</b> za izdati · <b>${issued}</b> vani. Statusi su isti kao u APK-u: za izdati → izdano → vraćeno / djelomično.</p><span>Otvori →</span></a><a class="cm-card" href="oruzar-master-inventar.html"><span class="ico">📦</span><h2>Inventar</h2><p>Pregled artikala po kategorijama, s brzim uređivanjem i exportom.</p><span>Otvori →</span></a><a class="cm-card" href="oruzar-master-inventura.html"><span class="ico">✅</span><h2>Inventura</h2><p>Brzo brojanje i XLS priprema po kategorijama.</p><span>Otvori →</span></a><a class="cm-card" href="oruzar-master-notes.html"><span class="ico">📝</span><h2>Bilješke</h2><p><b>${low}</b> stavki je ispod praga ili treba pažnju.</p><span>Otvori →</span></a></div>`;
  }

  function renderInventory(){
    const root=document.getElementById('inventoryRoot'); if(!root)return; injectCategoryMetaCss(); injectCatalogModeCss(); renderKpis(); bindSearch(renderInventory);
    if(!STATE.rows || !STATE.rows.length){renderDbLoading(); return;}
    const cat=STATE.cat, sub=STATE.sub;
    const allRows=filtered();
    const activeRows=allRows.filter(r=>!/(rashod|otpis|deleted|obrisano|arhiva|stari_katalog|neaktiv)/i.test(String(r.status||'')));
    const lowCount=activeRows.filter(r=>r.minimum&&r.av<=r.minimum).length;
    const hasQuery=!!strip(STATE.query);
    let title='Inventar opreme', hint='Kreni od kategorije, zatim podkategorije. Pretraga trpi tipfelere i radi po nazivu, modelu, lokaciji i napomeni.';
    let html='';
    if(hasQuery){
      const rows=activeRows.slice(0,120);
      title='Rezultati pretrage';
      hint=`Traženo: “${STATE.query}”. Pretraga trpi tipfelere, bez obzira na kvačice i velika/mala slova.`;
      html=`<div class="cm-section-head"><div><h2>${esc(title)}</h2><p>${esc(hint)}</p></div><div class="cm-mini-stats"><span>${esc(activeRows.length)} rezultata</span>${activeRows.length>120?`<span>prikazujem prvih 120</span>`:''}</div></div><div class="cm-tools cm-inline-actions"><button class="cm-btn" onclick="CleanArmory.clearSearch()">Očisti pretragu</button>${locFilterHtml()}</div>${rows.length?`<div class="item-grid item-grid-clean">${rows.map(itemCard).join('')}</div>`:`<div class="empty">Nema rezultata. Probaj kraći pojam, npr. “karab”, “bosch”, “croll”.</div>`}`;
    }
    else if(!cat){
      title='Kategorije opreme';
      hint='Čisti operativni pregled inventara za oružara. Količine su vidljive tek na artiklu, da se početni ekran ne zatrpa.';
      html=`<div class="cm-section-head"><div><h2>${esc(title)}</h2><p>${esc(hint)}</p></div><div class="cm-mini-stats"><span>${esc(activeRows.length)} stavki</span>${lowCount?`<span class="danger">${esc(lowCount)} ispod praga</span>`:''}</div></div><div class="cm-tools cm-inline-actions">${locFilterHtml()}</div><div class="cat-grid cat-grid-clean">${categories(activeRows).map(([c,rs])=>categoryTile(c,rs)).join('')}</div>`;
    }
    else if(!sub){
      const catRows=activeRows.filter(r=>r.category===cat);
      title=cat;
      hint='Odaberi podkategoriju ili se vrati na sve kategorije.';
      html=`<div class="cm-breadcrumb"><button onclick="CleanArmory.pickCat('')">Sve kategorije</button><span>${esc(displayCategoryName(cat))}</span></div>${categoryMetaPanel(cat)}<div class="cm-section-head"><div><h2>${esc(title)}</h2><p>${esc(hint)}</p></div><div class="cm-mini-stats"><span>${esc(catRows.length)} stavki</span></div></div><div class="cat-grid cat-grid-clean">${subcategories(cat).map(([s,rs])=>`<button class="cat-tile cat-tile-clean" onclick="CleanArmory.pickSub('${esc(s)}')"><span class="ico">${esc(subcategoryIcon(cat,s))}</span><b>${esc(displaySubcategoryName(cat,s))}</b><small>${esc(rs.length)} artikala</small></button>`).join('')}</div>`;
    }
    else {
      const rows=activeRows.filter(r=>r.category===cat&&r.subcategory===sub);
      title=sub;
      hint='Uredi artikl, dodaj novu stavku ili exportaj cijeli inventar iz gornjeg izbornika.';
      html=`<div class="cm-breadcrumb"><button onclick="CleanArmory.pickCat('')">Sve kategorije</button><button onclick="CleanArmory.pickCat('${esc(cat)}')">${esc(displayCategoryName(cat))}</button><span>${esc(displaySubcategoryName(cat,sub))}</span></div>${categoryMetaPanel(cat)}<div class="cm-section-head"><div><h2>${esc(title)}</h2><p>${esc(hint)}</p></div><div class="cm-mini-stats"><span>${esc(rows.length)} artikala</span></div></div><div class="cm-tools cm-inline-actions"><button class="cm-btn primary" onclick="CleanArmory.newItem()">+ Dodaj artikl</button><button class="cm-btn" onclick="CleanArmory.pickSub('')">← Podkategorije</button>${locFilterHtml()}</div><div class="item-grid item-grid-clean">${rows.map(itemCard).join('')}</div>`;
    }
    root.innerHTML=catalogSelectorPanel()+(html||`<div class="empty">Nema artikala za prikaz.</div>`);
  }
  function itemCard(r){
    const low=r.minimum&&r.av<=r.minimum;
    const statusClass=r.needsCount?'warn':(low?'bad':(r.av>0?'ok':'bad'));
    const qtyDisplay=r.needsCount?(r.qtyLabel||'—'):r.qty;
    const avDisplay=r.needsCount?(r.avLabel||'provjeriti'):r.av;
    const statusText=r.needsCount?'prebrojiti':(low?'ispod praga':(r.av>0?'dostupno':'nema dostupno'));
    // UI FIX v6.1.30: ne prikazuj interni XLS/source tekst na karticama opreme
    return `<article class="item-card item-card-clean ${low?'low-stock':''} ${r.needsCount?'needs-count':''}"><div class="item-head"><div><h3>${esc(r.name)}</h3><div class="muted">${esc(displayCategoryName(r.category))} · ${esc(displaySubcategoryName(r.category,r.subcategory))}</div></div><span class="badge ${statusClass}">${esc(statusText)}</span></div><div class="badgetray"><span class="badge">${esc(r.location||'bez lokacije')}</span><span class="badge">${esc(r.unit)}</span>${r.lastInventoryDate?`<span class="badge">inventura ${esc(r.lastInventoryDate)}</span>`:''}${r.variants&&r.variants>1?`<span class="badge">${esc(r.variants)} varijanti</span>`:''}${r.minimum?`<span class="badge warn">prag ${esc(r.minimum)}</span>`:''}</div><div class="stock stock-clean"><span><b>${esc(qtyDisplay)}</b><em>ukupno</em></span><span><b>${esc(avDisplay)}</b><em>dostupno</em></span><span><b>${esc(r.loan)}</b><em>vani</em></span></div><div class="cm-tools item-actions"><button class="cm-btn" onclick="CleanArmory.editItem('${esc(r.id)}')">Uredi</button><button class="cm-btn bad" onclick="CleanArmory.removeItem('${esc(r.id)}')">Makni</button></div></article>`;
  }


  async function loadRequests(){
    let req=null; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadRequests) req=await SOVArmoryDB.loadRequests(); }catch(e){console.warn('[armory master v5.47.3] load requests failed',e)}
    if(Array.isArray(req)){STATE.requests=req;STATE.reqSource='supabase';return req}
    try{STATE.requests=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]');STATE.reqSource='local';return STATE.requests}catch(e){STATE.requests=[];STATE.reqSource='empty';return []}
  }
  function itemPills(r){return (r.items||[]).map(i=>`<span class="badge">${esc(i.name||i.item_name||'Artikl')} × ${esc(i.quantity||1)}</span>`).join('')||'<span class="badge">bez stavki</span>';}
  function requestCard(r,mode){
    const k=statusKey(r.status);
    const actions=mode==='requested'
      ? `<button class="cm-btn primary" onclick="CleanArmory.issueLoan('${esc(r.id)}')">Označi izdano</button><button class="cm-btn" onclick="CleanArmory.setStatus('${esc(r.id)}','cancelled')">Odbij / zatvori</button>`
      : (mode==='active'?`<button class="cm-btn primary" onclick="CleanArmory.openReturn('${esc(r.id)}')">Povrat po artiklu</button>`:'');
    return `<div class="loan-row loan-status-${k}"><div class="loan-row-top"><div><b>${esc(r.user||r.member_name||r.requester_name||'Član')}</b><div class="muted">${esc(r.trip||r.note||'Zahtjev')} · ${esc((r.created_at||'').slice(0,10))}</div></div><span class="badge ${statusBadge(r.status)}">${esc(statusLabel(r.status))}</span></div><div class="loan-items">${itemPills(r)}</div><div class="cm-tools">${actions}<button class="cm-btn" onclick="CleanArmory.hideRequest('${esc(r.id)}')">Makni iz viewa</button></div></div>`;
  }
  async function renderLoans(){
    const root=document.getElementById('loansRoot'); if(!root)return; const reqs=await loadRequests();
    const requested=reqs.filter(r=>statusKey(r.status)==='requested');
    const active=reqs.filter(r=>['issued','partial_return'].includes(statusKey(r.status)));
    const done=reqs.filter(r=>['returned','cancelled'].includes(statusKey(r.status))).slice(0,30);
    root.innerHTML=`<section class="cm-panel cm-loan-summary"><div class="cm-section-head"><div><h2>Posudbe opreme</h2><p>Prvo izdavanje, zatim povrat po artiklu. Bez viška statusa i bez tehničkog debug teksta.</p></div><div class="cm-mini-stats"><span>${esc(requested.length)} za izdati</span><span>${esc(active.length)} vani</span></div></div><div class="cm-tools"><button class="cm-btn" onclick="CleanArmory.renderLoans()">Osvježi</button></div></section><div class="loan-grid loan-grid-v546 loan-grid-clean"><section class="cm-panel"><div class="cm-panel-head"><h2>Za izdati</h2><p class="muted">Zahtjevi koji čekaju fizičko izdavanje.</p></div><div class="loan-list">${requested.length?requested.map(r=>requestCard(r,'requested')).join(''):'<div class="empty">Nema novih zahtjeva.</div>'}</div><form class="cm-form manual-loan" onsubmit="CleanArmory.manualLoan(event)"><h3>Ručni unos</h3><div class="cm-form-grid"><input class="cm-input" id="mUser" placeholder="Tko traži"><input class="cm-input" id="mItem" placeholder="Artikl"><input class="cm-input" id="mQty" type="number" min="1" value="1"></div><div class="cm-tools"><input class="cm-input" id="mNote" placeholder="Izlet / napomena"><button class="cm-btn primary">Dodaj</button></div></form></section><section class="cm-panel"><div class="cm-panel-head"><h2>Izdano vani</h2><p class="muted">Aktivne posudbe i djelomični povrati.</p></div><div class="loan-list">${active.length?active.map(r=>requestCard(r,'active')).join(''):'<div class="empty">Ništa trenutno nije vani.</div>'}</div></section></div><section class="cm-panel closed-requests"><div class="cm-panel-head"><h2>Zatvoreno</h2><p class="muted">Zadnji povrati i odbijeni zahtjevi.</p></div><div class="loan-list closed-list">${done.length?done.map(r=>requestCard(r,'closed')).join(''):'<div class="empty">Još nema zatvorenih zahtjeva.</div>'}</div></section>`;
  }
  async function setStatus(id,status){
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.updateRequestStatus) await SOVArmoryDB.updateRequestStatus(id,status); }catch(e){console.warn('[armory master v5.47.3] remote status failed',e); toast('Supabase update nije prošao; spremam lokalno.');}
    try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const r=l.find(x=>String(x.id)===String(id)); if(r)r.status=status; localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
    await loadRequests(); await renderLoans(); renderMaster(); toast(statusLabel(status));
  }
  async function issueLoan(id){
    const r=(STATE.requests||[]).find(x=>String(x.id)===String(id));
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.issueRequest) await SOVArmoryDB.issueRequest(id,r); else await setStatus(id,'issued'); }
    catch(e){console.warn('[armory master v5.47.3] issue failed',e); await setStatus(id,'issued'); return;}
    try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const x=l.find(y=>String(y.id)===String(id)); if(x)x.status='issued'; localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
    await loadRequests(); await renderLoans(); renderMaster(); toast('Označeno izdano');
  }
  function openReturn(id){
    const r=(STATE.requests||[]).find(x=>String(x.id)===String(id)); if(!r){toast('Ne mogu naći posudbu.');return;}
    const items=(r.items||[]); if(!items.length){toast('Zahtjev nema stavke.');return;}
    const html=`<div class="cm-modal-backdrop" id="returnModal"><div class="cm-modal"><div class="cm-modal-head"><div><h2>Povrat po artiklu</h2><p class="muted">Upiši koliko je stvarno vraćeno. Ako nešto ostaje vani, zahtjev ide u djelomični povrat.</p></div><button class="cm-icon-btn" onclick="CleanArmory.closeReturn()">×</button></div><div class="return-list">${items.map((it,idx)=>{const q=countInt(it.quantity,1);return `<div class="return-row" data-idx="${idx}" data-id="${esc(it.id||'')}" data-name="${esc(it.name||it.item_name||'Artikl')}" data-qty="${q}"><div><b>${esc(it.name||it.item_name||'Artikl')}</b><div class="muted">Izdano: ${q}</div></div><label>Vraćeno<input class="cm-input return-qty" type="number" min="0" max="${q}" value="${q}"></label><label>Gdje ide vraćeno<select class="cm-input return-dest"><option>Oružarstvo - Klaićeva</option><option>Krasno</option><option>Kod posuđivača</option><option>Rashod</option></select></label><label>Ako nije sve vraćeno, ostatak je<select class="cm-input remain-dest"><option>Kod posuđivača</option><option>Krasno</option><option>Oružarstvo - Klaićeva</option><option>Izgubljeno</option><option>Rashod</option></select></label></div>`}).join('')}</div><textarea class="cm-input" id="returnNote" placeholder="Napomena, npr. dio ostao u jami / kod koga je ostatak"></textarea><div class="cm-tools"><button class="cm-btn" onclick="CleanArmory.closeReturn()">Odustani</button><button class="cm-btn primary" onclick="CleanArmory.confirmReturn('${esc(id)}')">Spremi povrat</button></div></div></div>`;
    document.body.insertAdjacentHTML('beforeend',html);
  }
  function closeReturn(){const m=document.getElementById('returnModal'); if(m)m.remove();}
  async function confirmReturn(id){
    const r=(STATE.requests||[]).find(x=>String(x.id)===String(id));
    const rows=[...document.querySelectorAll('#returnModal .return-row')].map(row=>{const issued=countInt(row.dataset.qty,0); const returned=countInt(row.querySelector('.return-qty').value,0); const ok=Math.min(Math.max(returned,0),issued); return {id:row.dataset.id||null,name:row.dataset.name,issued_quantity:issued,returned_quantity:ok,missing_quantity:Math.max(issued-ok,0),return_location:row.querySelector('.return-dest').value,remaining_location:row.querySelector('.remain-dest').value};});
    const full=rows.every(x=>x.missing_quantity===0); const status=full?'returned':'partial_return'; const note=document.getElementById('returnNote')?.value||'';
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.returnRequestItems) await SOVArmoryDB.returnRequestItems(id,rows,note,r); else await setStatus(id,status); }
    catch(e){console.warn('[armory master v5.47.3] return failed',e); toast('Supabase povrat nije prošao; spremam lokalno.');}
    try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const x=l.find(y=>String(y.id)===String(id)); if(x){x.status=status;x.return_items=rows;x.return_note=note;} localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
    closeReturn(); await loadRequests(); await renderLoans(); renderMaster(); toast(full?'Sve vraćeno':'Djelomični povrat spremljen');
  }
  async function hideRequest(id){
    if(!confirm('Maknuti ovu posudbu/zahtjev iz aktivnog viewa? Ostaje arhivirano u bazi.')) return;
    try{
      if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.hideRequestFromArmory){
        await SOVArmoryDB.hideRequestFromArmory(id);
      }
      try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]').filter(x=>String(x.id)!==String(id)); localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(_e){}
      STATE.requests=(STATE.requests||[]).filter(x=>String(x.id)!==String(id));
      await loadRequests(); await renderLoans(); renderMaster(); toast('Maknuto iz viewa');
    }catch(e){console.warn('[armory v6.1.39c] hide request failed',e); toast('Nije uspjelo maknuti iz viewa');}
  }

  async function manualLoan(ev){
    ev.preventDefault(); const req={id:'REQ-'+Date.now(),created_at:new Date().toISOString(),user:document.getElementById('mUser').value||'Član',trip:document.getElementById('mNote').value||'Ručni unos',status:'pending',items:[{id:'manual',name:document.getElementById('mItem').value||'Artikl',quantity:Number(document.getElementById('mQty').value)||1}]};
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.createRequest){const saved=await SOVArmoryDB.createRequest(req); req.id=saved.id||req.id; req.created_at=saved.created_at||req.created_at; req.status=saved.status||req.status; }}catch(e){console.warn(e)}
    const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); l.unshift(req); localStorage.setItem('sov_equipment_requests',JSON.stringify(l)); await loadRequests(); await renderLoans(); renderMaster(); toast('Zahtjev dodan');
  }

  function getRow(id){return (STATE.rows||[]).find(r=>String(r.id)===String(id));}
  function openItemModal(r){
    const isNew=!r; r=r||{id:'NEW-'+Date.now(),name:'',category:STATE.cat||'Ostalo',subcategory:STATE.sub||'Ostalo',qty:0,av:0,loan:0,location:'Oružarstvo - Klaićeva',minimum:0,status:'aktivno',type:'item'};
    const html=`<div class="cm-modal-backdrop" id="itemModal"><div class="cm-modal"><div class="cm-modal-head"><div><h2>${isNew?'Dodaj artikl':'Uredi artikl'}</h2><p class="muted">Ručni edit je za korekcije. Glavna kategorizacija dolazi iz SQL canonical viewa.</p></div><button class="cm-icon-btn" onclick="CleanArmory.closeItemModal()">×</button></div><form class="cm-form" onsubmit="CleanArmory.saveItem(event,'${esc(r.id)}',${isNew})"><div class="cm-form-grid"><input class="cm-input" id="itemName" placeholder="Naziv artikla" value="${esc(r.name)}"><input class="cm-input" id="itemCat" placeholder="Kategorija" value="${esc(r.rawCategory||r.category)}"><input class="cm-input" id="itemSub" placeholder="Podkategorija" value="${esc(r.rawSubcategory||r.subcategory)}"></div><div class="cm-form-grid"><input class="cm-input" id="itemQty" type="number" min="0" placeholder="Ukupno" value="${esc(r.qty)}"><input class="cm-input" id="itemAv" type="number" min="0" placeholder="Dostupno" value="${esc(r.av)}"><input class="cm-input" id="itemMin" type="number" min="0" placeholder="Crveni prag" value="${esc(r.minimum||0)}"></div><div class="cm-form-grid">${locationSelectHtml(r.location||'Oružarstvo - Klaićeva')}<input class="cm-input" id="itemCode" placeholder="Opcionalni kod / napomena" value="${esc(r.raw&&r.raw.physical_code_note||'')}"><input class="cm-input" id="itemStatus" placeholder="Status" value="${esc(r.status||'aktivno')}"></div><div class="cm-tools"><button class="cm-btn" type="button" onclick="CleanArmory.closeItemModal()">Odustani</button><button class="cm-btn primary">Spremi</button></div></form></div></div>`;
    document.body.insertAdjacentHTML('beforeend',html);
  }
  function closeItemModal(){const m=document.getElementById('itemModal'); if(m)m.remove();}
  async function saveItem(ev,id,isNew){
    ev.preventDefault(); const row=getRow(id)||{}; const legacy=isNew?('ART-'+Date.now()):(row.raw&&row.raw.legacy_id)||id;
    const payload={legacy_id:legacy,catalog_id:legacy,name:document.getElementById('itemName').value.trim()||'Artikl',category_name:document.getElementById('itemCat').value.trim()||'Ostalo',subcategory:document.getElementById('itemSub').value.trim()||'Ostalo',quantity:countInt(document.getElementById('itemQty').value,0),available:countInt(document.getElementById('itemAv').value,0),loaned:Math.max(0,countInt(document.getElementById('itemQty').value,0)-countInt(document.getElementById('itemAv').value,0)),minimum:countInt(document.getElementById('itemMin').value,0),location_name:document.getElementById('itemLoc').value.trim()||'Oružarstvo - Klaićeva',status:document.getElementById('itemStatus').value.trim()||'aktivno',physical_code_note:document.getElementById('itemCode').value.trim()||null,item_kind:'quantity_article',code_required:false,member_visible:true};
    try{
      if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()){
        if(!isNew && SOVArmoryDB.updateEquipmentItemFull){
          // v6.1.36: persist the FULL edit (qty, naziv, kategorija, podkategorija, lokacija, prag, dostupno, status),
          // not just the inventura count. Keyed by the row's real id so NULL-legacy_id imports still match.
          await SOVArmoryDB.updateEquipmentItemFull(
            {id:id,legacy_id:(row.raw&&row.raw.legacy_id)||null,catalog_id:(row.raw&&row.raw.catalog_id)||null,name:payload.name},
            {
              name:payload.name,
              category_name:payload.category_name,
              subcategory:payload.subcategory,
              quantity:payload.quantity,
              available:payload.available,
              loaned:payload.loaned,
              minimum:payload.minimum,
              location_name:payload.location_name,
              status:payload.status,
              availability:(payload.available>0?'dostupno':'nedostupno'),
              physical_code_note:payload.physical_code_note,
              quantity_label:String(payload.quantity),
              available_label:String(payload.available),
              last_inventory_date:new Date().toISOString().slice(0,10)
            }
          );
        }else if(!isNew && SOVArmoryDB.updateInventoryCount){
          await SOVArmoryDB.updateInventoryCount({legacy_id:legacy,catalog_id:legacy,id,name:payload.name},payload.available,payload.status,'Inventura Klaićeva 2026 — ručni edit');
        }else if(SOVArmoryDB.upsertSimpleItem){
          await SOVArmoryDB.upsertSimpleItem(payload);
        }else throw new Error('no supabase helper');
        try{localStorage.removeItem('sov_armory_catalog_cache_v607')}catch(_e){}
      } else throw new Error('no supabase helper');
    }
    catch(e){ console.warn('[armory master v6.1.35-save-fix] save item local fallback',e); const local=JSON.parse(localStorage.getItem('sov_armory_items_override')||'[]'); const i=local.findIndex(x=>String(x.legacy_id)===String(payload.legacy_id)); if(i>=0)local[i]=payload; else local.unshift(payload); localStorage.setItem('sov_armory_items_override',JSON.stringify(local)); }
    closeItemModal(); STATE.data=null; await loadData(); renderInventory(); toast('Artikl spremljen');
  }
  function newItem(){openItemModal(null)}
  function editItem(id){const r=getRow(id); if(!r){toast('Ne mogu naći artikl.');return;} openItemModal(r)}
  async function removeItem(id){const r=getRow(id); if(!r){toast('Ne mogu naći artikl.');return;} if(!confirm('Maknuti/rashodovati artikl iz aktivnog inventara?'))return; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.retireSimpleItem) await SOVArmoryDB.retireSimpleItem((r.raw&&r.raw.legacy_id)||id,r.name); }catch(e){console.warn(e)} r.status='rashod'; r.av=0; renderInventory(); toast('Artikl maknut iz aktivnog inventara')}

  function safeSheetName(name){let x=String(name||'Kategorija').replace(/[\\\/?*\[\]:]/g,' ').replace(/\s+/g,' ').trim(); if(!x)x='Kategorija'; return x.slice(0,31)}
  function xmlEsc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]));}
  function xlsCell(v,cls=''){return `<td class="${cls}">${xmlEsc(v)}</td>`;}
  function xlsWorkbook(filename,sheets){const names=sheets.map(s=>safeSheetName(s.name)); const tabs=names.map(n=>`<x:ExcelWorksheet><x:Name>${xmlEsc(n)}</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet>`).join(''); const body=sheets.map((sheet,idx)=>`<div style="mso-element:worksheet" id="${xmlEsc(names[idx])}"><table>${sheet.html}</table></div>`).join('\n'); const html=`<!doctype html><html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:x="urn:schemas-microsoft-com:office:excel" xmlns="http://www.w3.org/TR/REC-html40"><head><meta charset="utf-8"><!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets>${tabs}</x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]--><style>table{border-collapse:collapse;font-family:Arial,sans-serif;font-size:12px}td,th{border:1px solid #999;padding:6px;mso-number-format:"\\@"}th{background:#d9ead3;font-weight:bold}.num{mso-number-format:"0"}.head{background:#073b32;color:#fff;font-size:16px;font-weight:bold}</style></head><body>${body}</body></html>`; const blob=new Blob([html],{type:'application/vnd.ms-excel;charset=utf-8'}); const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download=filename; document.body.appendChild(a); a.click(); setTimeout(()=>{URL.revokeObjectURL(a.href); a.remove();},1500);}
  function rowsByCategory(){const m=new Map(); STATE.rows.filter(r=>r.name&&!/rashod|otpis|deleted|obrisano|arhiva|stari_katalog|neaktiv/i.test(String(r.status||''))).forEach(r=>{const c=r.category||'Ostalo'; if(!m.has(c))m.set(c,[]); m.get(c).push(r);}); return [...m.entries()].sort((a,b)=>(categoryPriority(a[0])-categoryPriority(b[0]))||a[0].localeCompare(b[0],'hr'));}
  async function exportInventoryXls(){await loadData(); const date=new Date().toISOString().slice(0,10); const sheets=rowsByCategory().map(([cat,rows])=>{const displayCat=displayCategoryName(cat); const header=`<tr><th colspan="9" class="head">Inventar — ${xmlEsc(displayCat)} — ${xmlEsc(date)}</th></tr><tr><th>Kategorija</th><th>Podkategorija</th><th>Naziv</th><th>Količina</th><th>Jedinica</th><th>Datum evidencije</th><th>Lokacija</th><th>Status</th><th>Napomena / detalji</th></tr>`; const body=rows.sort((a,b)=>(a.subcategory+a.name).localeCompare(b.subcategory+b.name,'hr')).map(r=>`<tr>${xlsCell(displayCategoryName(r.category))}${xlsCell(displaySubcategoryName(r.category,r.subcategory))}${xlsCell(r.name)}${xlsCell(r.needsCount?(r.qtyLabel||''):r.qty,'num')}${xlsCell(r.unit)}${xlsCell(r.lastInventoryDate||'')}${xlsCell(r.location||'Oružarstvo - Klaićeva')}${xlsCell(r.status||'aktivno')}${xlsCell((r.raw&&(r.raw.physical_code_note||r.raw.note||r.raw.internal_note||r.raw.sku))||'')}</tr>`).join(''); return {name:displayCat,html:header+body};}); if(!sheets.length){toast('Nema inventara za export.');return;} xlsWorkbook(`SOV_inventar_${date}.xls`,sheets); toast('Inventar exportiran u XLS');}
  async function exportInventuraXls(){await loadData(); const date=(document.querySelector('input[type="date"]')?.value)||new Date().toISOString().slice(0,10); const sheets=rowsByCategory().map(([cat,rows])=>{const displayCat=displayCategoryName(cat); const header=`<tr><th colspan="10" class="head">Inventura — ${xmlEsc(displayCat)} — ${xmlEsc(date)}</th></tr><tr><th>Kategorija</th><th>Podkategorija</th><th>Naziv</th><th>Broj u bazi</th><th>Jedinica</th><th>Stvarno prebrojano</th><th>Razlika</th><th>Lokacija</th><th>Za rashod?</th><th>Napomena</th></tr>`; const body=rows.sort((a,b)=>(a.subcategory+a.name).localeCompare(b.subcategory+b.name,'hr')).map(r=>`<tr>${xlsCell(displayCategoryName(r.category))}${xlsCell(displaySubcategoryName(r.category,r.subcategory))}${xlsCell(r.name)}${xlsCell(r.needsCount?(r.qtyLabel||''):r.qty,'num')}${xlsCell(r.unit)}${xlsCell('')}${xlsCell('')}${xlsCell(r.location||'Oružarstvo - Klaićeva')}${xlsCell('')}${xlsCell(r.needsCount?'prebrojiti':'')}</tr>`).join(''); return {name:displayCat,html:header+body};}); if(!sheets.length){toast('Nema inventara za inventuru export.');return;} xlsWorkbook(`SOV_inventura_${date}.xls`,sheets); toast('Inventura exportirana u XLS');}

  async function loadNotes(){try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadArmoryNotes){const n=await SOVArmoryDB.loadArmoryNotes(); if(Array.isArray(n))return n;} }catch(e){console.warn(e)} try{return JSON.parse(localStorage.getItem('sov_armory_notes')||'[]')}catch(e){return []}}
  async function renderNotes(){const root=document.getElementById('notesRoot'); if(!root)return; const notes=await loadNotes(); const open=notes.filter(n=>!/done|closed|obavljeno/i.test(String(n.status||'open'))); root.innerHTML=`<div class="loan-grid"><section class="cm-panel"><h2>+ Nova bilješka / reminder</h2><form class="cm-form" onsubmit="CleanArmory.saveNote(event)"><input class="cm-input" id="noteTitle" placeholder="Naslov, npr. Nabaviti spitove"><textarea class="cm-input" id="noteBody" placeholder="Detalji / napomena"></textarea><div class="cm-form-grid"><input class="cm-input" id="noteDue" type="date"><select class="cm-input" id="noteType"><option value="todo">Obaviti</option><option value="buy">Nabaviti</option><option value="check">Provjeriti</option></select><select class="cm-input" id="notePriority"><option value="normal">Normalno</option><option value="high">Hitno</option><option value="low">Nisko</option></select></div><button class="cm-btn primary">Spremi reminder</button></form></section><section class="cm-panel"><h2>Bilješke</h2><div class="loan-list">${open.length?open.map(n=>`<div class="loan-row"><div class="loan-row-top"><div><b>${esc(n.title||'Bilješka')}</b><div class="muted">${esc(n.due_date||'bez datuma')} · ${esc(n.note_type||n.type||'todo')} · ${esc(n.priority||'normal')}</div></div><span class="badge ${n.priority==='high'?'bad':'warn'}">${esc(n.status||'open')}</span></div><p>${esc(n.body||n.note||'')}</p><button class="cm-btn primary" onclick="CleanArmory.doneNote('${esc(n.id)}')">Označi obavljeno</button></div>`).join(''):'<div class="empty">Nema otvorenih podsjetnika.</div>'}</div></section></div>`;}
  async function saveNote(ev){ev.preventDefault(); const n={id:'NOTE-'+Date.now(),title:document.getElementById('noteTitle').value||'Bilješka',body:document.getElementById('noteBody').value||'',due_date:document.getElementById('noteDue').value||null,note_type:document.getElementById('noteType').value,priority:document.getElementById('notePriority').value,status:'open',created_at:new Date().toISOString()}; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.saveArmoryNote){await SOVArmoryDB.saveArmoryNote(n);} }catch(e){console.warn(e)} const l=JSON.parse(localStorage.getItem('sov_armory_notes')||'[]'); l.unshift(n); localStorage.setItem('sov_armory_notes',JSON.stringify(l)); await renderNotes(); toast('Reminder spremljen');}
  async function doneNote(id){try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.doneArmoryNote) await SOVArmoryDB.doneArmoryNote(id);}catch(e){console.warn(e)} const l=JSON.parse(localStorage.getItem('sov_armory_notes')||'[]'); const n=l.find(x=>String(x.id)===String(id)); if(n)n.status='done'; localStorage.setItem('sov_armory_notes',JSON.stringify(l)); await renderNotes(); toast('Označeno obavljeno');}

  async function saveCategoryMeta(ev,cat){
    ev.preventDefault();
    const icon=norm(document.getElementById('catIconInput')?.value||iconFor(cat));
    const note=norm(document.getElementById('catNoteInput')?.value||'');
    try{
      if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.updateCategoryMeta){
        const saved=await SOVArmoryDB.updateCategoryMeta(cat,icon,note);
        STATE.data=STATE.data||{}; STATE.data.categories=STATE.data.categories||[];
        const key=strip(cat);
        let m=STATE.data.categories.find(c=>strip(c.name)===key);
        if(!m){m={name:cat}; STATE.data.categories.push(m);}
        m.icon=(saved&&saved.icon)||icon; m.note=(saved&&saved.note)||note; m.id=(saved&&saved.id)||m.id;
        toast('Kategorija spremljena');
      } else {
        toast('Supabase nije dostupan za spremanje kategorije.');
      }
    }catch(e){console.warn('saveCategoryMeta failed',e); toast('Greška kod spremanja kategorije');}
    renderInventory();
  }

  async function init(){renderDbLoading(); await loadData(); await loadRequests(); renderKpis(); if(STATE.rows&&STATE.rows.length){renderMaster(); renderInventory();} else {renderDbLoading(); scheduleDbRetry();} await renderLoans(); await renderNotes();}
  window.CleanArmory={init,pickCat(c){STATE.cat=c||null;STATE.sub=null;renderInventory()},pickSub(s){STATE.sub=s||null;renderInventory()},clearSearch(){STATE.query='';STATE.cat=null;STATE.sub=null;const q=document.getElementById('cmSearch'); if(q)q.value=''; renderInventory()},renderLoans,setStatus,manualLoan,newItem,editItem,removeItem,exportInventoryXls,exportInventuraXls,openReturn,closeReturn,confirmReturn,closeItemModal,saveItem,renderNotes,saveNote,doneNote,saveCategoryMeta,issueLoan,onLocChange,pickLoc,selectCatalogMode,reloadCatalogMode,createCatalogSnapshotUi,hideRequest};
  document.addEventListener('DOMContentLoaded',init);
})();
