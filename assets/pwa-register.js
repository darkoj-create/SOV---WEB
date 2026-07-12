(function(){
  'use strict';
  const VERSION = '6.1.46a-pwa-step1';
  const BANNER_ID = 'sov-pwa-update-banner';

  function ready(fn){
    if(document.readyState === 'loading') document.addEventListener('DOMContentLoaded', fn, {once:true});
    else fn();
  }

  function showBanner(worker){
    if(document.getElementById(BANNER_ID)) return;
    const bar = document.createElement('div');
    bar.id = BANNER_ID;
    bar.setAttribute('role', 'status');
    bar.style.cssText = 'position:fixed;left:12px;right:12px;bottom:12px;z-index:2147483647;display:flex;align-items:center;justify-content:space-between;gap:12px;border:1px solid rgba(255,255,255,.18);border-radius:18px;background:rgba(5,8,9,.94);color:#eef8f2;padding:12px 14px;box-shadow:0 18px 60px rgba(0,0,0,.45);font:900 14px Inter,system-ui,-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;backdrop-filter:blur(14px)';
    bar.innerHTML = '<span>Nova verzija SOV-a je dostupna.</span><button type="button" style="border:0;border-radius:999px;background:#d7f66f;color:#101510;font-weight:1000;padding:9px 12px;cursor:pointer">Osvježi</button>';
    bar.querySelector('button').addEventListener('click', function(){
      if(worker) worker.postMessage({type:'SOV_SKIP_WAITING'});
    });
    document.body.appendChild(bar);
  }

  function register(){
    if(!('serviceWorker' in navigator)) return;
    navigator.serviceWorker.register('/sw.js?v=' + encodeURIComponent(VERSION), {scope:'/'}).then(function(reg){
      if(reg.waiting) showBanner(reg.waiting);
      reg.addEventListener('updatefound', function(){
        const worker = reg.installing;
        if(!worker) return;
        worker.addEventListener('statechange', function(){
          if(worker.state === 'installed' && navigator.serviceWorker.controller){
            showBanner(worker);
          }
        });
      });
    }).catch(function(err){
      try{ console.warn('SOV PWA registracija nije uspjela:', err); }catch(_){ }
    });

    let refreshing = false;
    navigator.serviceWorker.addEventListener('controllerchange', function(){
      if(refreshing) return;
      refreshing = true;
      window.location.reload();
    });
  }

  ready(register);
})();
