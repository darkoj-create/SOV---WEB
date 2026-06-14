/* SOV Oružarstvo single boot v6.0.7
   Contract: skeleton immediately, static JSON first, Supabase refresh in background.
   This file intentionally owns catalog rendering so older inline recovery fragments cannot leave the page in infinite loading state. */
(function(){
  'use strict';
  const BUILD='6.1.5';
  const STATIC_URL='data/oruzarstvo-data.json?b=607';
  const CACHE_KEY='sov_armory_catalog_cache_v607';
  const OLD_KEYS=['sov_armory_catalog_cache_v605','sov_armory_catalog_cache_v604','sov_armory_catalog_cache_v548','sov_armory_catalog_cache_v548_old','sov_armory_catalog_cache'];
  const STATIC_TIMEOUT=4200;
  const LIVE_TIMEOUT=5500;
  let activeSource='boot';
  let activeCat='';
  let activeSub='';
  let booted=false;
  let liveRefreshing=false;
  const $=id=>document.getElementById(id);
  const esc=s=>String(s??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[m]));
  const plain=s=>String(s||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
  const timeout=(ms,value)=>new Promise(r=>setTimeout(()=>r(value),ms));
  function countData(d){return (Array.isArray(d?.items)?d.items.length:0)+(Array.isArray(d?.ropes)?d.ropes.length:0)+(Array.isArray(d?.pieces)?d.pieces.length:0)+(Array.isArray(d?.raw_app_catalog)?d.raw_app_catalog.length:0)}
  function hasRows(d){return !!d && countData(d)>0;}
  function normalize(d){
    const out={items:[],categories:[],ropes:[],pieces:[],loans:[],inventories:[],inventory_items:[],procurement:[],services:[],disposed:[],lost:[],field:[],locations:[],members:[],summary:{}};
    if(!d||typeof d!=='object')return out;
    for(const k of Object.keys(out)){
      if(Array.isArray(out[k])) out[k]=Array.isArray(d[k])?d[k]:[];
      else out[k]=(d[k]&&typeof d[k]==='object')?d[k]:{};
    }
    // Convert canonical grouped rows into member-catalog items if Supabase live returns raw_app/grouped structure only.
    if(!out.items.length && Array.isArray(d.raw_app_catalog)) out.items=d.raw_app_catalog.map((r,i)=>({
      id:r.app_id||r.id||r.catalog_group_key||('APP-'+i),
      catalog_id:r.catalog_group_key||r.app_id||r.id||('APP-'+i),
      name:r.display_name||r.name||r.item_name||'Artikl',
      category:r.main_category||r.category_name||r.category||'Ostalo',
      category_name:r.main_category||r.category_name||r.category||'Ostalo',
      subcategory:r.subcategory||r.group_name||'Ostalo',
      quantity:Number(r.total_quantity??r.quantity??0)||0,
      available:Number(r.available_quantity??r.available??r.total_quantity??r.quantity??0)||0,
      unit:r.unit||'kom',
      status:r.status||'aktivno',
      availability:r.availability||'dostupno',
      member_visible:r.member_visible!==false,
      internal_note:r.note||r.internal_note||''
    }));
    if(d.summary&&typeof d.summary==='object') out.summary={...d.summary};
    out.summary.source=activeSource;
    return out;
  }
  function saveCache(d,source){try{if(hasRows(d))localStorage.setItem(CACHE_KEY,JSON.stringify({saved_at:Date.now(),source,data:d}));}catch(e){}}
  function readCache(){try{return JSON.parse(localStorage.getItem(CACHE_KEY)||'null');}catch(e){return null;}}
  function clearOldCache(){try{OLD_KEYS.forEach(k=>localStorage.removeItem(k));}catch(e){}}
  function idOf(r){return String(r.id||r.legacy_id||r.catalog_id||r.sku||r.name||Math.random()).replace(/'/g,'');}
  function nameOf(r){return r.display_name||r.name||r.item_name||r.model||'Artikl';}
  function catOf(r){return r.main_category||r.category_name||r.category||'Ostalo';}
  function subOf(r){return r.subcategory||r.group_name||r.type||'Ostalo';}
  function visible(r){return r.member_visible!==false && !/rashod|otpis|izgublj|obris/i.test(String(r.status||''));}
  function available(r){return Number(r.available)>0;}
  function rowText(r){return plain([nameOf(r),catOf(r),subOf(r),r.internal_note,r.note,r.location,r.location_name,r.sku].join(' '));}
  function rows(){
    const d=window.DATA||{};
    const items=(Array.isArray(d.items)?d.items:[]).map(x=>({...x,item_type:x.item_type||'item'}));
    const ropes=(Array.isArray(d.ropes)?d.ropes:[]).map(r=>({
      ...r,
      id:'ROPE-'+(r.id||r.legacy_id||r.sku||r.name),
      item_type:'rope',
      name:r.name||('Uže '+(r.sku||'')),
      category:r.category||r.category_name||'Užeta',
      category_name:r.category_name||r.category||'Užeta',
      subcategory:r.subcategory||[r.diameter_mm?`Promjer ${r.diameter_mm} mm`:'',r.length_m?`Duljina ${r.length_m} m`:r.length_label?`Duljina ${r.length_label} m`:'' ].filter(Boolean).join(' · ')||'Užad',
      available:/društvu|drustvu|aktiv|dostup/i.test(String(r.status||''))?1:0,
      unit:'kom'
    }));
    const pieces=(Array.isArray(d.pieces)?d.pieces:[]).map(p=>({
      ...p,
      id:'PIECE-'+(p.id||p.legacy_id||p.sku||p.name),
      item_type:'piece',
      name:p.name||p.model||p.sku||'Komad opreme',
      category:p.category||p.category_name||'Inventarna oprema',
      category_name:p.category_name||p.category||'Inventarna oprema',
      subcategory:p.subcategory||p.location_name||'Pojedinačno',
      available:/društvu|drustvu|aktiv|dostup/i.test(String(p.status||''))?1:0,
      unit:'kom'
    }));
    return [...items,...ropes,...pieces];
  }
  function categoryMeta(c){const x=plain(c); if(x.includes('uze'))return ['🪢','Užeta']; if(x.includes('osob'))return ['🧗','Osobna oprema']; if(x.includes('postavlj'))return ['🪛','Oprema za postavljanje']; if(x.includes('prosir'))return ['⛏️','Oprema za proširivanje']; if(x.includes('elektro')||x.includes('foto')||x.includes('rasv'))return ['💡','Elektro i foto oprema']; if(x.includes('crtan')||x.includes('mjeren')||x.includes('kompas')||x.includes('busol'))return ['🧭','Oprema za crtanje']; if(x.includes('dron'))return ['🚁','Dronovi i dodaci']; if(x.includes('alat'))return ['🧰','Ostali alat']; return ['📦','Oprema'];}
  function sortCats(a){const order=['Osobna oprema','Oprema za postavljanje','Čisto podzemlje','Oprema za crtanje','Oprema za proširivanje','Elektro i foto oprema','Alpinistička oprema','Ronilačka oprema','Ostali alat','Užeta','Oprema za logor','Medicinska oprema','Ostalo']; return a.sort((x,y)=>(order.indexOf(x)<0?99:order.indexOf(x))-(order.indexOf(y)<0?99:order.indexOf(y))||String(x).localeCompare(String(y),'hr'));}
  function currentItems(){const q=plain($('q')?.value||''); const cat=$('cat')?.value||activeCat; const av=$('avail')?.value||''; return rows().filter(visible).filter(r=>!q||rowText(r).includes(q)).filter(r=>!cat||catOf(r)===cat).filter(r=>!activeSub||subOf(r)===activeSub).filter(r=>!av||(av==='dostupno'?available(r):(av==='nedostupno'?!available(r):true)));}
  function statusLine(total,kind='ok'){return `<div class="v607-status-line"><span class="v607-pill ${kind}">${esc(activeSource)} · ${total} stavki</span><span class="v607-pill">single boot ${BUILD}</span><span class="v607-pill">static first · live background</span></div>`;}
  function setMiniStatus(kind,msg){const el=$('armoryBootStatus')||document.querySelector('[data-armory-boot-status]'); if(el)el.textContent=msg; const line=$('v607StatusText'); if(line)line.textContent=msg;}
  function renderFilters(){const sel=$('cat'); if(!sel)return; const cur=sel.value||activeCat; const cats=sortCats([...new Set(rows().filter(visible).map(catOf).filter(Boolean))]); sel.innerHTML='<option value="">Sve kategorije</option>'+cats.map(c=>`<option value="${esc(c)}">${esc(c)}</option>`).join(''); sel.value=cur;}
  function renderCatalog(){
    const el=$('catalog'); if(!el)return;
    const all=rows().filter(visible); const q=plain($('q')?.value||''); const cat=$('cat')?.value||activeCat;
    if(!all.length){el.innerHTML=`<div class="v607-empty"><b>Katalog nije učitan.</b><small>Provjeri da je na deployu prisutan <code>data/oruzarstvo-data.json</code>. Stranica više ne čeka Supabase beskonačno.</small></div>`; return;}
    const status=statusLine(all.length);
    if(!q&&!cat&&!activeSub){
      const cats=sortCats([...new Set(all.map(catOf).filter(Boolean))]);
      el.innerHTML=status+`<div class="user-catalog-head"><div><h2>Što trebaš?</h2><p>Katalog se učita lokalno odmah. Supabase samo osvježava stanje u pozadini.</p></div><button class="btn primary" onclick="document.getElementById('drawer')?.classList.add('open')">Moj zahtjev</button></div><div class="user-category-grid">${cats.map(c=>{const meta=categoryMeta(c); const rs=all.filter(r=>catOf(r)===c); const subs=new Set(rs.map(subOf)); return `<button class="user-category-card" onclick="sovArmory606.pickCat('${esc(c)}')"><span class="ico">${meta[0]}</span><b>${esc(c)}</b><small>${esc(meta[1])}</small><small class="v607-card-count">${rs.length} stavki · ${subs.size} podkategorija</small></button>`}).join('')}</div>`;
      return;
    }
    const base=rows().filter(visible).filter(r=>!q||rowText(r).includes(q)).filter(r=>!cat||catOf(r)===cat);
    if(cat&&!activeSub&&!q){
      const subs=[...new Set(base.map(subOf).filter(Boolean))].sort((a,b)=>String(a).localeCompare(String(b),'hr'));
      el.innerHTML=status+`<div class="user-path"><button onclick="sovArmory606.clear()">Sve kategorije</button><button class="active">${esc(cat)}</button></div><div class="v607-sub-head"><div><h2>${esc(cat)}</h2><p class="fineprint">Odaberi podkategoriju ili kreni pretraživati unutar kategorije.</p></div><button class="btn" onclick="sovArmory606.clear()">← Kategorije</button></div><div class="user-subcat-list">${subs.map(s=>`<button class="user-subcat-card" onclick="sovArmory606.pickSub('${esc(s)}')">${esc(s)}<small>${base.filter(r=>subOf(r)===s).length} stavki</small></button>`).join('')}</div>`;
      return;
    }
    const items=currentItems(); const title=activeSub||cat||(q?'Rezultati':'Oprema');
    el.innerHTML=status+`<div class="user-path"><button onclick="sovArmory606.clear()">Sve kategorije</button>${cat?`<button onclick="sovArmory606.backToCat()">${esc(cat)}</button>`:''}${activeSub?`<button class="active">${esc(activeSub)}</button>`:''}</div><div class="v607-sub-head"><div><h2>${esc(title)}</h2><p class="fineprint">Klikni Zatraži. Količinu i napomenu urediš u zahtjevu.</p></div><button class="btn" onclick="sovArmory606.back()">← Nazad</button></div>${items.length?`<div class="grid-list">${items.map(r=>{const c=catOf(r),s=subOf(r),m=categoryMeta(c),ok=available(r);return `<article class="item-card"><div class="meta"><span class="badge">${m[0]} ${esc(c)}</span><span class="badge">${esc(s)}</span></div><h3>${esc(nameOf(r))}</h3>${r.internal_note||r.note?`<p class="fineprint">${esc(String(r.internal_note||r.note)).slice(0,180)}</p>`:''}<div class="actions"><button class="btn primary" ${ok?'':'disabled'} onclick="sovArmory606.add('${esc(idOf(r))}')">${ok?'Zatraži':'Nedostupno'}</button></div></article>`}).join('')}</div>`:'<div class="v607-empty">Nema artikala za ovaj izbor.</div>'}`;
  }
  function applyData(d,source){if(!hasRows(d))return false; activeSource=source; const norm=normalize(d); try{window.DATA=norm; DATA=norm;}catch(e){window.DATA=norm;} renderFilters(); try{if(typeof renderKpis==='function')renderKpis();}catch(e){} renderCatalog(); try{if(typeof renderRequest==='function')renderRequest();}catch(e){} try{if(typeof renderMyRequests==='function')renderMyRequests();}catch(e){} saveCache(norm,source); setMiniStatus('ok',`${source} · ${countData(norm)} stavki · ${BUILD}`); booted=true; return true;}
  async function fetchJson(url,ms){const ctrl=new AbortController(); const t=setTimeout(()=>ctrl.abort(),ms); try{const r=await fetch(url,{cache:'no-store',signal:ctrl.signal}); if(!r.ok)throw new Error('HTTP '+r.status); return await r.json();} finally{clearTimeout(t);}}
  async function loadStatic(){try{return await fetchJson(STATIC_URL,STATIC_TIMEOUT);}catch(e){console.warn('SOV armory v607 static failed',e?.message||e); return null;}}
  async function loadLive(){ if(liveRefreshing)return; liveRefreshing=true; try{ if(!(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadAllData))return; const live=await Promise.race([SOVArmoryDB.loadAllData({force:true}),timeout(LIVE_TIMEOUT,{__timeout:true})]); if(live&&live.__timeout){setMiniStatus('warn',`Statički katalog radi · Supabase timeout nakon ${LIVE_TIMEOUT/1000}s`);return;} if(hasRows(live)) applyData(live,'Supabase live'); }catch(e){console.warn('SOV armory v607 live skipped',e?.message||e); setMiniStatus('warn','Statički katalog radi · Supabase nije osvježen');} finally{liveRefreshing=false;} }
  async function loadRequests(){try{ if(typeof loadRequestsFromBackend==='function') await Promise.race([loadRequestsFromBackend(),timeout(2500,null)]); if(typeof renderMyRequests==='function')renderMyRequests();}catch(e){console.warn('SOV armory v607 requests skipped',e?.message||e);} }
  async function authLight(){try{if(window.SOVAuth&&SOVAuth.getProfile){const p=await Promise.race([SOVAuth.getProfile(),timeout(2200,null)]); if(p){window.CURRENT_USER=p; if(p.role)window.CURRENT_ROLE=p.role; if(p.permissions)window.CURRENT_CAN_ARMORY=!!p.permissions.can_manage_equipment;}} if(typeof applyRole==='function')applyRole(); if(window.SOVAuth&&SOVAuth.renderUserBadge)SOVAuth.renderUserBadge();}catch(e){}}
  function attach(){
    const q=$('q'); if(q)q.oninput=()=>{activeSub=''; renderCatalog();};
    const cat=$('cat'); if(cat)cat.oninput=()=>{activeCat=cat.value; activeSub=''; renderCatalog();};
    const av=$('avail'); if(av)av.oninput=()=>renderCatalog();
    const open=$('openRequest'); if(open)open.onclick=()=>$('drawer')?.classList.add('open');
    const close=$('closeDrawer'); if(close)close.onclick=()=>$('drawer')?.classList.remove('open');
    const send=$('sendRequest'); if(send&&typeof sendRequest==='function')send.onclick=sendRequest;
    document.querySelectorAll('.tab').forEach(b=>{b.onclick=()=>{try{if(typeof switchTab==='function')switchTab(b.dataset.tab); if(b.dataset.tab==='catalog')setTimeout(renderCatalog,0);}catch(e){}}});
    window.renderCatalog=renderCatalog;
  }
  async function boot(){
    const el=$('catalog'); if(el)el.innerHTML='<div class="v607-load-note"><b>Učitavam lokalni katalog opreme…</b><small>Ovaj boot ne čeka Supabase, request tablice ni stare fallback skripte.</small></div>';
    clearOldCache(); attach();
    const cached=readCache(); if(cached&&hasRows(cached.data))applyData(cached.data,'Cache v607');
    const stat=await loadStatic();
    if(stat&&hasRows(stat)) applyData(stat,'Statički katalog');
    else if(!booted) renderCatalog();
    authLight().then(()=>setTimeout(renderCatalog,0));
    setTimeout(loadRequests,120);
    setTimeout(loadLive,300);
  }
  window.sovArmory606={
    boot, render:renderCatalog,
    pickCat(c){activeCat=c; activeSub=''; const sel=$('cat'); if(sel)sel.value=c; renderCatalog();},
    pickSub(s){activeSub=s; renderCatalog();},
    clear(){activeCat=''; activeSub=''; const sel=$('cat'); if(sel)sel.value=''; const q=$('q'); if(q)q.value=''; renderCatalog();},
    backToCat(){activeSub=''; renderCatalog();},
    back(){if(activeSub)activeSub=''; else {activeCat=''; const sel=$('cat'); if(sel)sel.value='';} renderCatalog();},
    add(id){try{if(typeof addToCart==='function')return addToCart(id);}catch(e){} const item=rows().find(r=>idOf(r)===id); if(!item)return; try{window.CART=window.CART||[]; CART=window.CART||[]; CART.push({id:idOf(item),name:nameOf(item),quantity:1,type:item.item_type||'item'}); if(typeof renderRequest==='function')renderRequest(); $('drawer')?.classList.add('open');}catch(e){console.warn(e);}}
  };
  window.addEventListener('sov-armory-catalog-refreshed',ev=>{try{if(ev.detail&&hasRows(ev.detail))applyData(ev.detail,'Supabase live event');}catch(e){}});
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,20)); else setTimeout(boot,20);
  setTimeout(()=>{ if(!booted) boot(); else {attach(); renderCatalog();} },1400);
})();
