(function(){
  const VERSION='6.1.45g';
  const state={cat:null,sub:null};
  const $=(s,r=document)=>r.querySelector(s);
  const $$=(s,r=document)=>Array.from(r.querySelectorAll(s));
  const html=(v)=>String(v??'').replace(/[&<>"']/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[m]));
  const norm=(v)=>String(v||'').trim();
  const lower=(v)=>norm(v).toLowerCase();
  const toast=(m)=>{try{window.toast?window.toast(m):console.log(m)}catch(e){console.log(m)}};
  const safe=(fn,fb)=>{try{return fn()}catch(e){return fb}};
  function rows(){
    const raw=safe(()=>typeof window.requestableItems==='function'?window.requestableItems():[],[]);
    return (raw||[]).filter(Boolean).filter(r=>safe(()=>window.canArmory&&window.canArmory()?true:r.member_visible!==false,true));
  }
  function idOf(r){return String(r.id||r._id||r.catalog_id||r.equipment_item_id||r.equipment_asset_id||r.sku||r.legacy_id||r.name||r.display_name||r.model||'');}
  function nameOf(r){return String(r.name||r._name||r.display_name||r.model||r.sku||'Artikl');}
  function catOf(r){return String(safe(()=>window.displayCategory?window.displayCategory(r):'', '')||r.category_name||r.category||r.group_name||'Ostalo');}
  function subOf(r){return String(safe(()=>window.displaySubcategory?window.displaySubcategory(r):'', '')||r.subcategory_name||r.subcategory||r.type||'Ostalo');}
  function noteOf(r){return String(r.internal_note||r.note||r.description||'').trim();}
  function availableOf(r){return Number(r.available ?? r.quantity ?? r.count ?? 1)>0 && !/nedostup|rashod|otpis|izgublj/i.test(String(r.availability||r.status||''));}
  function iconFor(c){
    const s=lower(c);
    if(/uže|uzad|rope/.test(s))return '🪢';
    if(/osob|pojas|croll|prs|stop|ruč|ruc|bloker/.test(s))return '🧗';
    if(/karab|sidr|ploč|ploc|spit|bolt|postav/.test(s))return '🔩';
    if(/svjet|bater|rasv/.test(s))return '🔦';
    if(/crt|topo|mjern|kompas/.test(s))return '🗺️';
    if(/bušil|busil|alat/.test(s))return '🛠️';
    if(/transport|torb|vreć|vrec/.test(s))return '🎒';
    return '📦';
  }
  function descFor(c){
    const s=lower(c);
    if(/uže|uzad|rope/.test(s))return 'Užad i linije za teren';
    if(/osob/.test(s))return 'Osobna speleo oprema';
    if(/postav|sidr|karab|ploč|ploc/.test(s))return 'Oprema za postavljanje';
    if(/crt|topo|mjern/.test(s))return 'Crtanje, mjerenje i dokumentacija';
    if(/svjet|bater/.test(s))return 'Rasvjeta i napajanje';
    return 'Oprema za posudbu';
  }
  function searchText(r){return [nameOf(r),catOf(r),subOf(r),r.sku,r.legacy_id,r.model,r.manufacturer,noteOf(r)].join(' ').toLowerCase();}
  function sorted(arr){return [...arr].sort((a,b)=>String(a).localeCompare(String(b),'hr',{sensitivity:'base'}));}
  function currentQuery(){return lower($('#q')?.value||'');}
  function badgeStatus(status){
    const s=lower(status);
    if(/return|vrać|vrac|closed|done/.test(s)) return ['Vraćeno',4];
    if(/issued|izdan|vani/.test(s)) return ['Izdano vani',3];
    if(/cancel|odbij|reject/.test(s)) return ['Zatvoreno',4];
    return ['Za izdati',2];
  }
  function findItem(rawId){
    const wanted=decodeURIComponent(String(rawId||''));
    return rows().find(r=>[idOf(r),r.id,r._id,r.catalog_id,r.equipment_item_id,r.equipment_asset_id,r.sku,r.legacy_id,nameOf(r),r.display_name,r.model].some(v=>String(v||'')===wanted));
  }
  function cartRef(){
    try{ if(!Array.isArray(window.CART)) window.CART=[]; }catch(e){ window.CART=[]; }
    try{ if(typeof CART!=='undefined' && Array.isArray(CART)) return CART; }catch(e){}
    return window.CART;
  }
  function cartCount(){return cartRef().reduce((a,x)=>a+(Number(x.quantity)||1),0);}
  function syncCartFab(){
    let fab=$('.sov-cart-fab');
    if(!fab){
      fab=document.createElement('button');
      fab.type='button'; fab.className='sov-cart-fab'; fab.innerHTML='<span>🛒 Moj zahtjev</span><span class="count">0</span>';
      fab.onclick=()=>{$('#drawer')?.classList.add('open'); if(window.renderRequest) window.renderRequest();};
      document.body.appendChild(fab);
    }
    const n=cartCount();
    $('.count',fab).textContent=String(n);
    fab.style.display=n?'flex':'none';
  }
  function addToCart(rawId){
    const item=findItem(rawId);
    if(!item){toast('Nisam našao artikl.');return;}
    const id=idOf(item), name=nameOf(item);
    const cart=cartRef();
    const existing=cart.find(x=>String(x.id)===id);
    if(existing) existing.quantity=Number(existing.quantity||1)+1;
    else cart.push({id,name,quantity:1,type:item.item_type||item.tracking_type||'item',category:catOf(item),subcategory:subOf(item)});
    try{window.CART=cart; if(typeof CART!=='undefined') CART=cart;}catch(e){}
    renderRequest();
    $('#drawer')?.classList.add('open');
    toast(existing?'Količina povećana u košarici':'Dodano u košaricu');
    syncCartFab();
  }
  function removeCart(id){
    const cart=cartRef().filter(x=>String(x.id)!==String(id));
    try{window.CART=cart; if(typeof CART!=='undefined') CART=cart;}catch(e){}
    renderRequest(); syncCartFab();
  }
  function changeQty(id,delta){
    const cart=cartRef(); const x=cart.find(i=>String(i.id)===String(id)); if(!x)return;
    x.quantity=Math.max(1,(Number(x.quantity)||1)+delta); renderRequest(); syncCartFab();
  }
  function renderRequest(){
    const mount=$('#requestItems'); if(!mount)return;
    const cart=cartRef();
    syncCartFab();
    mount.innerHTML=cart.length?`<div class="sov-checkout-hint">Ovo je košarica. Ništa se ne šalje oružaru dok ne klikneš <b>Pošalji oružaru</b>.</div><div class="sov-checkout-summary">${cart.map(i=>`<div class="sov-checkout-line"><div><b>${html(i.name)}</b><small>${html(i.category||'Oprema')}${i.subcategory?' · '+html(i.subcategory):''} · količina ${html(i.quantity||1)}</small></div><div class="sov-checkout-actions"><button type="button" onclick="sovShopQty('${html(i.id)}',-1)">−</button><button type="button" onclick="sovShopQty('${html(i.id)}',1)">+</button><button type="button" class="remove" onclick="sovShopRemove('${html(i.id)}')">Makni</button></div></div>`).join('')}</div>`:`<div class="sov-checkout-empty"><b>Košarica je prazna</b><br><small>U katalogu klikni “Zatraži” na opremi koju trebaš.</small></div>`;
  }
  function renderHeader(title,desc,back){
    return `<div class="sov-shop-head"><div><h2>${html(title)}</h2><p>${html(desc)}</p></div><div class="sov-shop-searchbar"><input id="sovShopSearchMirror" placeholder="Traži: croll, uže, karabiner..." value="${html($('#q')?.value||'')}"><button class="btn primary" type="button" onclick="document.getElementById('q').value=document.getElementById('sovShopSearchMirror').value;renderCatalog()">Traži</button>${back?`<button class="btn" type="button" onclick="${back}">Nazad</button>`:''}</div></div>`;
  }
  function renderSteps(){return `<div class="sov-shop-steps"><div class="sov-shop-step"><b>1. Odaberi</b><small>Kategorija ili pretraga</small></div><div class="sov-shop-step"><b>2. Dodaj</b><small>“Zatraži” ide u košaricu</small></div><div class="sov-shop-step"><b>3. Pošalji</b><small>Checkout zahtjev oružaru</small></div><div class="sov-shop-step"><b>4. Vrati</b><small>Oružar zatvara posudbu</small></div></div>`;}
  function renderCatalog(){
    const mount=$('#catalog'); if(!mount)return;
    const q=currentQuery();
    const forced=$('#cat')?.value||'';
    let list=rows();
    if(q) list=list.filter(r=>searchText(r).includes(q));
    if(forced) list=list.filter(r=>catOf(r)===forced);
    if(state.cat) list=list.filter(r=>catOf(r)===state.cat);
    if(state.sub) list=list.filter(r=>subOf(r)===state.sub);
    const a=$('#avail')?.value||'';
    if(a) list=list.filter(r=>a==='dostupno'?availableOf(r):!availableOf(r));

    if(!q && !forced && !state.cat){
      const cats=new Map(); rows().forEach(r=>{const c=catOf(r); if(!cats.has(c)) cats.set(c,{name:c,count:0,available:0,subs:new Set()}); const o=cats.get(c); o.count++; if(availableOf(r)) o.available++; o.subs.add(subOf(r));});
      mount.innerHTML=renderHeader('Što trebaš za teren?','Odaberi grupu opreme kao u webshopu. Detalje i količinu uređuješ u košarici prije slanja.',null)+renderSteps()+`<div class="sov-shop-cats">${sorted([...cats.keys()]).map(c=>{const o=cats.get(c);return `<button class="sov-shop-cat" type="button" onclick="sovShopPickCat('${html(c)}')"><span class="ico">${iconFor(c)}</span><b>${html(c)}</b><small>${html(descFor(c))}</small><span class="meta">${o.count} artikala · ${o.available} dostupno</span></button>`}).join('')}</div>`;
      bindMirror(); return;
    }
    const title=q?`Rezultati za “${q}”`:(state.sub||state.cat||forced||'Rezultati');
    const desc=q?'Klikni Zatraži za dodavanje u košaricu.':'Odaberi podkategoriju ili odmah dodaj artikl u košaricu.';
    const path=`<div class="sov-shop-path"><button type="button" onclick="sovShopClear()">Sve kategorije</button>${state.cat?`<button type="button" class="active" onclick="sovShopBackCat()">${html(state.cat)}</button>`:''}${state.sub?`<span class="active">${html(state.sub)}</span>`:''}</div>`;
    const subs=(!q && (state.cat||forced) && !state.sub)?sorted([...new Set(rows().filter(r=>(!state.cat||catOf(r)===state.cat)&&(!forced||catOf(r)===forced)).map(subOf))]):[];
    const subHtml=subs.length?`<div class="sov-shop-subcats">${subs.map(s=>`<button type="button" class="sov-shop-subcat" onclick="sovShopPickSub('${html(s)}')">${html(s)}<small>${rows().filter(r=>(!state.cat||catOf(r)===state.cat)&&subOf(r)===s).length} artikala</small></button>`).join('')}</div>`:'';
    mount.innerHTML=path+renderHeader(title,desc,state.sub?'sovShopBackCat()':(state.cat?'sovShopClear()':''))+subHtml+`<div class="sov-shop-grid">${list.slice(0,220).map(r=>{const ok=availableOf(r);return `<article class="sov-shop-item"><div class="top"><div class="tags"><span class="sov-shop-pill">${iconFor(catOf(r))} ${html(catOf(r))}</span><span class="sov-shop-pill">${html(subOf(r))}</span></div><span class="sov-shop-availability ${ok?'ok':'bad'}">${ok?'Dostupno':'Nedostupno'}</span></div><h3>${html(nameOf(r))}</h3>${noteOf(r)?`<p class="note">${html(noteOf(r)).slice(0,150)}</p>`:''}<div class="actions"><button class="btn primary" ${ok?'':'disabled'} type="button" onclick="sovShopAdd('${html(idOf(r))}')">${ok?'Zatraži':'Trenutno nedostupno'}</button></div></article>`}).join('')}</div>${!list.length?'<div class="sov-shop-empty">Nema rezultata. Probaj kraću pretragu ili drugu kategoriju.</div>':''}`;
    bindMirror();
  }
  function bindMirror(){const m=$('#sovShopSearchMirror'); if(m){m.addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault(); const q=$('#q'); if(q)q.value=m.value; renderCatalog();}})}}
  function getRequests(){return safe(()=>window.getRequests?window.getRequests():(DB_REQUESTS||JSON.parse(localStorage.getItem('sov_equipment_requests')||'[]')),[]);}
  function renderMyRequests(){
    const mount=$('#myrequests'); if(!mount)return;
    const reqs=getRequests();
    mount.innerHTML=renderHeader('Moji zahtjevi','Status posudbe prikazan je kao narudžba: zahtjev, izdavanje, vani, povrat.',null)+(!reqs.length?'<div class="sov-shop-empty">Još nema poslanih zahtjeva.</div>':`<div class="sov-order-list">${reqs.map(r=>{const st=badgeStatus(r.status);return `<article class="sov-order-card"><div class="head"><div><h3>${html(r.trip||r.note||'Zahtjev za opremu')}</h3><small>${html((r.created_at||'').slice(0,10)||'')} ${r.from?`· ${html(r.from)}${r.to?' – '+html(r.to):''}`:''}</small></div><span class="sov-shop-availability ${st[1]>=3?'ok':'bad'}">${html(st[0])}</span></div><div class="sov-order-items">${(r.items||[]).map(i=>`<span class="sov-shop-pill">${html(i.name||i.item_name||'Artikl')} × ${html(i.quantity||1)}</span>`).join('')||'<span class="sov-shop-pill">bez stavki</span>'}</div><div class="sov-order-flow">${[1,2,3,4].map(i=>`<span class="${i<=st[1]?'on':''}"></span>`).join('')}</div><div class="sov-shop-toast-note">${html(r.email||r.user||'')}</div></article>`}).join('')}</div>`);
  }
  function polishDom(){
    document.body.classList.add('sov-shop-ux');
    const h1=$('.armory-hero h1'); if(h1)h1.textContent='Posudi opremu bez Excel panike';
    const p=$('.armory-hero p'); if(p)p.textContent='Katalog radi kao webshop: pronađi opremu, dodaj u košaricu, pošalji zahtjev oružaru i prati status posudbe.';
    const b=$('.hero-actions .btn.primary'); if(b)b.textContent='Otvori katalog';
    const reqBtn=$('.hero-actions .btn:not(.primary)'); if(reqBtn)reqBtn.textContent='🛒 Košarica';
    const dh=$('.drawer-head h3'); if(dh)dh.textContent='Košarica zahtjeva';
    const send=$('#sendRequest'); if(send)send.textContent='Pošalji zahtjev oružaru';
    const trip=$('label[for="reqTrip"]');
    const open=$('#openRequest'); if(open)open.textContent='🛒 Moj zahtjev / košarica';
    const q=$('#q'); if(q){q.placeholder='Traži opremu: croll, uže, karabiner, baterija...'; q.addEventListener('input',()=>{state.cat=null;state.sub=null;renderCatalog();});}
    ['cat','avail'].forEach(id=>{$('#'+id)?.addEventListener('input',()=>renderCatalog());});
    syncCartFab();
  }
  window.sovShopAdd=addToCart;
  window.sovShopRemove=removeCart;
  window.sovShopQty=changeQty;
  window.sovShopPickCat=(c)=>{state.cat=c;state.sub=null;$('#q')&&( $('#q').value=''); renderCatalog();};
  window.sovShopPickSub=(s)=>{state.sub=s;$('#q')&&( $('#q').value=''); renderCatalog();};
  window.sovShopClear=()=>{state.cat=null;state.sub=null;$('#q')&&( $('#q').value='');$('#cat')&&( $('#cat').value=''); renderCatalog();};
  window.sovShopBackCat=()=>{state.sub=null; renderCatalog();};
  window.addToCart=addToCart;
  window.v459Add=addToCart;
  window.sovArmoryAdd55912=addToCart;
  window.renderRequest=renderRequest;
  window.renderCatalog=renderCatalog;
  window.renderMyRequests=renderMyRequests;
  function boot(){polishDom(); setTimeout(()=>{polishDom(); renderCatalog(); renderRequest(); renderMyRequests();},120); setTimeout(()=>{renderCatalog();renderRequest();syncCartFab();},800);}
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',boot); else boot();
})();
