// SOV Web v6.0.5 — single frontend version helper.
(function(){
  'use strict';
  const EXPECTED='6.0.5';
  const CACHE='605';
  window.SOV_BUILD={version:EXPECTED, versionName:'v6.0.5-system-health-center', build:'sov-web-build-v6.0.5-system-health-center', cacheBust:CACHE};
  function setText(sel, value){ document.querySelectorAll(sel).forEach(el=>{ el.textContent=value; }); }
  function patchStatic(){
    document.documentElement.dataset.sovVersion=EXPECTED;
    document.body && (document.body.dataset.sovVersion=EXPECTED);
    setText('[data-sov-version]', EXPECTED);
    setText('[data-sov-build]', 'sov-web-build-v6.0.5-system-health-center');
    setText('[data-sov-version-name]', 'v6.0.5-system-health-center');
    try{ document.title=document.title.replace(/v\d+\.\d+(?:\.\d+)?/g,'v'+EXPECTED); }catch(e){}
  }
  async function loadManifest(){
    try{
      const res=await fetch('update.json?cb='+Date.now(), {cache:'no-store'});
      if(!res.ok) throw new Error('HTTP '+res.status);
      const m=await res.json();
      window.SOV_UPDATE_MANIFEST=m;
      setText('[data-sov-manifest-version]', m.version || EXPECTED);
      setText('[data-sov-manifest-build]', m.build || 'sov-web-build-v6.0.5-system-health-center');
      const ok=(m.version===EXPECTED);
      document.documentElement.dataset.sovVersionContract=ok?'ok':'mismatch';
      window.dispatchEvent(new CustomEvent('sov:version', {detail:{ok,expected:EXPECTED,manifest:m}}));
    }catch(err){
      document.documentElement.dataset.sovVersionContract='unknown';
      window.dispatchEvent(new CustomEvent('sov:version', {detail:{ok:false,expected:EXPECTED,error:String(err&&err.message||err)}}));
    }
  }
  if(document.readyState==='loading') document.addEventListener('DOMContentLoaded',()=>{ patchStatic(); loadManifest(); });
  else { patchStatic(); loadManifest(); }
})();
