// SOV Web 6.1.8 — safe frontend version helper.
// Fix: never writes textContent into <html> or <body>; older v6.1.1 could wipe the page because body.dataset.sovVersion matched [data-sov-version].
(function(){
  'use strict';
  const FALLBACK_VERSION='6.1.8';
  const FALLBACK_CACHE='618';
  const FALLBACK_BUILD='sov-web-build-v6.1.8-documents-current-minutes-2026';
  const FALLBACK_NAME='v6.1.8-documents-current-minutes-2026';
  window.SOV_BUILD={version:FALLBACK_VERSION, versionName:FALLBACK_NAME, build:FALLBACK_BUILD, cacheBust:FALLBACK_CACHE};
  function safeSetText(sel, value){
    try{
      document.querySelectorAll(sel).forEach(el=>{
        if(!el || el===document.body || el===document.documentElement) return;
        // Only small explicit labels should be rewritten, never layout containers.
        el.textContent=value;
      });
    }catch(e){}
  }
  function applyVersion(v, b, n){
    v = v || FALLBACK_VERSION; b = b || FALLBACK_BUILD; n = n || FALLBACK_NAME;
    try{ document.documentElement.dataset.sovBuildVersion=v; }catch(e){}
    try{ if(document.body) document.body.dataset.sovBuildVersion=v; }catch(e){}
    safeSetText('[data-sov-version]', v);
    safeSetText('[data-sov-build]', b);
    safeSetText('[data-sov-version-name]', n);
    try{ document.title=document.title.replace(/v\d+\.\d+(?:\.\d+)?/g,'v'+v); }catch(e){}
  }
  async function loadManifest(){
    applyVersion(FALLBACK_VERSION,FALLBACK_BUILD,FALLBACK_NAME);
    try{
      const res=await fetch('update.json?cb='+Date.now(), {cache:'no-store'});
      if(!res.ok) throw new Error('HTTP '+res.status);
      const m=await res.json();
      window.SOV_UPDATE_MANIFEST=m;
      const v=m.version || FALLBACK_VERSION;
      const b=m.build || FALLBACK_BUILD;
      const n=m.versionName || FALLBACK_NAME;
      window.SOV_BUILD={version:v, versionName:n, build:b, cacheBust:m.cacheBust || FALLBACK_CACHE};
      applyVersion(v,b,n);
      safeSetText('[data-sov-manifest-version]', v);
      safeSetText('[data-sov-manifest-build]', b);
      document.documentElement.dataset.sovVersionContract='ok';
      window.dispatchEvent(new CustomEvent('sov:version', {detail:{ok:true,expected:v,manifest:m}}));
    }catch(err){
      document.documentElement.dataset.sovVersionContract='unknown';
      window.dispatchEvent(new CustomEvent('sov:version', {detail:{ok:false,expected:FALLBACK_VERSION,error:String(err&&err.message||err)}}));
    }
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded', loadManifest);
  else loadManifest();
})();
