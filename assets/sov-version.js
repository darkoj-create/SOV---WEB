// SOV Web version helper.
// Also injects the approved-member Nacrt Generator entry on the main Cloud dashboard.
(function(){
  'use strict';
  const FALLBACK_VERSION='6.1.45ay';
  const FALLBACK_CACHE='6145ay-trips-auto-upcoming';
  const FALLBACK_BUILD='sov-web-build-v6.1.45ay-trips-auto-upcoming';
  const FALLBACK_NAME='v6.1.45ay-trips-auto-upcoming';
  window.SOV_BUILD={version:FALLBACK_VERSION,versionName:FALLBACK_NAME,build:FALLBACK_BUILD,cacheBust:FALLBACK_CACHE};

  function safeSetText(sel,value){
    try{document.querySelectorAll(sel).forEach(el=>{if(!el||el===document.body||el===document.documentElement)return;el.textContent=value;});}catch(e){}
  }
  function applyVersion(v,b,n){
    v=v||FALLBACK_VERSION;b=b||FALLBACK_BUILD;n=n||FALLBACK_NAME;
    try{document.documentElement.dataset.sovBuildVersion=v;}catch(e){}
    try{if(document.body)document.body.dataset.sovBuildVersion=v;}catch(e){}
    safeSetText('[data-sov-version]',v);
    safeSetText('[data-sov-build]',b);
    safeSetText('[data-sov-version-name]',n);
    try{document.title=document.title.replace(/v\d+\.\d+(?:\.\d+)?/g,'v'+v);}catch(e){}
  }

  function injectNacrtDashboardCard(){
    try{
      if(!document.body||!document.body.classList.contains('dashboard-page')) return;
      if(document.getElementById('sov-nacrt-generator-card')) return;
      const grid=document.querySelector('.v609-grid-primary, .sov-grid[aria-label="Moje stvari"]');
      if(!grid) return;
      const card=document.createElement('a');
      card.id='sov-nacrt-generator-card';
      card.className='sov-module v609-module';
      card.href='nacrt.html';
      card.setAttribute('data-dash-visible','user,editor,oruzar,arhivar,admin,webmaster');
      card.setAttribute('data-dash-ability','drawings');
      card.style.setProperty('--accent','rgba(171,196,255,.18)');
      card.innerHTML='<div class="sov-icon">📐</div><h3>Nacrt Generator</h3><p>Učitaj TopoDroid ZIP i izradi semantički SOV nacrt s točnim materijalima, drvećem, kamenjem, scrapovima i Velebitovim logotipima.</p><div class="sov-module-foot"><span class="sov-tag">Cloud</span><span class="sov-soft">Otvori</span></div>';
      grid.appendChild(card);
    }catch(e){console.warn('Nacrt dashboard card skipped',e);}
  }

  function injectTripsHumanLayer(){
    try{
      const path=String(location.pathname||'').toLowerCase();
      if(!path.endsWith('/izleti-cloud.html')&&!path.endsWith('izleti-cloud.html')) return;
      if(!document.querySelector('link[data-sov-trips-human]')){
        const link=document.createElement('link');
        link.rel='stylesheet';
        link.href='assets/sov-trips-human-v6146.css?v=6.1.46';
        link.setAttribute('data-sov-trips-human','');
        document.head.appendChild(link);
      }
      if(!document.querySelector('script[data-sov-trips-human]')){
        const script=document.createElement('script');
        script.src='assets/sov-trips-human-v6146.js?v=6.1.46';
        script.setAttribute('data-sov-trips-human','');
        document.body.appendChild(script);
      }
    }catch(e){console.warn('Trips human layer skipped',e);}
  }

  async function loadManifest(){
    applyVersion(FALLBACK_VERSION,FALLBACK_BUILD,FALLBACK_NAME);
    injectNacrtDashboardCard();
    injectTripsHumanLayer();
    try{
      const res=await fetch('/update.json?cb='+Date.now(),{cache:'no-store'});
      if(!res.ok)throw new Error('HTTP '+res.status);
      const m=await res.json();
      window.SOV_UPDATE_MANIFEST=m;
      const v=m.version||FALLBACK_VERSION,b=m.build||FALLBACK_BUILD,n=m.versionName||FALLBACK_NAME;
      window.SOV_BUILD={version:v,versionName:n,build:b,cacheBust:m.cacheBust||FALLBACK_CACHE};
      applyVersion(v,b,n);
      safeSetText('[data-sov-manifest-version]',v);
      safeSetText('[data-sov-manifest-build]',b);
      document.documentElement.dataset.sovVersionContract='ok';
      window.dispatchEvent(new CustomEvent('sov:version',{detail:{ok:true,expected:v,manifest:m}}));
    }catch(err){
      document.documentElement.dataset.sovVersionContract='unknown';
      window.dispatchEvent(new CustomEvent('sov:version',{detail:{ok:false,expected:FALLBACK_VERSION,error:String(err&&err.message||err)}}));
    }
    injectNacrtDashboardCard();
    injectTripsHumanLayer();
  }

  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',loadManifest);
  else loadManifest();
  setTimeout(injectNacrtDashboardCard,350);
  setTimeout(injectNacrtDashboardCard,1200);
  setTimeout(injectTripsHumanLayer,80);
})();
