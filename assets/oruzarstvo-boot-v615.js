/* SOV Oružarstvo DB gate v6.1.6
   Contract: do not render any inventory/catalog rows until Supabase returns a real catalog.
   Static JSON/cache are deliberately not displayed during DB seed/import. */
(function(){
  'use strict';
  const BUILD='6.1.39h-user-catalog-rows-fix';
  const MIN_ROWS=20;
  const LIVE_TIMEOUT=65000;
  const RETRY_MS=4500;
  let activeSource='Čekam Supabase';
  let activeCat='';
  let activeSub='';
  let booted=false;
  let loading=true;
  let retryTimer=null;
  let attempt=0;
  const $=id=>document.getElementById(id);
  const esc=s=>String(s??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[m]));
  const plain=s=>String(s||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase().trim();
  const timeout=(ms,value)=>new Promise(r=>setTimeout(()=>r(value),ms));
  function injectCss(){
    if(document.getElementById('armory-db-gate-v616-css'))return;
    const css=document.createElement('style'); css.id='armory-db-gate-v616-css';
    css.textContent=`.armory-db-loading{border:1px solid rgba(215,246,111,.25);border-radius:26px;padding:24px;background:linear-gradient(135deg,rgba(215,246,111,.10),rgba(141,216,255,.055));box-shadow:0 18px 60px rgba(0,0,0,.25);display:grid;gap:13px;color:#eef7f3}.armory-db-loading h2{margin:0;font-size:24px;letter-spacing:-.03em}.armory-db-loading p{margin:0;color:#b9cbc5;line-height:1.55}.armory-db-bar{height:11px;border-radius:999px;background:rgba(255,255,255,.10);overflow:hidden;position:relative}.armory-db-bar:before{content:"";position:absolute;inset:0;width:42%;border-radius:999px;background:linear-gradient(90deg,#d7f66f,#7ff0b2,#8dd8ff);animation:armoryDbLoad 1.25s ease-in-out infinite}.armory-db-pills{display:flex;gap:8px;flex-wrap:wrap}.armory-db-pill{border:1px solid rgba(255,255,255,.13);background:rgba(255,255,255,.055);border-radius:999px;padding:7px 10px;font-size:12px;font-weight:950;color:#dce9e4}.armory-db-pill.warn{border-color:rgba(255,211,107,.3);background:rgba(255,211,107,.1);color:#ffe7a6}@keyframes armoryDbLoad{0%{transform:translateX(-100%)}50%{transform:translateX(70%)}100%{transform:translateX(250%)}}.user-category-card{min-height:176px!important;display:flex!important;flex-direction:column!important;gap:8px!important;overflow:hidden!important}.user-category-card .ico{width:58px!important;height:58px!important;min-width:58px!important;border-radius:18px!important;display:grid!important;place-items:center!important;font-size:31px!important;line-height:1!important;margin:0 0 4px!important;background:rgba(0,0,0,.16)!important;border:1px solid rgba(255,255,255,.12)!important;overflow:hidden!important}.user-category-card b{font-size:clamp(17px,4.8vw,20px)!important;line-height:1.12!important;overflow-wrap:break-word!important;hyphens:auto!important}.user-category-card small{font-size:12px!important;line-height:1.28!important}.user-subcat-card{overflow-wrap:break-word!important;line-height:1.18!important}.user-path button{max-width:100%;overflow-wrap:break-word!important}`;
    document.head.appendChild(css);
  }
  function countData(d){return (Array.isArray(d?.items)?d.items.length:0)+(Array.isArray(d?.ropes)?d.ropes.length:0)+(Array.isArray(d?.pieces)?d.pieces.length:0)+(Array.isArray(d?.raw_app_catalog)?d.raw_app_catalog.length:0)}
  function hasRows(d){return !!d && countData(d)>=MIN_ROWS;}
  function normalize(d){
    const out={items:[],categories:[],ropes:[],pieces:[],loans:[],inventories:[],inventory_items:[],procurement:[],services:[],disposed:[],lost:[],field:[],locations:[],members:[],subcategory_meta:[],summary:{}};
    if(!d||typeof d!=='object')return out;
    for(const k of Object.keys(out)){
      if(Array.isArray(out[k])) out[k]=Array.isArray(d[k])?d[k]:[];
      else out[k]=(d[k]&&typeof d[k]==='object')?d[k]:{};
    }
    if(!out.items.length && Array.isArray(d.raw_app_catalog)) out.items=d.raw_app_catalog.map((r,i)=>({
      id:r.app_id||r.id||r.catalog_group_key||('APP-'+i),
      catalog_id:r.catalog_group_key||r.app_id||r.id||('APP-'+i),
      name:r.display_name||r.name||r.item_name||'Artikl',
      category:r.category_name||r.main_category||r.category||'Ostalo',
      category_name:r.category_name||r.main_category||r.category||'Ostalo',
      subcategory:r.subcategory||r.group_name||'Ostalo',
      quantity:Number(r.total_quantity??r.total_qty??r.quantity??0)||0,
      available:Number(r.available_quantity??r.available_qty??r.available??r.total_quantity??r.total_qty??r.quantity??0)||0,
      unit:r.unit||'kom', status:r.status||'aktivno', availability:r.availability||'dostupno',
      member_visible:r.member_visible!==false, internal_note:r.note||r.internal_note||''
    }));
    out.summary={...(d.summary||{}),source:activeSource};
    return out;
  }
  function rows(){
    const d=window.DATA||{};
    const lists=[d.items,d.ropes,d.pieces];
    const out=[];
    lists.forEach(list=>{
      if(!Array.isArray(list))return;
      list.forEach((r,i)=>{
        if(!r||typeof r!=='object')return;
        const q=Number(r.quantity??r.total_quantity??r.total_qty??1)||0;
        const av=Number(r.available??r.available_quantity??r.available_qty??q)||0;
        out.push({
          ...r,
          id:r.id||r.legacy_id||r.catalog_id||r.sku||('row-'+out.length+'-'+i),
          name:r.display_name||r.name||r.item_name||r.model||r.sku||'Artikl',
          category:r.category||r.category_name||r.main_category||r.xls_category||'Ostalo',
          category_name:r.category_name||r.category||r.main_category||r.xls_category||'Ostalo',
          subcategory:r.subcategory||r.subcategory_name||r.group_name||r.xls_subcategory||'Ostalo',
          quantity:q,
          available:av,
          unit:r.unit||'kom',
          status:r.status||r.availability||'aktivno',
          member_visible:r.member_visible!==false
        });
      });
    });
    return out;
  }
  function idOf(r){return String(r.id||r.legacy_id||r.catalog_id||r.sku||r.name||Math.random()).replace(/'/g,'');}
  function nameOf(r){return r.display_name||r.name||r.item_name||r.model||'Artikl';}
  function catOf(r){
    const raw=r.category_name||r.main_category||r.category||r.xls_category||'Ostalo';
    return String(raw)==='Užeta'?'Užad':raw;
  }
  function subOf(r){return r.subcategory||r.group_name||r.type||'Ostalo';}
  function nameOf(r){return r.display_name||r.name||r.item_name||r.model||'Artikl';}
  function idOf(r){return String(r.id||r.legacy_id||r.catalog_id||r.sku||r.name||Math.random()).replace(/'/g,'');}
  function categoryRow(c){
    const key=plain(c);
    const cats=(window.DATA&&Array.isArray(window.DATA.categories))?window.DATA.categories:[];
    return cats.find(x=>plain(x.name)===key||plain(x.display_name)===key||plain(x.short_name)===key)||null;
  }
  function subcategoryRow(cat,sub){
    const ck=plain(cat), sk=plain(sub);
    const rows=(window.DATA&&Array.isArray(window.DATA.subcategory_meta))?window.DATA.subcategory_meta:[];
    return rows.find(x=>plain(x.category_name)===ck && plain(x.subcategory_name)===sk)||null;
  }
  function displayCat(c){const row=categoryRow(c); return (row&&(row.display_name||row.short_name))||c||'Ostalo';}
  function displaySub(cat,sub){const row=subcategoryRow(cat,sub); return (row&&(row.display_name||row.short_name))||sub||'Ostalo';}
  function searchTermsFor(r){
    const c=catOf(r), s=subOf(r), cr=categoryRow(c), sr=subcategoryRow(c,s);
    return [displayCat(c),displaySub(c,s),cr&&cr.search_terms,sr&&sr.search_terms].filter(Boolean).join(' ');
  }
  function visible(r){return r.member_visible!==false && !/rashod|otpis|izgublj|obris/i.test(String(r.status||''));}
  function available(r){return Number(r.available)>0;}
  function rowText(r){return plain([nameOf(r),catOf(r),subOf(r),searchTermsFor(r),r.internal_note,r.note,r.location,r.location_name,r.sku].join(' '));}
  function fallbackIcon(c){
    const x=plain(c);
    if(x.includes('srt')||x.includes('osobni'))return '🧗';
    if(x.includes('uzad')||x.includes('uze'))return '🪢';
    if(x.includes('sidrist')||x.includes('opremanj')||x.includes('spit')||x.includes('fix')||x.includes('ploc'))return '⚓';
    if(x.includes('spas')||x.includes('cisto')||x.includes('paw')||x.includes('kolot')||x.includes('nosil'))return '🛟';
    if(x.includes('prosir')||x.includes('busil')||x.includes('svrd')||x.includes('regul'))return '⛏️';
    if(x.includes('mjer')||x.includes('crtan')||x.includes('dokument')||x.includes('topofil')||x.includes('kompas')||x.includes('busol'))return '📐';
    if(x.includes('rasvjet')||x.includes('elektr')||x.includes('komunik')||x.includes('foto')||x.includes('dron'))return '🔦';
    if(x.includes('logor')||x.includes('ekspedic')||x.includes('kuhinj')||x.includes('kamp'))return '⛺';
    if(x.includes('medic')||x.includes('prva pomoc'))return '🧰';
    if(x.includes('alpin')||x.includes('penjac'))return '⛰️';
    if(x.includes('ronil'))return '🤿';
    if(x.includes('alat')||x.includes('odrzav')||x.includes('radion'))return '🛠️';
    return '📦';
  }
  function categoryMeta(c){
    const row=categoryRow(c);
    return [row&&row.icon?row.icon:fallbackIcon(c), row&&row.note?row.note:displayCat(c)];
  }
  function sortCats(a){
    const order=['Osobni SRT komplet','Užad','Sidrišta i opremanje','Tehničko spašavanje i Čisto podzemlje','Proširivanje i regulirana oprema','Mjerenje, crtanje i dokumentacija','Rasvjeta, elektronika i komunikacija','Logor, ekspedicija i kuhinja','Medicinska oprema','Alpinistička i penjačka oprema','Ronilačka oprema','Alat i održavanje','Ostalo'];
    return [...a].sort((x,y)=>(order.indexOf(x)<0?99:order.indexOf(x))-(order.indexOf(y)<0?99:order.indexOf(y))||displayCat(x).localeCompare(displayCat(y),'hr'));
  }
  function currentItems(){const q=plain($('q')?.value||''); const cat=$('cat')?.value||activeCat; const av=$('avail')?.value||''; return rows().filter(visible).filter(r=>!q||rowText(r).includes(q)).filter(r=>!cat||catOf(r)===cat).filter(r=>!activeSub||subOf(r)===activeSub).filter(r=>!av||(av==='dostupno'?available(r):(av==='nedostupno'?!available(r):true)));}
  function setMiniStatus(kind,msg){const el=$('armoryBootStatus')||document.querySelector('[data-armory-boot-status]'); if(el)el.textContent=msg; const line=$('v607StatusText'); if(line)line.textContent=msg;}
  function renderLoading(message, detail){
    loading=true; injectCss();
    const el=$('catalog'); if(!el)return;
    el.innerHTML=`<div class="armory-db-loading"><div class="armory-db-pills"><span class="armory-db-pill warn">DB gate ${BUILD}</span><span class="armory-db-pill">pokušaj ${attempt||1}</span></div><h2>${esc(message||'Punim oružarstvo iz baze…')}</h2><div class="armory-db-bar" aria-hidden="true"></div><p>${esc(detail||'Inventar je namjerno sakriven dok Supabase ne vrati stvarni katalog. Nema lokalnog XLS/cache prikaza i nema demo/fallback opreme.')}</p></div>`;
    setMiniStatus('warn', message||'Čekam bazu');
  }
  function renderFilters(){const sel=$('cat'); if(!sel)return; const cur=sel.value||activeCat; const cats=sortCats([...new Set(rows().filter(visible).map(catOf).filter(Boolean))]); sel.innerHTML='<option value="">Sve kategorije</option>'+cats.map(c=>`<option value="${esc(c)}">${esc(displayCat(c))}</option>`).join(''); sel.value=cur;}
  function statusLine(total){return `<div class="v607-status-line"><span class="v607-pill ok">${esc(activeSource)} · ${total} stavki</span><span class="v607-pill">DB ready · ${BUILD}</span></div>`;}
  function renderCatalog(){
    if(loading){renderLoading('Punim oružarstvo iz baze…','Inventar ostaje sakriven dok se baza ne popuni.'); return;}
    const el=$('catalog'); if(!el)return;
    const all=rows().filter(visible); const q=plain($('q')?.value||''); const cat=$('cat')?.value||activeCat;
    if(!all.length){renderLoading('Baza još ne vraća katalog.','Ne prikazujem statički katalog ni cache.'); return;}
    const status=statusLine(all.length);
    if(!q&&!cat&&!activeSub){
      const cats=sortCats([...new Set(all.map(catOf).filter(Boolean))]);
      el.innerHTML=status+`<div class="user-catalog-head"><div><h2>Što trebaš?</h2><p>Katalog je učitan iz Supabase baze. Nema prikaza prije stvarnog DB stanja.</p></div><button class="btn primary" onclick="document.getElementById('drawer')?.classList.add('open')">Moj zahtjev</button></div><div class="user-category-grid">${cats.map(c=>{const meta=categoryMeta(c); const rs=all.filter(r=>catOf(r)===c); const subs=new Set(rs.map(subOf)); return `<button class="user-category-card" onclick="sovArmory606.pickCat('${esc(c)}')"><span class="ico">${meta[0]}</span><b>${esc(displayCat(c))}</b><small>${esc(meta[1]||displayCat(c))}</small><small class="v607-card-count">${rs.length} stavki · ${subs.size} podkategorija</small></button>`}).join('')}</div>`;
      return;
    }
    const base=rows().filter(visible).filter(r=>!q||rowText(r).includes(q)).filter(r=>!cat||catOf(r)===cat);
    if(cat&&!activeSub&&!q){
      const subs=[...new Set(base.map(subOf).filter(Boolean))].sort((a,b)=>String(a).localeCompare(String(b),'hr'));
      el.innerHTML=status+`<div class="user-path"><button onclick="sovArmory606.clear()">Sve kategorije</button><button class="active">${esc(displayCat(cat))}</button></div><div class="v607-sub-head"><div><h2>${esc(displayCat(cat))}</h2><p class="fineprint">Odaberi podkategoriju ili kreni pretraživati unutar kategorije.</p></div><button class="btn" onclick="sovArmory606.clear()">← Kategorije</button></div><div class="user-subcat-list">${subs.map(s=>`<button class="user-subcat-card" onclick="sovArmory606.pickSub('${esc(s)}')">${esc(displaySub(cat,s))}<small>${base.filter(r=>subOf(r)===s).length} stavki</small></button>`).join('')}</div>`;
      return;
    }
    const items=currentItems(); const title=activeSub?displaySub(cat,activeSub):(cat?displayCat(cat):(q?'Rezultati':'Oprema'));
    el.innerHTML=status+`<div class="user-path"><button onclick="sovArmory606.clear()">Sve kategorije</button>${cat?`<button onclick="sovArmory606.backToCat()">${esc(displayCat(cat))}</button>`:''}${activeSub?`<button class="active">${esc(displaySub(cat,activeSub))}</button>`:''}</div><div class="v607-sub-head"><div><h2>${esc(title)}</h2><p class="fineprint">Klikni Zatraži. Količinu i napomenu možeš urediti u zahtjevu.</p></div><button class="btn" onclick="sovArmory606.back()">← Nazad</button></div>${items.length?`<div class="grid-list">${items.map(r=>{const c=catOf(r),s=subOf(r),m=categoryMeta(c),av=available(r);return `<article class="item-card"><div class="meta"><span class="badge">${m[0]} ${esc(displayCat(c))}</span><span class="badge">${esc(displaySub(c,s))}</span></div><h3>${esc(nameOf(r))}</h3>${r.internal_note||r.note?`<p class="fineprint">${esc(String(r.internal_note||r.note)).slice(0,160)}</p>`:''}<div class="meta"><span class="badge ${av?'ok':'danger'}">${av?'Dostupno':'Nedostupno'}</span>${r.available!==undefined?`<span class="badge">${esc(r.available)} ${esc(r.unit||'kom')}</span>`:''}</div><div class="actions"><button class="btn primary" ${av?'':'disabled'} onclick="sovArmory606.add('${esc(idOf(r))}')">${av?'Zatraži':'Nedostupno'}</button></div></article>`}).join('')}</div>`:'<div class="v607-empty">Nema artikala za ovaj izbor.</div>'}`;
  }
  function applyData(d,source){
    if(!hasRows(d))return false;
    activeSource=source; const norm=normalize(d);
    try{window.DATA=norm; DATA=norm;}catch(e){window.DATA=norm;}
    loading=false; booted=true; clearTimeout(retryTimer); retryTimer=null;
    renderFilters(); try{if(typeof renderKpis==='function')renderKpis();}catch(e){}
    renderCatalog(); try{if(typeof renderRequest==='function')renderRequest();}catch(e){} try{if(typeof renderMyRequests==='function')renderMyRequests();}catch(e){}
    setMiniStatus('ok',`${source} · ${countData(norm)} stavki · ${BUILD}`);
    return true;
  }
  async function loadLiveStrict(){
    if(!(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadAllData)){
      renderLoading('Supabase još nije konfiguriran ili nije spreman.','Inventar je sakriven. Provjeri assets/supabase-config.js i pričekaj deploy.'); return false;
    }
    const live=await Promise.race([SOVArmoryDB.loadAllData({force:true,strictLive:true}),timeout(LIVE_TIMEOUT,{__timeout:true})]);
    if(live&&live.__timeout){renderLoading('Baza se još puni…','Supabase nije vratio katalog unutar timeouta. Ne prikazujem cache ni statiku.');return false;}
    if(hasRows(live)) return applyData(live,'Supabase live');
    renderLoading('Baza još ne vraća inventar.','Čekam da SQL seed/viewovi završe i da se pojavi stvarni katalog.'); return false;
  }
  async function pollLive(){
    attempt += 1; renderLoading('Punim oružarstvo iz baze…','Inventar ostaje sakriven dok Supabase ne vrati stvarni katalog.');
    try{ const ok=await loadLiveStrict(); if(ok){ setTimeout(loadRequests,150); return; } }
    catch(e){console.warn('SOV armory db-gate load failed',e?.message||e); renderLoading('Baza još nije spremna.',String(e?.message||e||'Čekam Supabase.').slice(0,180));}
    retryTimer=setTimeout(pollLive,RETRY_MS);
  }
  async function loadRequests(){try{ if(typeof loadRequestsFromBackend==='function') await Promise.race([loadRequestsFromBackend(),timeout(3000,null)]); if(typeof renderMyRequests==='function')renderMyRequests();}catch(e){console.warn('SOV armory requests skipped',e?.message||e);} }
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
    injectCss(); attach(); renderLoading('Punim oružarstvo iz baze…','Nema prikaza inventara dok baza ne vrati stvarni katalog.');
    authLight().then(()=>{try{if(!loading)renderCatalog();}catch(e){}});
    pollLive();
  }
  window.sovArmory606={
    boot, render:renderCatalog,
    pickCat(c){if(loading)return; activeCat=c; activeSub=''; const sel=$('cat'); if(sel)sel.value=c; renderCatalog();},
    pickSub(s){if(loading)return; activeSub=s; renderCatalog();},
    clear(){if(loading)return; activeCat=''; activeSub=''; const sel=$('cat'); if(sel)sel.value=''; const q=$('q'); if(q)q.value=''; renderCatalog();},
    backToCat(){if(loading)return; activeSub=''; renderCatalog();},
    back(){if(loading)return; if(activeSub)activeSub=''; else {activeCat=''; const sel=$('cat'); if(sel)sel.value='';} renderCatalog();},
    add(id){if(loading)return; try{if(typeof addToCart==='function')return addToCart(id);}catch(e){} const item=rows().find(r=>idOf(r)===id); if(!item)return; try{window.CART=window.CART||[]; CART=window.CART||[]; CART.push({id:idOf(item),name:nameOf(item),quantity:1,type:item.item_type||'item'}); if(typeof renderRequest==='function')renderRequest(); $('drawer')?.classList.add('open');}catch(e){console.warn(e);}}
  };
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,20)); else setTimeout(boot,20);
})();
