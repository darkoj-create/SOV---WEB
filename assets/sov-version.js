// SOV Web 6.1.1 — frontend version helper hotfix.
(function(){
  'use strict';
  const FALLBACK_VERSION='6.1.1';
  const FALLBACK_CACHE='611';
  const FALLBACK_BUILD='sov-web-build-v6.1.1-version-helper-hotfix';
  const FALLBACK_NAME='v6.1.1-version-helper-hotfix';
  window.SOV_BUILD={version:FALLBACK_VERSION, versionName:FALLBACK_NAME, build:FALLBACK_BUILD, cacheBust:FALLBACK_CACHE};
  function setText(sel, value){
    try{ document.querySelectorAll(sel).forEach(el=>{ el.textContent=value; }); }catch(e){}
  }
  function applyVersion(v, b, n){
    v = v || FALLBACK_VERSION; b = b || FALLBACK_BUILD; n = n || FALLBACK_NAME;
    try{ document.documentElement.dataset.sovVersion=v; }catch(e){}
    try{ if(document.body) document.body.dataset.sovVersion=v; }catch(e){}
    setText('[data-sov-version]', v);
    setText('[data-sov-build]', b);
    setText('[data-sov-version-name]', n);
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
      setText('[data-sov-manifest-version]', v);
      setText('[data-sov-manifest-build]', b);
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
