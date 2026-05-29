
(function(){
  const VERSION='5.58.23';
  const PAGE=(location.pathname.split('/').pop()||'index.html').toLowerCase();
  const APP_PAGES=new Set([
    'dashboard.html','karta.html','baza.html','izleti.html','izleti-cloud.html','kalendar-izleta.html','oruzarstvo.html','oruzarstvo-import.html','oruzar-master.html','oruzar-master-posudbe.html','oruzar-master-inventar.html','oruzar-master-inventura.html','oruzar-master-notes.html','arhivar-dashboard.html','arhivar.html','arhivar-predane-jame.html','arhivar-izvoz.html','arhivar-zahvati.html','news-editor.html','napisi-clanak.html','admin-users.html','admin-notifications.html','role-manager.html','sync-status.html','audit-status.html','speleo-sql-safe.html','speleo-sql-edit-sandbox.html','speleo-sql-compare.html','speleo-sql-object-hub.html','speleo-sql-promote.html','speleo-sql-go-live.html','topodroid.html','topodroid-import.html','speleo-zapisnik.html','novi-zapisnik.html','pregled-baze.html','pregled-zapisnika.html','dokumentacija.html','zapisnici-skupstine.html'
  ]);
  const ROLE_LABEL={webmaster:'Webmaster',admin:'Admin',oruzar:'Oružar',arhivar:'Arhivar',editor:'Urednik',user:'Član'};
  const LINKS=[
    {href:'dashboard.html',label:'Dashboard',roles:['user','editor','oruzar','arhivar','admin','webmaster'],group:'Osnovno'},
    {href:'karta.html',label:'Karta',roles:['user','editor','oruzar','arhivar','admin','webmaster'],group:'Osnovno'},
    {href:'izleti.html',label:'Izleti',roles:['user','editor','arhivar','admin','webmaster'],group:'Osnovno'},
    {href:'oruzarstvo.html',label:'Oprema',roles:['user','editor','oruzar','arhivar','admin','webmaster'],group:'Osnovno'},
    {href:'napisi-clanak.html',label:'Napiši članak',roles:['user','editor','oruzar','arhivar','admin','webmaster'],group:'Osnovno'},
    {href:'arhivar-dashboard.html',label:'Arhivar',roles:['arhivar','admin','webmaster'],group:'Arhivar'},
    {href:'arhivar.html',label:'Uređivanje arhive',roles:['arhivar','admin','webmaster'],group:'Arhivar'},
    {href:'arhivar-predane-jame.html',label:'Predane jame',roles:['arhivar','admin','webmaster'],group:'Arhivar'},
    {href:'arhivar-izvoz.html',label:'Izvoz arhive',roles:['arhivar','admin','webmaster'],group:'Arhivar'},
    {href:'oruzar-master.html',label:'Oružar workspace',roles:['oruzar','admin','webmaster'],group:'Oružar'},
    {href:'oruzar-master-posudbe.html',label:'Posudbe',roles:['oruzar','admin','webmaster'],group:'Oružar'},
    {href:'oruzar-master-inventar.html',label:'Inventar',roles:['oruzar','admin','webmaster'],group:'Oružar'},
    {href:'oruzar-master-inventura.html',label:'Inventura',roles:['oruzar','admin','webmaster'],group:'Oružar'},
    {href:'news-editor.html',label:'Urednik vijesti',roles:['editor','admin','webmaster'],group:'Urednik'},
    {href:'admin-users.html',label:'Korisnici',roles:['admin','webmaster'],group:'Admin'},
    {href:'admin-notifications.html',label:'Obavijesti',roles:['admin','webmaster'],group:'Admin'},
    {href:'role-manager.html',label:'Role manager',roles:['webmaster'],group:'Webmaster'},
    {href:'sync-status.html',label:'Sync status',roles:['webmaster'],group:'Webmaster'},
    {href:'speleo-sql-safe.html',label:'SQL alati',roles:['webmaster'],group:'Webmaster'}
  ];
  function ready(fn){ if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',fn); else fn(); }
  function esc(s){return String(s||'').replace(/[&<>'"]/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[m]));}
  function pageLabel(){
    const t=(document.title||'SOV Cloud').replace(/\s*[—|·].*$/,'').trim();
    const map={
      'karta.html':'Karta','baza.html':'Karta','oruzarstvo.html':'Oprema','oruzar-master.html':'Oružar','oruzar-master-posudbe.html':'Posudbe','oruzar-master-inventar.html':'Inventar','oruzar-master-inventura.html':'Inventura','arhivar.html':'Uređivanje arhive','arhivar-predane-jame.html':'Predane jame','arhivar-izvoz.html':'Izvoz arhive','news-editor.html':'Urednik vijesti','sync-status.html':'Sync status'
    };
    return map[PAGE] || t || 'SOV Cloud';
  }
  async function getRole(){
    try{
      if(window.SOVAuth && window.SOVAuth.getProfile){ const p=await window.SOVAuth.getProfile(); return String((p&&p.role)||'user').toLowerCase(); }
    }catch(e){}
    try{ return String(localStorage.getItem('SOV_DASHBOARD_PREVIEW_ROLE')||'user').toLowerCase(); }catch(e){ return 'user'; }
  }
  function headerSelector(){ return 'header.sov-top,header.aw-top,header.as-top,header.cm-top,header.top,header.topbar,header.app-head,header.armory-top,header.om-top,header'; }
  function findHeader(){
    const candidates=[...document.querySelectorAll(headerSelector())];
    return candidates.find(h=>!h.closest('.sov-shell-drawer') && h.offsetParent!==null) || candidates[0] || null;
  }
  function isCurrent(href){
    const target=href.split('#')[0].toLowerCase();
    if(PAGE==='baza.html' && target==='karta.html') return true;
    return target===PAGE;
  }
  function visibleLinks(role){ return LINKS.filter(l=>l.roles.includes(role)); }
  function groupLinks(items){
    const order=['Osnovno','Arhivar','Oružar','Urednik','Admin','Webmaster'];
    return order.map(g=>[g,items.filter(l=>l.group===g)]).filter(([,arr])=>arr.length);
  }
  function ensureHeader(role){
    if(!APP_PAGES.has(PAGE)) return;
    let header=findHeader();
    if(!header){
      header=document.createElement('header'); header.className='sov-top'; document.body.insertBefore(header,document.body.firstChild);
      header.innerHTML='<a class="sov-brand" href="dashboard.html"><span class="sov-mark">SOV</span><span><b>'+esc(pageLabel())+'</b><small>SOV Cloud</small></span></a><nav class="sov-nav"><a href="dashboard.html">Dashboard</a><a href="karta.html">Karta</a><a href="oruzarstvo.html">Oprema</a></nav>';
    }
    header.classList.add('sov-shell-normalized');
    if(!header.querySelector('[data-sov-shell-menu]')){
      const btn=document.createElement('button');
      btn.type='button'; btn.className='sov-shell-menu-button'; btn.setAttribute('aria-label','Otvori navigaciju'); btn.setAttribute('data-sov-shell-menu','1'); btn.textContent='☰';
      header.appendChild(btn);
      btn.addEventListener('click',()=>openDrawer(role));
    }
    const brand=header.querySelector('a, .brand, .sov-brand, .aw-brand, .as-brand, .cm-brand, .armory-brand, .om-brand');
    if(brand && !brand.querySelector('b')){
      brand.innerHTML='<span class="sov-mark">SOV</span><span><b>'+esc(pageLabel())+'</b><small>SOV Cloud</small></span>';
      if(brand.tagName==='A' && !brand.getAttribute('href')) brand.setAttribute('href','dashboard.html');
    }
    header.querySelectorAll('nav a,a').forEach(a=>{
      const href=(a.getAttribute('href')||'').split('#')[0].toLowerCase();
      if(href && isCurrent(href)){ a.classList.add('active','sov-shell-current'); a.setAttribute('aria-current','page'); }
    });
  }
  function ensureSkip(){
    if(document.querySelector('.sov-skip-link')) return;
    const main=document.querySelector('main,[role="main"],.shell,.wrap,.armory-shell,.aw-shell,.as-shell,.app-shell,.sov-shell');
    if(main && !main.id) main.id='sov-main-content';
    const href=main ? '#'+main.id : '#';
    const a=document.createElement('a'); a.className='sov-skip-link'; a.href=href; a.textContent='Preskoči na sadržaj'; document.body.insertBefore(a,document.body.firstChild);
  }
  function makeDrawer(role){
    let drawer=document.querySelector('.sov-shell-drawer');
    if(drawer) return drawer;
    drawer=document.createElement('div'); drawer.className='sov-shell-drawer'; drawer.setAttribute('aria-hidden','true');
    const groups=groupLinks(visibleLinks(role));
    const html=groups.map(([g,items])=>'<div class="sov-shell-drawer-group"><div class="sov-shell-group-title">'+esc(g)+'</div><div class="sov-shell-drawer-grid">'+items.map(l=>'<a href="'+esc(l.href)+'" class="'+(isCurrent(l.href)?'active':'')+'"><span>'+esc(l.label)+'</span><span>›</span></a>').join('')+'</div></div>').join('');
    drawer.innerHTML='<div class="sov-shell-drawer-panel" role="dialog" aria-modal="true" aria-label="SOV navigacija"><div class="sov-shell-drawer-head"><div><b>SOV Cloud</b><small>'+esc(pageLabel())+'</small></div><button type="button" class="sov-shell-close" aria-label="Zatvori navigaciju">×</button></div><span class="sov-shell-role-pill">'+esc(ROLE_LABEL[role]||role)+'</span>'+html+'</div>';
    document.body.appendChild(drawer);
    drawer.addEventListener('click',e=>{ if(e.target===drawer || e.target.closest('.sov-shell-close')) closeDrawer(); });
    document.addEventListener('keydown',e=>{ if(e.key==='Escape') closeDrawer(); });
    return drawer;
  }
  function openDrawer(role){ const d=makeDrawer(role); d.classList.add('is-open'); d.setAttribute('aria-hidden','false'); setTimeout(()=>{ const first=d.querySelector('a,button'); if(first) first.focus(); },30); }
  function closeDrawer(){ const d=document.querySelector('.sov-shell-drawer'); if(!d) return; d.classList.remove('is-open'); d.setAttribute('aria-hidden','true'); }
  function hideUnauthorized(role){
    // This is a UX guard, not security. Real access is still handled by page/RLS/auth.
    document.querySelectorAll('[data-role-webmaster],a[href*="speleo-sql"],a[href="sync-status.html"],a[href="role-manager.html"]').forEach(el=>{ if(role!=='webmaster') el.classList.add('sov-shell-hidden-for-role'); });
    if(!['webmaster','admin'].includes(role)) document.querySelectorAll('a[href="admin-users.html"],a[href="admin-notifications.html"]').forEach(el=>el.classList.add('sov-shell-hidden-for-role'));
  }
  ready(async function(){
    if(!APP_PAGES.has(PAGE)) return;
    ensureSkip();
    const role=await getRole();
    document.documentElement.dataset.sovShellVersion=VERSION;
    document.body.dataset.sovRole=role;
    ensureHeader(role);
    hideUnauthorized(role);
  });
})();
