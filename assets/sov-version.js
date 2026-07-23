// SOV Web version helper.
// Also injects approved, page-specific member helpers.
(function(){
  'use strict';
  const FALLBACK_VERSION='6.1.49';
  const FALLBACK_CACHE='6149-trips-single-loader';
  const FALLBACK_BUILD='sov-web-build-v6.1.49-trips-single-loader';
  const FALLBACK_NAME='v6.1.49-trips-single-loader';
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

  function normalizeMemberPageCopy(){
    try{
      const path=String(location.pathname||'').toLowerCase();
      const set=(title,selector='h1')=>{const el=document.querySelector(selector);if(el)el.textContent=title;document.title=title+' — SOV Velebit';};
      if(path.endsWith('/dokumenti.html')||path.endsWith('dokumenti.html'))set('Dokumenti','.docs-hero h1');
      else if(path.endsWith('/tracking.html')||path.endsWith('tracking.html'))set('Praćenje izleta','.tracking-hero-card h1');
      else if(path.endsWith('/predaj-novu-jamu.html')||path.endsWith('predaj-novu-jamu.html'))set('Predaj novu jamu','.sf-hero h1');
      else if(path.endsWith('/napisi-clanak.html')||path.endsWith('napisi-clanak.html'))set('Napiši članak','.hero h1');
      else if(path.endsWith('/nacrt.html')||path.endsWith('nacrt.html')){
        set('Nacrt','.nacrt-head h1');
        if(document.body){document.body.style.setProperty('background','linear-gradient(180deg,#e9e2d5,#f5f0e7 52%,#e8e0d2)','important');document.body.style.setProperty('color','#252a24','important');}
      }
    }catch(e){console.warn('Member page copy cleanup skipped',e);}
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
      card.innerHTML='<div class="sov-icon">📐</div><h3>Nacrt</h3><div class="sov-module-foot"><span class="sov-soft">Otvori</span></div>';
      grid.appendChild(card);
    }catch(e){console.warn('Nacrt dashboard card skipped',e);}
  }

  function injectArticleDraftHelper(){
    try{
      const path=String(location.pathname||'').toLowerCase();
      if(!path.endsWith('/napisi-clanak.html')&&!path.endsWith('napisi-clanak.html'))return;
      if(document.querySelector('script[data-sov-article-member]'))return;
      const script=document.createElement('script');
      script.src='assets/sov-article-member-v6150.js?v=6.1.50';
      script.setAttribute('data-sov-article-member','');
      document.body.appendChild(script);
    }catch(e){console.warn('Article draft helper skipped',e);}
  }

  function injectTripsHumanLayerOnce(){
    try{
      const path=String(location.pathname||'').toLowerCase();
      if(!path.endsWith('/izleti-cloud.html')&&!path.endsWith('izleti-cloud.html'))return;
      if(window.__SOV_TRIPS_HUMAN_LAYER)return;
      window.__SOV_TRIPS_HUMAN_LAYER=true;

      const root=document.documentElement;
      root.classList.add('sov-trips-style-pending');
      const veil=document.createElement('style');
      veil.id='sovTripsStyleVeil';
      veil.textContent='html.sov-trips-style-pending body{visibility:hidden!important}';
      document.head.appendChild(veil);

      const clean=document.createElement('style');
      clean.id='sovTripsCopyCleanup';
      clean.textContent='body.sov-trips-human .trips-hero-subtitle,body.sov-trips-human .heroText>.muted,body.sov-trips-human .heroStat,body.sov-trips-human .tripCard .desc,body.sov-trips-human .trip-assets-head p,body.sov-trips-human .trip-assets-note{display:none!important}body.sov-trips-human .tripCard{min-height:118px!important}';
      document.head.appendChild(clean);

      const reveal=()=>{
        if(document.body)document.body.classList.add('sov-trips-human');
        root.classList.remove('sov-trips-style-pending');
        if(veil.isConnected)veil.remove();
      };
      const loadScript=()=>{
        if(document.querySelector('script[data-sov-trips-human]'))return;
        const script=document.createElement('script');
        script.src='assets/sov-trips-human-v6146.js?v=6.1.49';
        script.setAttribute('data-sov-trips-human','');
        script.onload=reveal;
        script.onerror=reveal;
        document.body.appendChild(script);
      };

      const link=document.createElement('link');
      link.rel='stylesheet';
      link.href='assets/sov-trips-human-v6146.css?v=6.1.49';
      link.setAttribute('data-sov-trips-human','');
      link.onload=()=>{reveal();loadScript();};
      link.onerror=()=>{reveal();loadScript();};
      document.head.appendChild(link);
      setTimeout(()=>{reveal();loadScript();},2500);
    }catch(e){
      document.documentElement.classList.remove('sov-trips-style-pending');
      console.warn('Trips human layer skipped',e);
    }
  }

  async function loadManifest(){
    applyVersion(FALLBACK_VERSION,FALLBACK_BUILD,FALLBACK_NAME);
    normalizeMemberPageCopy();
    injectNacrtDashboardCard();
    injectArticleDraftHelper();
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
    normalizeMemberPageCopy();
    injectNacrtDashboardCard();
    injectArticleDraftHelper();
  }

  injectTripsHumanLayerOnce();
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',loadManifest);
  else loadManifest();
  setTimeout(injectNacrtDashboardCard,350);
  setTimeout(injectNacrtDashboardCard,1200);
})();
