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
  function toastAction(message,actionLabel,action){
    let t=document.getElementById('cmToast');
    if(!t){t=document.createElement('div');t.id='cmToast';t.className='cm-toast';document.body.appendChild(t)}
    t.innerHTML='';
    const span=document.createElement('span'); span.textContent=message+' '; t.appendChild(span);
    if(actionLabel&&typeof action==='function'){
      const btn=document.createElement('button'); btn.type='button'; btn.className='cm-toast-action'; btn.textContent=actionLabel;
      btn.onclick=()=>{clearTimeout(t._to); t.classList.remove('show'); action();};
      t.appendChild(btn);
    }
    t.classList.add('show'); clearTimeout(t._to); t._to=setTimeout(()=>t.classList.remove('show'),5200);
  }
  function confirmSimple(message,yesLabel){
    return window.confirm(message+'\n\n'+(yesLabel||'Potvrdi')+' / Odustani');
  }
  function requestPerson(r){return esc(r&&((r.user||r.member_name||r.requester_name||r.email)||'članu'));}
  function categoryName(row,type){
    const raw=norm(row.xls_category||row.raw_category||row.main_category||row.category_name||row.category||(type==='rope'?'Užeta':'Ostalo'))||'Ostalo';
    return raw==='Užad' ? 'Užeta' : raw;
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
  function iconFor(t){t=strip(t); if(t.includes('osobna'))return '🧑‍🚒'; if(t.includes('uzad')||t.includes('uzetna')||t.includes('uze'))return '🪢'; if(t.includes('postav')||t.includes('spit')||t.includes('sidri'))return '⚓'; if(t.includes('crtan')||t.includes('mjer')||t.includes('kompas')||t.includes('busol'))return '📐'; if(t.includes('busil')||t.includes('svrd'))return '🔩'; if(t.includes('elektro')||t.includes('rasvjet')||t.includes('foto'))return '🔦'; if(t.includes('dron'))return '🚁'; if(t.includes('alpin'))return '⛰️'; if(t.includes('med'))return '🧰'; if(t.includes('ronil'))return '🤿'; if(t.includes('logor')||t.includes('kamp'))return '⛺'; if(t.includes('cisto'))return '🧹'; if(t.includes('pros'))return '🔨'; if(t.includes('alat')||t.includes('radion'))return '🧰'; return '📦'}
  function categoryPriority(c){
    const x=strip(c);
    const order=['osobna oprema','oprema za postavljanje','cisto podzemlje','oprema za crtanje','oprema za prosirivanje','elektro i foto oprema','alpinisticka oprema','ronilacka oprema','ostali alat','uzeta','oprema za logor','medicinska oprema','ostalo'];
    const exact=order.indexOf(x);
    if(exact>=0) return exact;
    const i=order.findIndex(k=>x.includes(k)); return i<0?999:i;
  }


  let STATE={data:null,rows:[],cat:null,sub:null,query:'',requests:[],reqSource:'none'};
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
    STATE.data=live; STATE.source='Evidencija opreme'; STATE.liveRows=liveRows; STATE.rows=makeRows(STATE.data);
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
    return `<div class="cm-panel armory-db-loading"><h2>Učitavam podatke, samo trenutak…</h2><div class="armory-db-bar" aria-hidden="true"></div><p>Popis opreme će se prikazati čim se podaci učitaju.</p></div>`;
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
          STATE.data=live; STATE.source='Evidencija opreme'; STATE.liveRows=liveRows; STATE.staticRows=0; STATE.rows=makeRows(STATE.data);
          return STATE.data;
        }
      }
    }catch(e){console.warn('[armory master v6.1.6] strict Supabase catalog failed',e)}
    STATE.data={items:[],ropes:[],pieces:[],categories:[]}; STATE.source='Učitavam podatke'; STATE.liveRows=0; STATE.staticRows=0; STATE.rows=[];
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
    return {raw:r,type,id:String(r.app_id||r.source_id||r.legacy_id||r.catalog_id||r.sku||r.id||`${type}-${i}`),name:displayName(r,type),category:categoryName(r,type),subcategory:subcategoryName(r),qty,av,loan,qtyLabel,avLabel,unit,needsCount,lastInventoryDate:norm(r.last_inventory_date||''),sourceXlsRow:r.source_xls_row||null,location:norm(r.location_name||r.location||''),status:norm(r.status||r.availability||'aktivno'),minimum:countInt(r.minimum??r.threshold??r.min_quantity??0,0),search:qtext(r),variants:countInt(r.variant_count||1,1)};
  }
  function filtered(){
    let rows=STATE.rows;
    if(STATE.query){rows=rows.filter(r=>rowMatchesQuery(r,STATE.query));}
    return rows;
  }
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
    root.innerHTML=`<div class="cm-grid cm-grid-dashboard"><a class="cm-card cm-card-posudbe" href="oruzar-master-posudbe.html"><span class="ico cm-card-icon" aria-hidden="true">📋</span><h2>Posudbe</h2><p><b>${requested}</b> za izdati · <b>${issued}</b> vani. Zahtjevi, izdavanje i povrat opreme.</p><span>Otvori posudbe →</span></a><button class="cm-card cm-card-button cm-card-return" onclick="CleanArmory.openLegacyReturn()"><span class="ico cm-card-icon" aria-hidden="true">↩️</span><h2>Povrat stare opreme</h2><p>Za opremu koja se vratila iz stare ili loše evidentirane posudbe. Ne traži otvorenu posudbu.</p><span>Zaprimi opremu →</span></button><a class="cm-card cm-card-inventar" href="oruzar-master-inventar.html"><span class="ico cm-card-icon" aria-hidden="true">🧰</span><h2>Inventar</h2><p>Pregled opreme po kategorijama, uređivanje artikala i preuzimanje popisa.</p><span>Otvori inventar →</span></a><a class="cm-card cm-card-inventura" href="oruzar-master-inventura.html"><span class="ico cm-card-icon" aria-hidden="true">✅</span><h2>Inventura</h2><p>Brzo brojanje, provjera stanja i spremanje izvještaja po kategorijama.</p><span>Otvori inventuru →</span></a><a class="cm-card cm-card-notes" href="oruzar-master-notes.html"><span class="ico cm-card-icon" aria-hidden="true">📝</span><h2>Bilješke</h2><p><b>${low}</b> stavki je ispod praga ili treba pažnju.</p><span>Otvori bilješke →</span></a></div>`;
  }

  function renderInventory(){
    const root=document.getElementById('inventoryRoot'); if(!root)return; renderKpis(); bindSearch(renderInventory);
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
      html=`<div class="cm-section-head"><div><h2>${esc(title)}</h2><p>${esc(hint)}</p></div><div class="cm-mini-stats"><span>${esc(activeRows.length)} rezultata</span>${activeRows.length>120?`<span>prikazujem prvih 120</span>`:''}</div></div><div class="cm-tools cm-inline-actions"><button class="cm-btn" onclick="CleanArmory.clearSearch()">Očisti pretragu</button></div>${rows.length?`<div class="item-grid item-grid-clean">${rows.map(itemCard).join('')}</div>`:`<div class="empty">Nema rezultata. Probaj kraći pojam, npr. “karab”, “bosch”, “croll”.</div>`}`;
    }
    else if(!cat){
      title='Kategorije opreme';
      hint='Čisti operativni pregled inventara za oružara. Količine su vidljive tek na artiklu, da se početni ekran ne zatrpa.';
      html=`<div class="cm-section-head"><div><h2>${esc(title)}</h2><p>${esc(hint)}</p></div><div class="cm-mini-stats"><span>${esc(activeRows.length)} stavki</span>${lowCount?`<span class="danger">${esc(lowCount)} ispod praga</span>`:''}</div></div><div class="cat-grid cat-grid-clean">${categories(activeRows).map(([c,rs])=>`<button class="cat-tile cat-tile-clean" onclick="CleanArmory.pickCat('${esc(c)}')"><span class="ico">${iconFor(c)}</span><b>${esc(c)}</b><small>${esc(rs.length)} stavki · otvori podkategorije</small></button>`).join('')}</div>`;
    }
    else if(!sub){
      const catRows=activeRows.filter(r=>r.category===cat);
      title=cat;
      hint='Odaberi podkategoriju ili se vrati na sve kategorije.';
      html=`<div class="cm-breadcrumb"><button onclick="CleanArmory.pickCat('')">Sve kategorije</button><span>${esc(cat)}</span></div><div class="cm-section-head"><div><h2>${esc(title)}</h2><p>${esc(hint)}</p></div><div class="cm-mini-stats"><span>${esc(catRows.length)} stavki</span></div></div><div class="cat-grid cat-grid-clean">${subcategories(cat).map(([s,rs])=>`<button class="cat-tile cat-tile-clean" onclick="CleanArmory.pickSub('${esc(s)}')"><span class="ico">${iconFor(s)}</span><b>${esc(s)}</b><small>${esc(rs.length)} artikala</small></button>`).join('')}</div>`;
    }
    else {
      const rows=activeRows.filter(r=>r.category===cat&&r.subcategory===sub);
      title=sub;
      hint='Uredi artikl, dodaj novu stavku ili preuzmi cijeli inventar iz gornjeg izbornika.';
      html=`<div class="cm-breadcrumb"><button onclick="CleanArmory.pickCat('')">Sve kategorije</button><button onclick="CleanArmory.pickCat('${esc(cat)}')">${esc(cat)}</button><span>${esc(sub)}</span></div><div class="cm-section-head"><div><h2>${esc(title)}</h2><p>${esc(hint)}</p></div><div class="cm-mini-stats"><span>${esc(rows.length)} artikala</span></div></div><div class="cm-tools cm-inline-actions"><button class="cm-btn primary" onclick="CleanArmory.newItem()">+ Dodaj artikl</button><button class="cm-btn" onclick="CleanArmory.pickSub('')">← Podkategorije</button></div><div class="item-grid item-grid-clean">${rows.map(itemCard).join('')}</div>`;
    }
    root.innerHTML=html||`<div class="empty">Nema artikala za prikaz.</div>`;
  }
  function itemCard(r){
    const low=r.minimum&&r.av<=r.minimum;
    const statusClass=r.needsCount?'warn':(low?'bad':(r.av>0?'ok':'bad'));
    const qtyDisplay=r.needsCount?(r.qtyLabel||'—'):r.qty;
    const avDisplay=r.needsCount?(r.avLabel||'provjeriti'):r.av;
    const statusText=r.needsCount?'prebrojiti':(low?'ispod praga':(r.av>0?'dostupno':'nema dostupno'));
    // UI FIX v6.1.30: ne prikazuj interni XLS/source tekst na karticama opreme
    return `<article class="item-card item-card-clean ${low?'low-stock':''} ${r.needsCount?'needs-count':''}"><div class="item-head"><div><h3>${esc(r.name)}</h3><div class="muted">${esc(r.category)} · ${esc(r.subcategory)}</div></div><span class="badge ${statusClass}">${esc(statusText)}</span></div><div class="badgetray"><span class="badge">${esc(r.location||'bez lokacije')}</span><span class="badge">${esc(r.unit)}</span>${r.lastInventoryDate?`<span class="badge">inventura ${esc(r.lastInventoryDate)}</span>`:''}${r.variants&&r.variants>1?`<span class="badge">${esc(r.variants)} varijanti</span>`:''}${r.minimum?`<span class="badge warn">prag ${esc(r.minimum)}</span>`:''}</div><div class="stock stock-clean"><span><b>${esc(qtyDisplay)}</b><em>ukupno</em></span><span><b>${esc(avDisplay)}</b><em>dostupno</em></span><span><b>${esc(r.loan)}</b><em>vani</em></span></div><div class="cm-tools item-actions"><button class="cm-btn primary" onclick="CleanArmory.openLegacyReturn('${esc(r.id)}')">Povrat stare</button><button class="cm-btn" onclick="CleanArmory.editItem('${esc(r.id)}')">Uredi</button><button class="cm-btn bad" onclick="CleanArmory.removeItem('${esc(r.id)}')">Makni</button></div></article>`;
  }


  async function loadRequests(){
    let req=null; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadRequests) req=await SOVArmoryDB.loadRequests(); }catch(e){console.warn('[armory master v5.47.3] load requests failed',e)}
    if(Array.isArray(req)){STATE.requests=req;STATE.reqSource='supabase';return req}
    try{STATE.requests=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]');STATE.reqSource='local';return STATE.requests}catch(e){STATE.requests=[];STATE.reqSource='empty';return []}
  }
  function itemPills(r){
    const items=r.items||[];
    if(!items.length) return '<span class="badge">bez stavki</span>';
    const render=()=>items.map(i=>`<span class="badge">${esc(i.name||i.item_name||'Artikl')} × ${esc(i.quantity||1)}</span>`).join('');
    if(items.length>8){
      const text=strip([r.package_name,r.kit_name,r.bundle_name,r.note,r.trip,items.map(i=>i.name||i.item_name).join(' ')].join(' '));
      const title=/srt|croll|pojas|descender|penjal|pupkov|gurtna|maillon/.test(text)?'Osobni SRT komplet':`${items.length} stavki opreme`;
      return `<details class="loan-items-collapsed"><summary>${esc(title)} (${esc(items.length)} stavki) · Prikaži stavke</summary><div class="loan-items-expanded">${render()}</div></details>`;
    }
    return render();
  }
  function requestCard(r,mode){const k=statusKey(r.status);return `<div class="loan-row loan-status-${k}"><div class="loan-row-top"><div><b>${esc(r.user||r.member_name||r.requester_name||'Član')}</b><div class="muted">${esc(r.trip||r.note||'Zahtjev')} · ${esc((r.created_at||'').slice(0,10))}</div></div><span class="badge ${statusBadge(r.status)}">${esc(statusLabel(r.status))}</span></div><div class="loan-items">${itemPills(r)}</div><div class="cm-tools">${mode==='requested'?`<button class="cm-btn primary" onclick="CleanArmory.issueLoan('${esc(r.id)}')">Izdaj opremu</button><button class="cm-btn" onclick="CleanArmory.setStatus('${esc(r.id)}','cancelled')">Otkaži zahtjev</button>`:`<button class="cm-btn primary" onclick="CleanArmory.openReturn('${esc(r.id)}')">Vrati opremu</button>`}</div></div>`;}
  async function renderLoans(){
    const root=document.getElementById('loansRoot'); if(!root)return; const reqs=await loadRequests();
    const requested=reqs.filter(r=>statusKey(r.status)==='requested');
    const active=reqs.filter(r=>['issued','partial_return'].includes(statusKey(r.status)));
    const done=reqs.filter(r=>['returned','cancelled'].includes(statusKey(r.status))).slice(0,30);
    root.innerHTML=`<section class="cm-panel cm-loan-summary"><div class="cm-section-head"><div><h2>Posudbe opreme</h2><p>Zahtjevi članova, izdavanje opreme i povrat.</p></div><div class="cm-mini-stats"><span>${esc(requested.length)} za izdati</span><span>${esc(active.length)} vani</span></div></div><div class="cm-tools"><button class="cm-btn" onclick="CleanArmory.renderLoans()">Osvježi</button><button class="cm-btn primary" onclick="CleanArmory.openLegacyReturn()">Vrati staru opremu</button></div></section><section class="cm-panel legacy-return-panel"><h2>Povrat bez otvorene posudbe</h2><p class="muted">Za opremu koja se vratila, a nije bila uredno zadužena.</p><div class="cm-tools"><button class="cm-btn primary" onclick="CleanArmory.openLegacyReturn()">Zaprimi opremu</button></div></section><div class="loan-grid loan-grid-v546 loan-grid-clean"><section class="cm-panel"><div class="cm-panel-head"><h2>Za izdati</h2><p class="muted">Zahtjevi koji čekaju fizičko izdavanje.</p></div><div class="loan-list">${requested.length?requested.map(r=>requestCard(r,'requested')).join(''):'<div class="empty">Nema novih zahtjeva.</div>'}</div><form class="cm-form manual-loan" onsubmit="CleanArmory.manualLoan(event)"><h3>Ručni unos</h3><div class="cm-form-grid"><input class="cm-input" id="mUser" placeholder="Tko traži"><input class="cm-input" id="mItem" placeholder="Artikl"><input class="cm-input" id="mQty" type="number" min="1" value="1"></div><div class="cm-tools"><input class="cm-input" id="mNote" placeholder="Izlet / napomena"><button class="cm-btn primary">Dodaj zahtjev</button></div></form></section><section class="cm-panel"><div class="cm-panel-head"><h2>Izdano vani</h2><p class="muted">Aktivne posudbe i djelomični povrati.</p></div><div class="loan-list">${active.length?active.map(r=>requestCard(r,'active')).join(''):'<div class="empty">Ništa trenutno nije vani.</div>'}</div></section></div><section class="cm-panel closed-requests"><div class="cm-panel-head"><h2>Zatvoreno</h2><p class="muted">Zadnji povrati i odbijeni zahtjevi.</p></div><div class="loan-list closed-list">${done.length?done.map(r=>`<div class="loan-row compact"><div class="loan-row-top"><div><b>${esc(r.user||r.member_name||r.requester_name||'Član')}</b><div class="muted">${esc(r.trip||r.note||'Zahtjev')}</div></div><span class="badge ${statusBadge(r.status)}">${esc(statusLabel(r.status))}</span></div><div class="loan-items">${itemPills(r)}</div></div>`).join(''):'<div class="empty">Još nema zatvorenih zahtjeva.</div>'}</div></section>`;
  }
  async function setStatus(id,status,opts){
    opts=opts||{};
    const req=(STATE.requests||[]).find(x=>String(x.id)===String(id));
    const oldStatus=req&&req.status;
    if(!opts.skipConfirm && statusKey(status)==='cancelled'){
      if(!confirmSimple(`Otkazati zahtjev korisnika ${requestPerson(req)}?`,'Da, otkaži zahtjev')) return;
    }
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.updateRequestStatus) await SOVArmoryDB.updateRequestStatus(id,status); }catch(e){console.warn('[armory master v5.47.3] remote status failed',e); toast('Nije uspjelo spremiti online; spremam lokalno.');}
    try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const r=l.find(x=>String(x.id)===String(id)); if(r)r.status=status; localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
    await loadRequests(); await renderLoans(); renderMaster();
    if(statusKey(status)==='cancelled' && oldStatus){
      toastAction('Zahtjev je otkazan.','Poništi',()=>setStatus(id,oldStatus,{skipConfirm:true}));
    }else toast(statusLabel(status));
  }
  async function issueLoan(id){
    const r=(STATE.requests||[]).find(x=>String(x.id)===String(id));
    if(!confirmSimple(`Izdati ovu opremu korisniku ${requestPerson(r)}?`,'Da, izdaj opremu')) return;
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.issueRequest) await SOVArmoryDB.issueRequest(id,r); else await setStatus(id,'issued',{skipConfirm:true}); }
    catch(e){console.warn('[armory master v5.47.3] issue failed',e); await setStatus(id,'issued',{skipConfirm:true}); toastAction('Oprema je označena kao izdana. Provjeri stanje.','Poništi',()=>undoIssueLoan(id,r)); return;}
    try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const x=l.find(y=>String(y.id)===String(id)); if(x)x.status='issued'; localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
    await loadRequests(); await renderLoans(); renderMaster();
    toastAction('Oprema je izdana.','Poništi',()=>undoIssueLoan(id,r));
  }
  async function undoIssueLoan(id,req){
    if(!confirmSimple(`Poništiti izdavanje za korisnika ${requestPerson(req)}?\n\nOprema će se pokušati vratiti u Oružarstvo, a zahtjev u stanje “za izdati”.`,'Da, poništi izdavanje')) return;
    let ok=false;
    try{
      if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.undoIssueRequest){
        ok=await SOVArmoryDB.undoIssueRequest(id,req);
      }else{
        await setStatus(id,'pending',{skipConfirm:true});
        ok=true;
      }
      try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const x=l.find(y=>String(y.id)===String(id)); if(x)x.status='pending'; localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
      STATE.data=null;
      try{await loadData(true);}catch(e){}
      await loadRequests(); await renderLoans(); renderMaster(); renderInventory();
      toast(ok?'Izdavanje je poništeno.':'Zahtjev je vraćen, ali provjeri stanje opreme.');
    }catch(e){
      console.warn('[armory master] undo issue failed',e);
      toast('Poništavanje nije uspjelo. Provjeri stanje ručno.');
    }
  }
  function openReturn(id){
    const r=(STATE.requests||[]).find(x=>String(x.id)===String(id)); if(!r){toast('Ne mogu naći posudbu.');return;}
    const items=(r.items||[]); if(!items.length){toast('Zahtjev nema stavke.');return;}
    const html=`<div class="cm-modal-backdrop" id="returnModal"><div class="cm-modal"><div class="cm-modal-head"><div><h2>Vrati opremu</h2><p class="muted">Upiši koliko je stvarno vraćeno. Ako nešto ostaje vani, zahtjev ide u djelomični povrat.</p></div><button class="cm-icon-btn" onclick="CleanArmory.closeReturn()">×</button></div><div class="return-list">${items.map((it,idx)=>{const q=countInt(it.quantity,1);return `<div class="return-row" data-idx="${idx}" data-id="${esc(it.id||'')}" data-name="${esc(it.name||it.item_name||'Artikl')}" data-qty="${q}"><div><b>${esc(it.name||it.item_name||'Artikl')}</b><div class="muted">Izdano: ${q}</div></div><label>Vraćeno<input class="cm-input return-qty" type="number" min="0" max="${q}" value="${q}"></label><label>Gdje ide vraćeno<select class="cm-input return-dest"><option>Oružarstvo</option><option>U jami / teren</option><option>Kod nekoga drugoga</option><option>Rashod</option></select></label><label>Ako nije sve vraćeno, ostatak je<select class="cm-input remain-dest"><option>Kod posuđivača</option><option>U jami / teren</option><option>Kod nekoga drugoga</option><option>Izgubljeno</option><option>Rashod</option></select></label></div>`}).join('')}</div><textarea class="cm-input" id="returnNote" placeholder="Napomena, npr. dio ostao u jami / kod koga je ostatak"></textarea><div class="cm-tools"><button class="cm-btn" onclick="CleanArmory.closeReturn()">Odustani</button><button class="cm-btn primary" onclick="CleanArmory.confirmReturn('${esc(id)}')">Vrati opremu</button></div></div></div>`;
    document.body.insertAdjacentHTML('beforeend',html);
  }
  function closeReturn(){const m=document.getElementById('returnModal'); if(m)m.remove();}
  async function confirmReturn(id){
    const r=(STATE.requests||[]).find(x=>String(x.id)===String(id));
    const rows=[...document.querySelectorAll('#returnModal .return-row')].map(row=>{const issued=countInt(row.dataset.qty,0); const returned=countInt(row.querySelector('.return-qty').value,0); const ok=Math.min(Math.max(returned,0),issued); return {id:row.dataset.id||null,name:row.dataset.name,issued_quantity:issued,returned_quantity:ok,missing_quantity:Math.max(issued-ok,0),return_location:row.querySelector('.return-dest').value,remaining_location:row.querySelector('.remain-dest').value};});
    const full=rows.every(x=>x.missing_quantity===0); const status=full?'returned':'partial_return'; const note=document.getElementById('returnNote')?.value||'';
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.returnRequestItems) await SOVArmoryDB.returnRequestItems(id,rows,note,r); else await setStatus(id,status); }
    catch(e){console.warn('[armory master v5.47.3] return failed',e); toast('Povrat nije uspio online; spremam lokalno.');}
    try{const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); const x=l.find(y=>String(y.id)===String(id)); if(x){x.status=status;x.return_items=rows;x.return_note=note;} localStorage.setItem('sov_equipment_requests',JSON.stringify(l));}catch(e){}
    closeReturn(); await loadRequests(); await renderLoans(); renderMaster(); toast(full?'Sve vraćeno':'Djelomični povrat spremljen');
  }
  async function manualLoan(ev){
    ev.preventDefault(); const req={id:'REQ-'+Date.now(),created_at:new Date().toISOString(),user:document.getElementById('mUser').value||'Član',trip:document.getElementById('mNote').value||'Ručni unos',status:'pending',items:[{id:'manual',name:document.getElementById('mItem').value||'Artikl',quantity:Number(document.getElementById('mQty').value)||1}]};
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.createRequest){const saved=await SOVArmoryDB.createRequest(req); req.id=saved.id||req.id; req.created_at=saved.created_at||req.created_at; req.status=saved.status||req.status; }}catch(e){console.warn(e)}
    const l=JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]'); l.unshift(req); localStorage.setItem('sov_equipment_requests',JSON.stringify(l)); await loadRequests(); await renderLoans(); renderMaster(); toast('Zahtjev dodan');
  }

  function getRow(id){return (STATE.rows||[]).find(r=>String(r.id)===String(id));}
  function openItemModal(r){
    const isNew=!r; r=r||{id:'NEW-'+Date.now(),name:'',category:STATE.cat||'Ostalo',subcategory:STATE.sub||'Ostalo',qty:0,av:0,loan:0,location:'Oružarstvo',minimum:0,status:'aktivno',type:'item'};
    const html=`<div class="cm-modal-backdrop" id="itemModal"><div class="cm-modal"><div class="cm-modal-head"><div><h2>${isNew?'Dodaj artikl':'Uredi artikl'}</h2><p class="muted">Ovdje možeš dodati novu opremu ili ispraviti podatke.</p></div><button class="cm-icon-btn" onclick="CleanArmory.closeItemModal()">×</button></div><form class="cm-form" onsubmit="CleanArmory.saveItem(event,'${esc(r.id)}',${isNew})"><div class="cm-form-grid"><input class="cm-input" id="itemName" placeholder="Naziv artikla" value="${esc(r.name)}"><input class="cm-input" id="itemCat" placeholder="Kategorija" value="${esc(r.category)}"><input class="cm-input" id="itemSub" placeholder="Podkategorija" value="${esc(r.subcategory)}"></div><div class="cm-form-grid"><input class="cm-input" id="itemQty" type="number" min="0" placeholder="Ukupno" value="${esc(r.qty)}"><input class="cm-input" id="itemAv" type="number" min="0" placeholder="Dostupno" value="${esc(r.av)}"><input class="cm-input" id="itemMin" type="number" min="0" placeholder="Crveni prag" value="${esc(r.minimum||0)}"></div><div class="cm-form-grid"><input class="cm-input" id="itemLoc" placeholder="Lokacija" value="${esc(r.location||'Oružarstvo')}"><input class="cm-input" id="itemCode" placeholder="Opcionalni kod / napomena" value="${esc(r.raw&&r.raw.physical_code_note||'')}"><input class="cm-input" id="itemStatus" placeholder="Status" value="${esc(r.status||'aktivno')}"></div><div class="cm-tools"><button class="cm-btn" type="button" onclick="CleanArmory.closeItemModal()">Odustani</button><button class="cm-btn primary">Spremi</button></div></form></div></div>`;
    document.body.insertAdjacentHTML('beforeend',html);
  }
  function closeItemModal(){const m=document.getElementById('itemModal'); if(m)m.remove();}
  async function saveItem(ev,id,isNew){
    ev.preventDefault(); const row=getRow(id)||{}; const legacy=isNew?('ART-'+Date.now()):(row.raw&&row.raw.legacy_id)||id;
    const payload={legacy_id:cleanLegacyId(legacy),catalog_id:cleanLegacyId(legacy),name:document.getElementById('itemName').value.trim()||'Artikl',category_name:document.getElementById('itemCat').value.trim()||'Ostalo',subcategory:document.getElementById('itemSub').value.trim()||'Ostalo',quantity:countInt(document.getElementById('itemQty').value,0),available:countInt(document.getElementById('itemAv').value,0),loaned:Math.max(0,countInt(document.getElementById('itemQty').value,0)-countInt(document.getElementById('itemAv').value,0)),minimum:countInt(document.getElementById('itemMin').value,0),location_name:document.getElementById('itemLoc').value.trim()||'Oružarstvo',status:document.getElementById('itemStatus').value.trim()||'aktivno',physical_code_note:document.getElementById('itemCode').value.trim()||null,item_kind:'quantity_article',code_required:false,member_visible:true};
    try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.upsertSimpleItem) await SOVArmoryDB.upsertSimpleItem(payload); else throw new Error('no supabase helper'); }
    catch(e){ console.warn('[armory master v5.47.3] save item local fallback',e); const local=JSON.parse(localStorage.getItem('sov_armory_items_override')||'[]'); const i=local.findIndex(x=>String(x.legacy_id)===String(payload.legacy_id)); if(i>=0)local[i]=payload; else local.unshift(payload); localStorage.setItem('sov_armory_items_override',JSON.stringify(local)); }
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
  async function exportInventoryXls(){await loadData(); const date=new Date().toISOString().slice(0,10); const sheets=rowsByCategory().map(([cat,rows])=>{const header=`<tr><th colspan="9" class="head">Inventar — ${xmlEsc(cat)} — ${xmlEsc(date)}</th></tr><tr><th>Kategorija</th><th>Podkategorija</th><th>Naziv</th><th>Količina</th><th>Jedinica</th><th>Datum evidencije</th><th>Lokacija</th><th>Status</th><th>Napomena / detalji</th></tr>`; const body=rows.sort((a,b)=>(a.subcategory+a.name).localeCompare(b.subcategory+b.name,'hr')).map(r=>`<tr>${xlsCell(r.category)}${xlsCell(r.subcategory)}${xlsCell(r.name)}${xlsCell(r.needsCount?(r.qtyLabel||''):r.qty,'num')}${xlsCell(r.unit)}${xlsCell(r.lastInventoryDate||'')}${xlsCell(r.location||'Oružarstvo')}${xlsCell(r.status||'aktivno')}${xlsCell((r.raw&&(r.raw.physical_code_note||r.raw.note||r.raw.internal_note||r.raw.sku))||'')}</tr>`).join(''); return {name:cat,html:header+body};}); if(!sheets.length){toast('Nema inventara za preuzimanje.');return;} xlsWorkbook(`SOV_inventar_${date}.xls`,sheets); toast('Inventar je preuzet.');}
  async function exportInventuraXls(){await loadData(); const date=(document.querySelector('input[type="date"]')?.value)||new Date().toISOString().slice(0,10); const sheets=rowsByCategory().map(([cat,rows])=>{const header=`<tr><th colspan="10" class="head">Inventura — ${xmlEsc(cat)} — ${xmlEsc(date)}</th></tr><tr><th>Kategorija</th><th>Podkategorija</th><th>Naziv</th><th>Broj u bazi</th><th>Jedinica</th><th>Stvarno prebrojano</th><th>Razlika</th><th>Lokacija</th><th>Za rashod?</th><th>Napomena</th></tr>`; const body=rows.sort((a,b)=>(a.subcategory+a.name).localeCompare(b.subcategory+b.name,'hr')).map(r=>`<tr>${xlsCell(r.category)}${xlsCell(r.subcategory)}${xlsCell(r.name)}${xlsCell(r.needsCount?(r.qtyLabel||''):r.qty,'num')}${xlsCell(r.unit)}${xlsCell('')}${xlsCell('')}${xlsCell(r.location||'Oružarstvo')}${xlsCell('')}${xlsCell(r.needsCount?'prebrojiti':'')}</tr>`).join(''); return {name:cat,html:header+body};}); if(!sheets.length){toast('Nema inventara za preuzimanje.');return;} xlsWorkbook(`SOV_inventura_${date}.xls`,sheets); toast('Inventura je preuzeta.');}


  function findRowById(id){return (STATE.rows||[]).find(r=>String(r.id)===String(id))||null;}
  function cleanLegacyId(v){ return String(v||'').replace(/^item:/i,'').trim(); }
  function legacyReturnCss(){
    if(document.getElementById('legacy-return-v6136-css'))return;
    const css=document.createElement('style'); css.id='legacy-return-v6136-css';
    css.textContent='.cm-card-button{text-align:left;cursor:pointer;color:inherit;font:inherit}.legacy-return-panel{border:1px solid rgba(124,255,155,.20);background:rgba(124,255,155,.055)}.legacy-return-panel h2{margin:0 0 6px}.legacy-return-found{border:1px solid rgba(255,255,255,.11);background:rgba(255,255,255,.055);border-radius:16px;padding:11px;margin:8px 0}.legacy-return-mode{display:grid;gap:8px}.legacy-return-mode label{display:flex;gap:8px;align-items:center;font-weight:850;color:#dce9e4}.legacy-return-help{font-size:12px;color:#9fb4ae;line-height:1.45}.legacy-return-modal .cm-form-grid{align-items:end}.legacy-return-modal textarea{min-height:84px}';
    document.head.appendChild(css);
  }
  function openLegacyReturn(rowId){
    legacyReturnCss();
    const r=rowId?findRowById(rowId):null;
    const known=!!r;
    const cat=r? r.category : (STATE.cat||'Za provjeru');
    const sub=r? r.subcategory : (STATE.sub||'');
    const itemName=r? r.name : '';
    const unit=r? r.unit : 'kom';
    const legacy=r? cleanLegacyId(r.raw&&(r.raw.legacy_id||r.raw.catalog_id||r.raw.source_id||r.raw.id)||r.id) : '';
    const html=`<div class="cm-modal-backdrop legacy-return-modal" id="legacyReturnModal"><div class="cm-modal"><div class="cm-modal-head"><div><h2>Zaprimanje / povrat bez otvorene posudbe</h2><p class="muted">Za opremu koja se vratila, a nije bila uredno zadužena.</p></div><button class="cm-icon-btn" onclick="CleanArmory.closeLegacyReturn()">×</button></div><form class="cm-form" onsubmit="CleanArmory.submitLegacyReturn(event,'${esc(rowId||'')}')"><div class="legacy-return-mode"><label><input type="radio" name="lrMode" value="existing" ${known?'checked':''}> Postojeći artikl iz kataloga</label><label><input type="radio" name="lrMode" value="new" ${known?'':'checked'}> Nova / nejasna oprema, dodaj kao za provjeru</label></div>${known?`<div class="legacy-return-found"><b>${esc(itemName)}</b><div class="muted">${esc(cat)} · ${esc(sub||'Ostalo')} · ${esc(legacy)}</div></div>`:''}<div class="cm-form-grid"><input class="cm-input" id="lrItemName" placeholder="Naziv opreme" value="${esc(itemName)}"><input class="cm-input" id="lrQty" type="number" min="1" step="1" value="1"><select class="cm-input" id="lrCondition"><option value="ok">OK / raspoloživo</option><option value="za_provjeru">Za provjeru</option><option value="damaged">Oštećeno</option></select></div><div class="cm-form-grid"><input class="cm-input" id="lrCategory" placeholder="Kategorija" value="${esc(cat)}"><input class="cm-input" id="lrSubcategory" placeholder="Podkategorija" value="${esc(sub)}"><input class="cm-input" id="lrUnit" placeholder="Jedinica" value="${esc(unit)}"></div><div class="cm-form-grid"><input class="cm-input" id="lrSource" placeholder="Od koga / izvor, opcionalno"><input class="cm-input" id="lrLocation" placeholder="Lokacija" value="Oružarstvo Klaićeva"></div><textarea class="cm-input" id="lrNote" placeholder="Napomena, npr. vraćeno iz stare posudbe, provjeriti stanje..."></textarea><p class="legacy-return-help">Ako odabereš “Za provjeru” ili “Oštećeno”, količina se evidentira kao fizički vraćena, ali ne povećava dostupno stanje za izdavanje.</p><div class="cm-tools"><button class="cm-btn" type="button" onclick="CleanArmory.closeLegacyReturn()">Odustani</button><button class="cm-btn primary" id="lrSubmitBtn">Spremi</button></div></form></div></div>`;
    document.body.insertAdjacentHTML('beforeend',html);
  }

  function applyLegacyReturnOptimistic(rowId,res,payload){
    try{
      const r=rowId?findRowById(rowId):null;
      if(!r) return;
      const q=Number((res&&res.quantity_added) ?? (payload&&payload.quantity) ?? 0)||0;
      const av=Number((res&&res.available_added) ?? ((String(payload&&payload.condition_status||'ok').toLowerCase()==='ok')?q:0))||0;
      r.qty=Math.max(0,(Number(r.qty)||0)+q);
      r.av=Math.max(0,(Number(r.av)||0)+av);
      r.loan=Math.max(0,(Number(r.qty)||0)-(Number(r.av)||0));
      if(r.raw){
        r.raw.quantity=r.qty; r.raw.total_qty=r.qty;
        r.raw.available=r.av; r.raw.available_qty=r.av;
        r.raw.quantity_label=String(r.qty); r.raw.available_label=String(r.av);
        r.raw.updated_at=new Date().toISOString();
      }
    }catch(e){console.warn('[legacy_return] optimistic UI update skipped',e);}
  }
  function closeLegacyReturn(){const m=document.getElementById('legacyReturnModal'); if(m)m.remove();}
  async function submitLegacyReturn(ev,rowId){
    ev.preventDefault();
    const btn=document.getElementById('lrSubmitBtn'); if(btn){btn.disabled=true;btn.textContent='Spremam…';}
    const r=rowId?findRowById(rowId):null;
    const mode=(document.querySelector('input[name="lrMode"]:checked')||{}).value||'new';
    const qty=Number(document.getElementById('lrQty')?.value||1);
    const payload={
      item_id:r&&(r.raw&&r.raw.id),
      equipment_legacy_id:r?cleanLegacyId(r.raw&&(r.raw.legacy_id||r.raw.catalog_id||r.raw.source_id)||r.id):null,
      item_name:document.getElementById('lrItemName')?.value||r?.name||'',
      category_name:document.getElementById('lrCategory')?.value||r?.category||'Za provjeru',
      subcategory:document.getElementById('lrSubcategory')?.value||r?.subcategory||'',
      unit:document.getElementById('lrUnit')?.value||r?.unit||'kom',
      quantity:qty,
      to_location_name:document.getElementById('lrLocation')?.value||'Oružarstvo Klaićeva',
      condition_status:document.getElementById('lrCondition')?.value||'ok',
      source_name:document.getElementById('lrSource')?.value||'Povrat bez otvorene posudbe',
      note:document.getElementById('lrNote')?.value||'',
      client_event_id:'WEB-LEGACY-RETURN-'+Date.now()+'-'+Math.random().toString(16).slice(2)
    };
    try{
      if(!window.SOVArmoryDB||!SOVArmoryDB.configured||!SOVArmoryDB.configured()) throw new Error('Baza trenutno nije dostupna.');
      let res;
      if(mode==='existing' && (payload.item_id||payload.equipment_legacy_id||r)){
        if(!payload.equipment_legacy_id && r) payload.equipment_legacy_id=String(r.id||'');
        res=await SOVArmoryDB.recordLegacyReturn(payload);
      }else{
        res=await SOVArmoryDB.addItemAndLegacyReturn(payload);
      }
      applyLegacyReturnOptimistic(rowId,res,payload);
      toast((res&&res.duplicate)?'Već spremljeno — nije duplirano.':'Povrat stare opreme spremljen.');
      closeLegacyReturn();
      try{
        if(window.SOVArmoryDB&&SOVArmoryDB.loadAllData) await SOVArmoryDB.loadAllData({force:true,strictLive:true});
      }catch(e){console.warn('[legacy_return] strict refresh skipped',e);}
      STATE.data=null; await loadData(true); renderInventory(); renderMaster();
    }catch(e){
      console.error('[legacy_return] save failed',e);
      toast('Spremanje nije uspjelo: '+(e.message||e));
      if(btn){btn.disabled=false;btn.textContent='Spremi';}
    }
  }

  async function loadNotes(){try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadArmoryNotes){const n=await SOVArmoryDB.loadArmoryNotes(); if(Array.isArray(n))return n;} }catch(e){console.warn(e)} try{return JSON.parse(localStorage.getItem('sov_armory_notes')||'[]')}catch(e){return []}}
  async function renderNotes(){const root=document.getElementById('notesRoot'); if(!root)return; const notes=await loadNotes(); const open=notes.filter(n=>!/done|closed|obavljeno/i.test(String(n.status||'open'))); root.innerHTML=`<div class="loan-grid"><section class="cm-panel"><h2>+ Nova bilješka / podsjetnik</h2><form class="cm-form" onsubmit="CleanArmory.saveNote(event)"><input class="cm-input" id="noteTitle" placeholder="Naslov, npr. Nabaviti spitove"><textarea class="cm-input" id="noteBody" placeholder="Detalji / napomena"></textarea><div class="cm-form-grid"><input class="cm-input" id="noteDue" type="date"><select class="cm-input" id="noteType"><option value="todo">Obaviti</option><option value="buy">Nabaviti</option><option value="check">Provjeriti</option></select><select class="cm-input" id="notePriority"><option value="normal">Normalno</option><option value="high">Hitno</option><option value="low">Nisko</option></select></div><button class="cm-btn primary">Spremi podsjetnik</button></form></section><section class="cm-panel"><h2>Bilješke</h2><div class="loan-list">${open.length?open.map(n=>`<div class="loan-row"><div class="loan-row-top"><div><b>${esc(n.title||'Bilješka')}</b><div class="muted">${esc(n.due_date||'bez datuma')} · ${esc(n.note_type||n.type||'todo')} · ${esc(n.priority||'normal')}</div></div><span class="badge ${n.priority==='high'?'bad':'warn'}">${esc(n.status||'open')}</span></div><p>${esc(n.body||n.note||'')}</p><button class="cm-btn primary" onclick="CleanArmory.doneNote('${esc(n.id)}')">Označi obavljeno</button></div>`).join(''):'<div class="empty">Nema otvorenih podsjetnika.</div>'}</div></section></div>`;}
  async function saveNote(ev){ev.preventDefault(); const n={id:'NOTE-'+Date.now(),title:document.getElementById('noteTitle').value||'Bilješka',body:document.getElementById('noteBody').value||'',due_date:document.getElementById('noteDue').value||null,note_type:document.getElementById('noteType').value,priority:document.getElementById('notePriority').value,status:'open',created_at:new Date().toISOString()}; try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.saveArmoryNote){await SOVArmoryDB.saveArmoryNote(n);} }catch(e){console.warn(e)} const l=JSON.parse(localStorage.getItem('sov_armory_notes')||'[]'); l.unshift(n); localStorage.setItem('sov_armory_notes',JSON.stringify(l)); await renderNotes(); toast('Podsjetnik spremljen');}
  async function doneNote(id){try{ if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.doneArmoryNote) await SOVArmoryDB.doneArmoryNote(id);}catch(e){console.warn(e)} const l=JSON.parse(localStorage.getItem('sov_armory_notes')||'[]'); const n=l.find(x=>String(x.id)===String(id)); if(n)n.status='done'; localStorage.setItem('sov_armory_notes',JSON.stringify(l)); await renderNotes(); toast('Označeno obavljeno');}

  async function init(){renderDbLoading(); await loadData(); await loadRequests(); renderKpis(); if(STATE.rows&&STATE.rows.length){renderMaster(); renderInventory();} else {renderDbLoading(); scheduleDbRetry();} await renderLoans(); await renderNotes();}
  window.CleanArmory={init,pickCat(c){STATE.cat=c||null;STATE.sub=null;renderInventory()},pickSub(s){STATE.sub=s||null;renderInventory()},clearSearch(){STATE.query='';STATE.cat=null;STATE.sub=null;const q=document.getElementById('cmSearch'); if(q)q.value=''; renderInventory()},renderLoans,setStatus,manualLoan,newItem,editItem,removeItem,exportInventoryXls,exportInventuraXls,openReturn,closeReturn,confirmReturn,closeItemModal,saveItem,renderNotes,saveNote,doneNote,issueLoan,undoIssueLoan,openLegacyReturn,closeLegacyReturn,submitLegacyReturn};
  document.addEventListener('DOMContentLoaded',init);
})();
