/* SOV Web v5.58.26 — safe UX polish helpers. No data/backend changes. */
(function(){
  const VERSION='5.58.26';
  const PAGE=(location.pathname.split('/').pop()||'index.html').toLowerCase();
  const PUBLIC=new Set(['index.html','vijesti.html','vijest.html','o-drustvu.html','povijest.html','procelnistvo.html','velebitaski-duh.html','speleoskola.html','pridruzi-nam-se.html','videos.html','login.html']);
  function ready(fn){document.readyState==='loading'?document.addEventListener('DOMContentLoaded',fn):fn();}
  function ensurePublicHamburger(){
    const header=document.querySelector('header.sov-shell-normalized, header.topbar, header.sov-top, header.top, header');
    if(!header) return;
    if(!header.querySelector('[data-sov-shell-menu]')){
      const btn=document.createElement('button');
      btn.type='button'; btn.className='sov-shell-menu-button'; btn.setAttribute('data-sov-shell-menu','1'); btn.setAttribute('aria-label','Otvori navigaciju'); btn.textContent='☰';
      header.appendChild(btn);
    }
  }
  function ensureBackTop(){
    if(document.querySelector('.sov-back-top')) return;
    const b=document.createElement('button');
    b.type='button'; b.className='sov-back-top'; b.setAttribute('aria-label','Na vrh stranice'); b.textContent='↑';
    b.addEventListener('click',()=>window.scrollTo({top:0,behavior:window.matchMedia('(prefers-reduced-motion: reduce)').matches?'auto':'smooth'}));
    document.body.appendChild(b);
    const update=()=>b.classList.toggle('is-visible',window.scrollY>520);
    update(); window.addEventListener('scroll',update,{passive:true});
  }
  function polishHomepage(){
    if(PAGE!=='index.html') return;
    const actions=document.querySelector('.hero-actions');
    if(actions && !actions.querySelector('.hero-cloud')){
      const cloud=document.createElement('a'); cloud.className='hero-cloud'; cloud.href='dashboard.html'; cloud.textContent='Otvori SOV Cloud';
      const school=document.createElement('a'); school.className='hero-secondary'; school.href='speleoskola.html'; school.textContent='Speleoškola';
      const news=document.createElement('a'); news.className='hero-secondary'; news.href='vijesti.html'; news.textContent='Sve vijesti';
      actions.appendChild(cloud); actions.appendChild(school); actions.appendChild(news);
    }
    document.querySelectorAll('.news-card,.news-side-card,.news-featured').forEach(a=>{
      if(!a.querySelector('.sov-read-more')){
        const copy=a.querySelector('.news-copy');
        if(copy){ const span=document.createElement('span'); span.className='sov-read-more'; span.textContent='Pročitaj →'; span.style.cssText='display:inline-flex;margin-top:14px;color:#e9ffd0;font-weight:1000;font-size:13px'; copy.appendChild(span); }
      }
    });
  }
  function cleanPublicCopy(){
    document.querySelectorAll('footer.footer').forEach(f=>{
      if(/Moderni statički portal|v\d/i.test(f.textContent||'')) f.textContent='Speleološki odsjek PDS Velebit · Zagreb';
    });
    document.querySelectorAll('.contact-label').forEach(el=>{ if((el.textContent||'').trim()==='Open Wednesday') el.textContent='Otvoreno srijedom'; });
    document.querySelectorAll('.yt-note').forEach(el=>{ if(/API|statički portal|browser postavki/i.test(el.textContent||'')) el.remove(); });
  }
  function markBody(){
    document.documentElement.dataset.sovPolishVersion=VERSION;
    document.body.classList.add('sov-polish-ready');
    if(PUBLIC.has(PAGE)) document.body.classList.add('sov-public-page'); else document.body.classList.add('sov-cloud-page');
    if(document.getElementById('dashboardRolePreview')) document.body.classList.add('sov-has-bottom-preview');
  }
  function improveDrawerState(){
    const obs=new MutationObserver(()=>{
      const open=!!document.querySelector('.sov-shell-drawer.is-open');
      document.documentElement.classList.toggle('sov-drawer-open',open);
      document.body.style.overflow=open?'hidden':'';
    });
    obs.observe(document.body,{subtree:true,attributes:true,attributeFilter:['class']});
  }
  ready(function(){ markBody(); ensurePublicHamburger(); polishHomepage(); cleanPublicCopy(); ensureBackTop(); improveDrawerState(); });
})();
