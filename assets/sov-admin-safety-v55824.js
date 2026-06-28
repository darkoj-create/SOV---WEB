
(function(){
  function ready(fn){if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',fn);else fn()}
  const dangerWords=/(promoviraj|rollback|go live|produkcij|izvrši|pokreni|primijeni|spremi|obriši|delete|drop|truncate|update|insert|upsert|sql)/i;
  const safeWords=/(prikaži|učitaj|refresh|traži|kopiraj|export|download|nazad|otvori)/i;
  function shouldGuard(btn){
    if(btn.dataset.sovGuarded==='no')return false;
    const text=(btn.textContent||btn.value||btn.getAttribute('aria-label')||'').trim();
    const handler=btn.getAttribute('onclick')||'';
    if(!dangerWords.test(text+' '+handler))return false;
    if(safeWords.test(text) && !/(go live|promoviraj|rollback|izvrši|pokreni|primijeni|spremi|obriši|delete|drop|truncate|update|insert|upsert)/i.test(text+' '+handler)) return false;
    return true;
  }
  ready(function(){
    if(!/^\/?.*speleo-sql-/i.test(location.pathname.replace(/^\//,'')))return;
    if(window.SOVAuth && SOVAuth.requireWebmaster){
      SOVAuth.requireWebmaster().then(ok=>{
        if(!ok) document.body.innerHTML='<main style="max-width:760px;margin:48px auto;padding:24px;font-family:Inter,system-ui,sans-serif;color:#eef8f2;background:#0b1114;border:1px solid rgba(255,255,255,.14);border-radius:24px"><h1>Webmaster pristup</h1><p>SQL alati su zaključani samo za Webmaster rolu.</p><p><a href="dashboard.html" style="color:#d7f66f;font-weight:900">Natrag na Dashboard</a></p></main>';
      }).catch(()=>{});
    }
    const banner=document.createElement('div');
    banner.className='sov-sql-production-banner';
    banner.innerHTML='<b>PRODUKCIJSKI SQL ALAT</b> — koristi samo Webmaster. Prije svake radnje koja mijenja bazu traži se dvostruka potvrda.<small>Za produkcijske promjene prvo provjeri scope, broj redaka i rollback mogućnost.</small>';
    const target=document.querySelector('.hero,.wrap,.panel,main,body');
    if(target){ if(target===document.body) document.body.insertBefore(banner,document.body.firstChild); else target.insertAdjacentElement('afterend',banner); }
    document.addEventListener('click',function(ev){
      const btn=ev.target.closest('button,input[type="button"],input[type="submit"],a.btn');
      if(!btn || !shouldGuard(btn))return;
      if(btn.dataset.sovGuardPass==='1'){delete btn.dataset.sovGuardPass; return;}
      ev.preventDefault(); ev.stopPropagation(); ev.stopImmediatePropagation();
      btn.classList.add('sov-sql-danger-armed');
      const label=(btn.textContent||btn.value||'ovu SQL radnju').trim();
      const ok1=confirm('OPREZ: pokrećeš produkcijsku/SQL radnju: '+label+'\n\nNastaviti samo ako znaš što mijenjaš.');
      if(!ok1){btn.classList.remove('sov-sql-danger-armed');return;}
      const typed=prompt('Za potvrdu upiši točno: PRODUKCIJA');
      if(typed!=='PRODUKCIJA'){alert('Radnja nije pokrenuta.');btn.classList.remove('sov-sql-danger-armed');return;}
      btn.dataset.sovGuardPass='1';
      setTimeout(()=>{btn.click();btn.classList.remove('sov-sql-danger-armed');},0);
    },true);
  });
})();
