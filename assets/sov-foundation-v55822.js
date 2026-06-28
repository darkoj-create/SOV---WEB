
(function(){
  const VERSION='5.58.22';
  function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn);else fn()}
  function esc(s){return String(s||'').replace(/[&<>'"]/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[m]))}
  function setupIndexHome(){
    const path=(location.pathname||'').replace(/\\/g,'/');
    if(path && !/(^|\/)(index\.html?)?$/i.test(path)) return;
    document.body.classList.add('index-premium-home');
    const header=document.querySelector('.topbar');
    const nav=header&&header.querySelector('.nav');
    const links=header&&header.querySelector('.navlinks-main');
    if(!header||!nav||!links) return;
    links.id=links.id||'indexMobileNav';
    links.setAttribute('data-index-nav-panel','1');
    if(!header.querySelector('.index-menu-toggle')){
      const btn=document.createElement('button');
      btn.type='button';
      btn.className='index-menu-toggle';
      btn.setAttribute('aria-label','Otvori izbornik');
      btn.setAttribute('aria-controls',links.id);
      btn.setAttribute('aria-expanded','false');
      btn.innerHTML='<span class="index-menu-glyph" aria-hidden="true">☰</span>';
      const brand=nav.querySelector('.brand-sov,.brand');
      nav.insertBefore(btn, brand ? brand.nextSibling : links);
    }
    const menuButton=header.querySelector('.index-menu-toggle') || document.querySelector('.index-menu-toggle');
    if(menuButton && menuButton.parentNode!==document.body) document.body.appendChild(menuButton);
    const isMobile=()=>matchMedia('(max-width:980px)').matches;
    const syncMenuButton=()=>{
      if(!menuButton) return;
      if(isMobile()){
        const left=Math.max(12,Math.min((window.innerWidth||390)-58,320));
        menuButton.style.cssText='position:fixed;left:'+left+'px;top:15px;z-index:9999;display:flex;align-items:center;justify-content:center;width:46px;height:46px;border-radius:16px;border:1px solid rgba(255,255,255,.2);background:linear-gradient(135deg,#d7ff52,#6fe7cf);color:#07100d;font-size:24px;font-weight:1000;box-shadow:0 16px 42px rgba(0,0,0,.32);';
      } else {
        menuButton.removeAttribute('style');
      }
    };
    const syncDrawer=()=>{
      const open=document.body.classList.contains('index-menu-open');
      if(!isMobile()){
        links.removeAttribute('style');
        return;
      }
      const base='position:fixed;left:10px;right:10px;top:72px;z-index:9998;max-height:calc(100svh - 86px);overflow:auto;display:grid;grid-template-columns:1fr;gap:10px;padding:14px;border:1px solid rgba(255,255,255,.16);border-radius:24px;background:linear-gradient(180deg,rgba(8,13,15,.98),rgba(5,8,10,.96));box-shadow:0 32px 100px rgba(0,0,0,.62);transition:transform .18s ease,opacity .18s ease;';
      links.style.cssText=base+(open?'opacity:1;pointer-events:auto;transform:translateY(0) scale(1);':'opacity:0;pointer-events:none;transform:translateY(-12px) scale(.98);');
    };
    syncMenuButton();
    syncDrawer();
    window.addEventListener('resize',()=>{syncMenuButton(); syncDrawer();},{passive:true});
    if(!links.querySelector('.facebook-link')){
      const fb=document.createElement('a');
      fb.className='facebook-link';
      fb.href='https://www.facebook.com/sovelebit';
      fb.target='_blank';
      fb.rel='noopener';
      fb.setAttribute('aria-label','Otvori SOV Velebit Facebook stranicu');
      fb.innerHTML='<span class="fb-mark" aria-hidden="true">f</span><span>Facebook</span>';
      links.appendChild(fb);
    }
    const heroActions=document.querySelector('.portal-hero .hero-actions');
    if(heroActions && !heroActions.querySelector('.hero-secondary')){
      const gallery=document.createElement('a');
      gallery.className='hero-secondary';
      gallery.href='#galerija';
      gallery.innerHTML='Pogledaj galeriju <span>↓</span>';
      heroActions.appendChild(gallery);
    }
    const heroInner=document.querySelector('.portal-hero-inner');
    if(heroInner && !heroInner.querySelector('.index-hero-chips')){
      const chips=document.createElement('div');
      chips.className='index-hero-chips';
      chips.innerHTML='<span>Terenski dnevnik</span><span>Speleoškola</span><span>Ekspedicije</span>';
      heroInner.appendChild(chips);
    }
    const footer=document.querySelector('.footer');
    if(footer && /v4\.10/i.test(footer.textContent||'')){
      footer.textContent='Speleološki odsjek PDS Velebit · SOV portal · v5.58.24';
    }
    const btn=menuButton || document.querySelector('.index-menu-toggle');
    const close=()=>{document.body.classList.remove('index-menu-open'); btn&&btn.setAttribute('aria-expanded','false'); syncDrawer();};
    const toggle=()=>{const open=!document.body.classList.contains('index-menu-open'); document.body.classList.toggle('index-menu-open',open); btn&&btn.setAttribute('aria-expanded',String(open)); syncDrawer();};
    if(btn && !btn.dataset.bound){
      btn.dataset.bound='1';
      btn.addEventListener('click',toggle);
      links.addEventListener('click',e=>{ if(e.target.closest('a')) close(); });
      document.addEventListener('keydown',e=>{ if(e.key==='Escape') close(); });
    }
  }
  ready(function(){
    document.documentElement.classList.add('sov-foundation-loaded');
    setupIndexHome();
    // Add scroll hint wrapper to every horizontal nav row without changing markup semantics.
    document.querySelectorAll('.nav,.navlinks,.dash-nav,.aw-nav,.az-nav,.armory-nav,.tabs,.aw-tabs,.om-tabs,.topbar nav').forEach(nav=>{
      if(document.body.classList.contains('index-premium-home') && nav.closest('.topbar')) return;
      if(nav.closest('.sov-scroll-wrap')) return;
      const cs=getComputedStyle(nav);
      const likelyScrollable=cs.overflowX==='auto'||cs.overflowX==='scroll'||nav.scrollWidth>nav.clientWidth+8;
      if(!likelyScrollable && !matchMedia('(max-width:760px)').matches) return;
      const wrap=document.createElement('div');
      wrap.className='sov-scroll-wrap';
      nav.parentNode.insertBefore(wrap,nav);
      wrap.appendChild(nav);
      nav.classList.add('sov-scroll-row');
      const update=()=>wrap.classList.toggle('at-end', nav.scrollLeft+nav.clientWidth>=nav.scrollWidth-8);
      nav.addEventListener('scroll',update,{passive:true});
      setTimeout(update,80); window.addEventListener('resize',update,{passive:true});
    });
    // Make legacy tables readable on mobile by turning rows into cards.
    document.querySelectorAll('table').forEach(table=>{
      if(table.classList.contains('no-mobile-card')) return;
      const headers=[...table.querySelectorAll('thead th')].map(th=>th.textContent.trim()).filter(Boolean);
      table.classList.add('sov-mobile-card-table');
      table.querySelectorAll('tbody tr').forEach(tr=>{
        [...tr.children].forEach((cell,i)=>{
          if(!cell.getAttribute('data-label')){
            const twoCol = tr.children.length===2 && !headers.length;
            const label = headers[i] || (twoCol && i===1 ? (tr.children[0].textContent.trim()||'Podatak') : (i===0?'Podatak':'Vrijednost'));
            cell.setAttribute('data-label',label);
          }
        });
      });
    });
    // Armory catalog: show a humane skeleton while the large monolith waits for async Supabase data.
    if(/oruzarstvo\.html$/i.test(location.pathname)){
      const catalog=document.getElementById('catalog')||document.querySelector('.grid-list');
      if(catalog && !catalog.querySelector('.item-card,.empty')){
        const box=document.createElement('div');
        box.className='sov-armory-skeleton';
        box.innerHTML='<div class="sov-skeleton-note">Učitavam opremu…</div>'+Array.from({length:6}).map(()=>'<div class="sov-skeleton-card"></div>').join('');
        catalog.appendChild(box);
        const obs=new MutationObserver(()=>{
          if(catalog.querySelector('.item-card,.empty')){box.classList.add('is-gone'); setTimeout(()=>box.remove(),250); obs.disconnect();}
        });
        obs.observe(catalog,{childList:true,subtree:true});
        setTimeout(()=>{ if(document.body.contains(box)) box.querySelector('.sov-skeleton-note').textContent='Još učitavam katalog opreme…'; },3500);
      }
    }
  });
})();
