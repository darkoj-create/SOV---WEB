(function(){
  const navItems=[
    ['index.html','Novosti'],
    ['vijesti.html','Sve objave'],
    ['o-drustvu.html','O društvu'],
    ['speleoskola.html','Speleoškola'],
    ['pridruzi-nam-se.html','Pridruži nam se'],
    ['videos.html','Video'],
    ['dashboard.html','Članski ulaz','member-link']
  ];
  function pageName(){
    let p=(location.pathname.split('/').pop()||'index.html').toLowerCase();
    if(p==='') p='index.html';
    return p;
  }
  function loadHomepageRetroStyle(){
    if(pageName()!=='index.html')return;
    if(document.querySelector('link[data-sov-home-retro]'))return;
    const link=document.createElement('link');
    link.rel='stylesheet';
    link.href='assets/sov-home-retro-v6151.css?v=6.1.51';
    link.setAttribute('data-sov-home-retro','');
    document.head.appendChild(link);
  }
  loadHomepageRetroStyle();
  function isActive(href,current){
    if(current==='index.html' && href==='index.html') return true;
    if(current==='vijest.html' && href==='vijesti.html') return true;
    if(['o-drustvu.html','procelnistvo.html','povijest.html','velebitaski-duh.html','velebiten.html'].includes(current) && href==='o-drustvu.html') return true;
    return href===current;
  }
  function headerHtml(current){
    const links=navItems.map(([href,label,cls])=>`<a href="${href}" class="${cls||''}" ${isActive(href,current)?'aria-current="page"':''}>${label}</a>`).join('');
    return `<header class="topbar sov-public-header"><nav class="sov-public-nav" aria-label="Glavna navigacija"><a class="sov-public-brand" href="index.html" aria-label="Speleološki odsjek Velebit — naslovnica"><img src="assets/brand/sov-round-logo.png" alt="" loading="eager"><span><strong>Speleološki odsjek Velebit</strong><small>Zagreb · od 1971.</small></span></a><button class="sov-public-toggle" type="button" aria-label="Otvori izbornik" aria-expanded="false">☰</button><div class="sov-public-links">${links}</div></nav></header>`;
  }
  function normalizeLinks(){
    document.querySelectorAll('a[href]').forEach(a=>{
      const h=(a.getAttribute('href')||'').trim();
      if(h==='/o-nama'||h==='o-nama'||h==='o-nama.html'||h==='/o-nama/') a.setAttribute('href','o-drustvu.html');
      if(h==='login.html') a.setAttribute('href','login.html');
    });
  }
  function installHeader(){
    const current=pageName();
    const old=document.querySelector('body > header.topbar, body > header.top, header.topbar, header.top');
    if(!old) return;
    const holder=document.createElement('div');
    holder.innerHTML=headerHtml(current);
    old.replaceWith(holder.firstElementChild);
    const header=document.querySelector('.sov-public-header');
    const btn=header&&header.querySelector('.sov-public-toggle');
    if(btn){
      btn.addEventListener('click',()=>{
        const open=header.classList.toggle('is-open');
        btn.setAttribute('aria-expanded',String(open));
        btn.setAttribute('aria-label',open?'Zatvori izbornik':'Otvori izbornik');
      });
      document.addEventListener('keydown',e=>{ if(e.key==='Escape'){ header.classList.remove('is-open'); btn.setAttribute('aria-expanded','false'); }});
    }
  }
  function normalizeHomepageHero(){
    if(pageName()!=='index.html') return;
    document.body.classList.add('sov-home-retro');
    document.body.classList.remove('index-menu-open');
    document.querySelectorAll('.index-menu-toggle,.index-hero-chips').forEach(el=>el.remove());
    const hero=document.querySelector('.portal-hero');
    const title=hero&&hero.querySelector('h1');
    if(title)title.textContent='Speleološki odsjek Velebit';
    hero&&hero.querySelectorAll('.portal-sub').forEach(el=>el.remove());
    const actions=hero&&hero.querySelector('.hero-actions');
    if(actions){
      actions.innerHTML=`
        <a class="hero-join" href="pridruzi-nam-se.html">Pridruži nam se</a>
        <a class="hero-ghost" href="speleoskola.html">Speleoškola</a>
        <div class="hero-utility-links" aria-label="Brzi linkovi">
          <a href="#galerija">Galerija</a>
          <a href="dashboard.html">SOV Cloud</a>
          <a href="vijesti.html">Sve vijesti</a>
        </div>`;
    }
    const stats=hero&&hero.querySelector('.hero-stats');
    if(stats){
      stats.classList.add('home-stats-strip');
      hero.insertAdjacentElement('afterend',stats);
    }
  }
  function simplifyHomepageCards(){
    if(pageName()!=='index.html')return;
    document.querySelectorAll('.news-copy em,.quick-inner a p,.video-mini p,.link-card p,.section-head p').forEach(el=>el.remove());
    document.querySelectorAll('.news-card,.news-side-card,.news-featured').forEach(card=>card.classList.add('sov-title-only-card'));
  }
  function fixAboutSubnav(){
    const nav=document.querySelector('.public-tabs-v609');
    if(!nav) return;
    nav.classList.add('sov-about-subnav');
    const map=[['#pregled','Pregled'],['#procelnistvo','Pročelništvo'],['#povijest','Povijest'],['#duh','Velebitaški duh'],['velebiten.html','Velebiten'],['pridruzi-nam-se.html','Pridruži nam se']];
    nav.innerHTML=map.map(([href,label])=>`<a href="${href}">${label}</a>`).join('');
  }
  function fixThumbs(){
    document.querySelectorAll('.news-img,.archive-img').forEach(el=>{
      const s=(el.getAttribute('style')||'').toLowerCase();
      if(!s || s.includes('1f60a.svg') || s.includes('placeholder') || s.includes('undefined') || s.includes("url('')") || s.includes('url("")')){
        el.classList.add('sov-thumb-fallback');
        el.removeAttribute('style');
      }
    });
  }
  function accessibilityPass(){
    document.querySelectorAll('img:not([alt])').forEach(img=>img.setAttribute('alt',''));
    document.querySelectorAll('a.news-card,a.news-side-card,a.news-featured,a.archive-card').forEach(a=>{
      const title=a.querySelector('strong')?.textContent?.trim();
      if(title && !a.getAttribute('aria-label')) a.setAttribute('aria-label',title);
    });
  }
  document.addEventListener('DOMContentLoaded',()=>{
    document.body.classList.add('sov-public-final');
    normalizeLinks();
    installHeader();
    normalizeHomepageHero();
    simplifyHomepageCards();
    normalizeLinks();
    fixAboutSubnav();
    fixThumbs();
    accessibilityPass();
  });
})();