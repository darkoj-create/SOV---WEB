
(function(){
  const VERSION='5.58.22';
  function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn);else fn()}
  function esc(s){return String(s||'').replace(/[&<>'"]/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[m]))}
  ready(function(){
    document.documentElement.classList.add('sov-foundation-loaded');
    // Add scroll hint wrapper to every horizontal nav row without changing markup semantics.
    document.querySelectorAll('.nav,.navlinks,.dash-nav,.aw-nav,.az-nav,.armory-nav,.tabs,.aw-tabs,.om-tabs,.topbar nav').forEach(nav=>{
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
