/* SOV Oružarstvo catalog loader v6.1.43 */
(function(){
  'use strict';
  const BUILD='6.1.45m-static-fallback-responsive-fix';
  const MIN_ROWS=20;
  const LIVE_TIMEOUT=65000;
  const RETRY_MS=4500;
  let activeSource='Učitavanje evidencije';
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
    css.textContent=`.armory-db-loading{border:1px solid rgba(215,246,111,.25);border-radius:26px;padding:24px;background:linear-gradient(135deg,rgba(215,246,111,.10),rgba(141,216,255,.055));box-shadow:0 18px 60px rgba(0,0,0,.25);display:grid;gap:13px;color:#eef7f3}.armory-db-loading h2{margin:0;font-size:24px;letter-spacing:-.03em}.armory-db-loading p{margin:0;color:#b9cbc5;line-height:1.55}.armory-db-bar{height:11px;border-radius:999px;background:rgba(255,255,255,.10);overflow:hidden;position:relative}.armory-db-bar:before{content:"";position:absolute;inset:0;width:42%;border-radius:999px;background:linear-gradient(90deg,#d7f66f,#7ff0b2,#8dd8ff);animation:armoryDbLoad 1.25s ease-in-out infinite}.armory-db-pills{display:flex;gap:8px;flex-wrap:wrap}.armory-db-pill{border:1px solid rgba(255,255,255,.13);background:rgba(255,255,255,.055);border-radius:999px;padding:7px 10px;font-size:12px;font-weight:950;color:#dce9e4}.armory-db-pill.warn{border-color:rgba(255,211,107,.3);background:rgba(255,211,107,.1);color:#ffe7a6}@keyframes armoryDbLoad{0%{transform:translateX(-100%)}50%{transform:translateX(70%)}100%{transform:translateX(250%)}}`;
    document.head.appendChild(css);
  }
  function countData(d){return (Array.isArray(d?.items)?d.items.length:0)+(Array.isArray(d?.ropes)?d.ropes.length:0)+(Array.isArray(d?.pieces)?d.pieces.length:0)+(Array.isArray(d?.raw_app_catalog)?d.raw_app_catalog.length:0)}
  function hasRows(d){return !!d && countData(d)>=MIN_ROWS;}
  function normalize(d){
    const out={items:[],categories:[],ropes:[],pieces:[],loans:[],inventories:[],inventory_items:[],procurement:[],services:[],disposed:[],lost:[],field:[],locations:[],members:[],summary:{}};
    if(!d||typeof d!=='object')return out;
    for(const k of Object.keys(out)){
      if(Array.isArray(out[k])) out[k]=Array.isArray(d[k])?d[k]:[];
      else out[k]=(d[k]&&typeof d[k]==='object')?d[k]:{};
    }
    if(!out.items.length && Array.isArray(d.raw_app_catalog)) out.items=d.raw_app_catalog.map((r,i)=>({
      id:r.app_id||r.id||r.catalog_group_key||('APP-'+i),
      catalog_id:r.catalog_group_key||r.app_id||r.id||('APP-'+i),
      name:r.display_name||r.name||r.item_name||'Artikl',
      category:r.xls_category||r.raw_category||r.main_category||r.category_name||r.category||'Ostalo',
      category_name:r.xls_category||r.raw_category||r.main_category||r.category_name||r.category||'Ostalo',
      xls_category:r.xls_category||r.raw_category||r.main_category||r.category_name||r.category||'Ostalo',
      subcategory:r.xls_subcategory||r.raw_subcategory||r.subcategory||r.group_name||'Ostalo',
      xls_subcategory:r.xls_subcategory||r.raw_subcategory||r.subcategory||r.group_name||'Ostalo',
      quantity:Number(r.total_quantity??r.total_qty??r.quantity??0)||0,
      available:Number(r.available_quantity??r.available_qty??r.available??r.total_quantity??r.total_qty??r.quantity??0)||0,
      unit:r.unit||'kom', status:r.status||'aktivno', availability:r.availability||'dostupno',
      member_visible:r.member_visible!==false, internal_note:r.note||r.internal_note||''
    }));
    out.summary={...(d.summary||{}),source:activeSource};
    return out;
  }
  function idOf(r){return String(r.id||r.legacy_id||r.catalog_id||r.sku||r.name||Math.random()).replace(/'/g,'');}
  function nameOf(r){return r.display_name||r.name||r.item_name||r.model||'Artikl';}
  function catOf(r){return r.xls_category||r.raw_category||r.category_name||r.category||r.main_category||'Ostalo';}
  function subOf(r){return inventorySubcategoryName(catOf(r), r.xls_subcategory||r.raw_subcategory||r.subcategory||r.group_name||r.type||'Ostalo');}
  function inventorySubcategoryName(category, raw){
    const c=String(category||'').trim();
    const s=String(raw||'').trim();
    if(!s) return 'Ostalo';
    const exact={
      'Karabineri':'Karabineri',
      'Sidrišne pločice':'Pločice',
      'Bušilice, baterije i svrdla':'Bušilice i baterije',
      'Transportne vreće i drybagovi':'Transportke',
      'Tekstil i zaštita užeta':'Tekstil',
      'Alat za opremanje':'Alat',
      'Statička speleo užad':'Statik',
      'Dinamička užad':'Dinamik',
      'Označavanje i održavanje užadi':'Ostalo',
      'Kuhinjski pribor i posuđe':'Posuđe',
      'Higijena, čišćenje i potrošno':'Pranje',
      'Skladištenje i transport logora':'Skladištenje',
      'Voda i spremnici':'Voda',
      'Logorski alat i energija':'Alat',
      'Promo i društvena logistika':'Promo',
      'Kuhanje i plin':'Kuhanje',
      'Baterije, punjači i kablovi':'Baterije i punjenje',
      'Foto rasvjeta':'Rasvjeta',
      'Uređaji i navigacija':'Uređaji',
      'Dronovi i pribor':'Dronovi',
      'Regulirani potrošni materijal':'Hilti meci',
      'Regulirana aktivacijska oprema':'Izrada punjenja',
      'Ručni alat':'Alat za rokanje',
      'Svrdla i štemanje':'Svrdla',
      'Aid i tehnička oprema':'Ostalo',
      'Ledna oprema':'Ostalo',
      'Pojasevi i lanyardi':'Ostalo',
      'Osiguravanje i spuštanje':'Ostalo',
      'Via ferrata i apsorberi':'Ostalo',
      'Neopren i zaštita':'Ostalo',
      'Koloture i traxioni':'Ostalo',
      'Akcijska oprema':'Ostalo',
      'Rescue spuštalice':'Ostalo',
      'Karabineri / ostalo':'Ostalo',
      'Rigging plate / PAW':'Ostalo',
      'Penjalice i blokeri':'Ostalo',
      'Pojasevi i prsni navezi':'Ostalo',
      'Karabineri / ostalo':'Ostalo',
      'Spuštalice':'Ostalo',
      'Kacige':'Ostalo',
      'Pupci i lanyardi':'Ostalo'
    };
    if(exact[s]) return exact[s];
    if(c==='Osobna oprema'||c==='Osobni SRT komplet'||c==='Alpinistička oprema'||c==='Alpinistička i penjačka oprema'||c==='Čisto podzemlje'||c==='Tehničko spašavanje i Čisto podzemlje'||c==='Ronilačka oprema') return s==='Ostalo' ? s : 'Ostalo';
    return s;
  }
  function visible(r){return r.member_visible!==false && !/rashod|otpis|izgublj|obris/i.test(String(r.status||''));}
  function available(r){return Number(r.available)>0;}
  function rowText(r){return plain([nameOf(r),catOf(r),subOf(r),r.internal_note,r.note,r.location,r.location_name,r.sku].join(' '));}
  function rows(){
    const d=window.DATA||{};
    const items=(Array.isArray(d.items)?d.items:[]).map(x=>({...x,item_type:x.item_type||'item'}));
    const ropes=(Array.isArray(d.ropes)?d.ropes:[]).map(r=>({...r,id:'ROPE-'+(r.id||r.legacy_id||r.sku||r.name),item_type:'rope',name:r.name||('Uže '+(r.sku||'')),category:r.category||r.category_name||'Užad',category_name:r.category_name||r.category||'Užad',subcategory:r.subcategory||'Užad',available:/društvu|drustvu|aktiv|dostup/i.test(String(r.status||''))?1:0,unit:'kom'}));
    const pieces=(Array.isArray(d.pieces)?d.pieces:[]).map(p=>({...p,id:'PIECE-'+(p.id||p.legacy_id||p.sku||p.name),item_type:'piece',name:p.name||p.model||p.sku||'Komad opreme',category:p.category||p.category_name||'Inventarna oprema',category_name:p.category_name||p.category||'Inventarna oprema',subcategory:p.subcategory||p.location_name||'Pojedinačno',available:/društvu|drustvu|aktiv|dostup/i.test(String(p.status||''))?1:0,unit:'kom'}));
    return [...items,...ropes,...pieces];
  }
  function categoryMeta(c){
    const m={
      'Osobna oprema':['🧗','Osobna oprema iz inventara.'],
      'Oprema za postavljanje':['⚓','Karabineri, pločice i oprema za postavljanje.'],
      'Čisto podzemlje':['🧹','Oprema za akcije čišćenja podzemlja.'],
      'Oprema za crtanje':['🧭','Crtanje, mjerenje i dokumentiranje.'],
      'Oprema za proširivanje':['⛏️','Alat i materijal za proširivanje.'],
      'Elektro i foto oprema':['💡','Rasvjeta, elektronika, foto i komunikacijska oprema.'],
      'Alpinistička oprema':['⛰️','Alpinistička oprema.'],
      'Ronilačka oprema':['🤿','Ronilačka oprema.'],
      'Ostali alat':['🧰','Ostali alat iz inventara.'],
      'Užeta':['🪢','Užeta iz inventara.'],
      'Užad':['🪢','Užad iz inventara.'],
      'Oprema za logor':['⛺','Logor, kuhinja, voda i terenski boravak.'],
      'Medicinska oprema':['✚','Prva pomoć i medicinska oprema.'],
      'Ostalo':['📦','Stavke koje još treba provjeriti.']
    };
    return m[c]||['📦','Oprema'];
  }
  function sortCats(a){const order=['Osobna oprema','Oprema za postavljanje','Čisto podzemlje','Oprema za crtanje','Oprema za proširivanje','Elektro i foto oprema','Alpinistička oprema','Ronilačka oprema','Ostali alat','Užeta','Užad','Oprema za logor','Medicinska oprema','Ostalo']; return a.sort((x,y)=>(order.indexOf(x)<0?99:order.indexOf(x))-(order.indexOf(y)<0?99:order.indexOf(y))||String(x).localeCompare(String(y),'hr'));}
  function currentItems(){const q=plain($('q')?.value||''); const cat=$('cat')?.value||activeCat; const av=$('avail')?.value||''; return rows().filter(visible).filter(r=>!q||rowText(r).includes(q)).filter(r=>!cat||catOf(r)===cat).filter(r=>!activeSub||subOf(r)===activeSub).filter(r=>!av||(av==='dostupno'?available(r):(av==='nedostupno'?!available(r):true)));}
  function setMiniStatus(kind,msg){const el=$('armoryBootStatus')||document.querySelector('[data-armory-boot-status]'); if(el)el.textContent=msg; const line=$('v607StatusText'); if(line)line.textContent=msg;}
  function renderLoading(message, detail){
    loading=true; injectCss();
    const el=$('catalog'); if(!el)return;
    el.innerHTML=`<div class="armory-db-loading"><div class="armory-db-pills"><span class="armory-db-pill warn">Učitavanje</span><span class="armory-db-pill">pokušaj ${attempt||1}</span></div><h2>${esc(message||'Učitavam katalog opreme…')}</h2><div class="armory-db-bar" aria-hidden="true"></div><p>${esc(detail||'Katalog će se prikazati čim evidencija bude spremna.')}</p></div>`;
    setMiniStatus('warn', message||'Učitavam evidenciju');
  }
  function renderFilters(){const sel=$('cat'); if(!sel)return; const cur=sel.value||activeCat; const cats=sortCats([...new Set(rows().filter(visible).map(catOf).filter(Boolean))]); sel.innerHTML='<option value="">Sve kategorije</option>'+cats.map(c=>`<option value="${esc(c)}">${esc(c)}</option>`).join(''); sel.value=cur;}
  function statusLine(total){return `<div class="v607-status-line"><span class="v607-pill ok">${esc(activeSource)} · ${total} stavki</span><span class="v607-pill">spremno</span></div>`;}
  function renderCatalog(){
    if(loading){renderLoading('Učitavam katalog opreme…','Katalog će se prikazati čim evidencija bude spremna.'); return;}
    const el=$('catalog'); if(!el)return;
    const all=rows().filter(visible); const q=plain($('q')?.value||''); const cat=$('cat')?.value||activeCat;
    if(!all.length){renderLoading('Katalog još nije spreman.','Osvježi stranicu za koju minutu.'); return;}
    const status=statusLine(all.length);
    if(!q&&!cat&&!activeSub){
      const cats=sortCats([...new Set(all.map(catOf).filter(Boolean))]);
      el.innerHTML=status+`<div class="user-catalog-head"><div><h2>Što trebaš?</h2><p>Odaberi kategoriju opreme iz evidencije.</p></div><button class="btn primary" onclick="document.getElementById('drawer')?.classList.add('open')">Moj zahtjev</button></div><div class="user-category-grid">${cats.map(c=>{const meta=categoryMeta(c); const rs=all.filter(r=>catOf(r)===c); const subs=new Set(rs.map(subOf)); return `<button class="user-category-card" onclick="sovArmory606.pickCat('${esc(c)}')"><span class="ico">${meta[0]}</span><b>${esc(c)}</b><small>${esc(meta[1])}</small><small class="v607-card-count">${rs.length} stavki · ${subs.size} podkategorija</small></button>`}).join('')}</div>`;
      return;
    }
    const base=rows().filter(visible).filter(r=>!q||rowText(r).includes(q)).filter(r=>!cat||catOf(r)===cat);
    if(cat&&!activeSub&&!q){
      const subs=[...new Set(base.map(subOf).filter(Boolean))].sort((a,b)=>String(a).localeCompare(String(b),'hr'));
      el.innerHTML=status+`<div class="user-path"><button onclick="sovArmory606.clear()">Sve kategorije</button><button class="active">${esc(cat)}</button></div><div class="v607-sub-head"><div><h2>${esc(cat)}</h2><p class="fineprint">Odaberi podkategoriju ili kreni pretraživati unutar kategorije.</p></div><button class="btn" onclick="sovArmory606.clear()">← Kategorije</button></div><div class="user-subcat-list">${subs.map(s=>`<button class="user-subcat-card" onclick="sovArmory606.pickSub('${esc(s)}')">${esc(s)}<small>${base.filter(r=>subOf(r)===s).length} stavki</small></button>`).join('')}</div>`;
      return;
    }
    const items=currentItems(); const title=activeSub||cat||(q?'Rezultati':'Oprema');
    el.innerHTML=status+`<div class="user-path"><button onclick="sovArmory606.clear()">Sve kategorije</button>${cat?`<button onclick="sovArmory606.backToCat()">${esc(cat)}</button>`:''}${activeSub?`<button class="active">${esc(activeSub)}</button>`:''}</div><div class="v607-sub-head"><div><h2>${esc(title)}</h2><p class="fineprint">Klikni Zatraži. Količinu i napomenu možeš urediti u zahtjevu.</p></div><button class="btn" onclick="sovArmory606.back()">← Nazad</button></div>${items.length?`<div class="grid-list">${items.map(r=>{const c=catOf(r),s=subOf(r),m=categoryMeta(c),av=available(r);return `<article class="item-card"><div class="meta"><span class="badge">${m[0]} ${esc(c)}</span><span class="badge">${esc(s)}</span></div><h3>${esc(nameOf(r))}</h3>${r.internal_note||r.note?`<p class="fineprint">${esc(String(r.internal_note||r.note)).slice(0,160)}</p>`:''}<div class="meta"><span class="badge ${av?'ok':'danger'}">${av?'Dostupno':'Nedostupno'}</span>${r.available!==undefined?`<span class="badge">${esc(r.available)} ${esc(r.unit||'kom')}</span>`:''}</div><div class="actions"><button class="btn primary" ${av?'':'disabled'} onclick="sovArmory606.add('${esc(idOf(r))}')">${av?'Zatraži':'Nedostupno'}</button></div></article>`}).join('')}</div>`:'<div class="v607-empty">Nema artikala za ovaj izbor.</div>'}`;
  }
  function applyData(d,source){
    if(!hasRows(d))return false;
    activeSource=source; const norm=normalize(d);
    try{window.DATA=norm; DATA=norm;}catch(e){window.DATA=norm;}
    loading=false; booted=true; clearTimeout(retryTimer); retryTimer=null;
    renderFilters(); try{if(typeof renderKpis==='function')renderKpis();}catch(e){}
    renderCatalog(); try{if(typeof renderRequest==='function')renderRequest();}catch(e){} try{if(typeof renderMyRequests==='function')renderMyRequests();}catch(e){}
    setMiniStatus('ok',`${source} · ${countData(norm)} stavki`);
    return true;
  }
  async function loadStaticFallback(){
    const paths=['data/oruzarstvo-xls-canonical-v6.1.5.json','data/oruzarstvo-data.json','data/oruzarstvo-data-v1-model.json'];
    for(const path of paths){
      try{
        const res=await Promise.race([fetch(path,{cache:'no-store'}),timeout(2600,null)]);
        if(!res||!res.ok) continue;
        const data=await res.json();
        if(hasRows(data)) return applyData(data,'Evidencija opreme');
      }catch(e){ console.warn('armory static fallback skipped',path,e?.message||e); }
    }
    return false;
  }

  async function loadLiveStrict(){
    if(window.DATA && hasRows(window.DATA)) return applyData(window.DATA,'Evidencija opreme');
    if(window.SOVArmoryDB&&SOVArmoryDB.configured&&SOVArmoryDB.configured()&&SOVArmoryDB.loadAllData){
      const cachedOrLive=await Promise.race([SOVArmoryDB.loadAllData({force:false,strictLive:false}),timeout(5200,{__timeout:true})]);
      if(hasRows(cachedOrLive)){
        // prikaz odmah, a refresh neka ide u pozadini bez blokiranja kataloga
        setTimeout(()=>{try{SOVArmoryDB.loadAllData({force:true,background:true}).then(fresh=>{if(hasRows(fresh))applyData(fresh,'Evidencija opreme');}).catch(()=>{});}catch(e){}},250);
        return applyData(cachedOrLive,'Evidencija opreme');
      }
      const live=await Promise.race([SOVArmoryDB.loadAllData({force:true,strictLive:true}),timeout(9000,{__timeout:true})]);
      if(hasRows(live)) return applyData(live,'Evidencija opreme');
    }
    const staticOk=await loadStaticFallback();
    if(staticOk) return true;
    renderLoading('Katalog trenutno nije dostupan.','Pokušaj ponovno za nekoliko minuta.'); return false;
  }
  async function pollLive(){
    attempt += 1; renderLoading('Učitavam katalog opreme…','Katalog će se prikazati čim evidencija bude spremna.');
    try{ const ok=await loadLiveStrict(); if(ok){ setTimeout(loadRequests,150); return; } }
    catch(e){console.warn('SOV armory catalog load failed',e?.message||e); renderLoading('Katalog još nije spreman.','Pokušaj ponovno za nekoliko minuta.');}
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
    injectCss(); attach(); renderLoading('Učitavam katalog opreme…','Katalog će se prikazati čim evidencija bude spremna.');
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
