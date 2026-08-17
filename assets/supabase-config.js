// SOV Cloud Supabase config
window.SOV_SUPABASE_URL = 'https://ncomefzkuixyfixisrhi.supabase.co';
window.SOV_SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im5jb21lZnprdWl4eWZpeGlzcmhpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk1ODQwOTYsImV4cCI6MjA5NTE2MDA5Nn0.WFSiENYXv48Npaz7vFcY-ksYvg_Ja40iNGsEqb1nUDk';

// Password recovery fallback. If Supabase/email template sends the user to Site URL (/),
// preserve the auth payload and continue on the dedicated reset form instead of leaving them on home.
(function(){
  try{
    const name=(location.pathname||'/').split('/').filter(Boolean).pop()||'index.html';
    if(name!=='index.html') return;
    const q=new URLSearchParams(location.search||'');
    const h=new URLSearchParams((location.hash||'').replace(/^#/,''));
    const type=String(q.get('type')||h.get('type')||'').toLowerCase();
    let pending=false;
    try{pending=localStorage.getItem('sov_password_recovery_pending')==='1'}catch(e){}
    const recovery=type==='recovery'||(!!h.get('access_token')&&type==='recovery')||(!!q.get('code')&&pending);
    const recoveryError=(type==='recovery')&&(q.get('error')||q.get('error_code')||h.get('error')||h.get('error_code'));
    if(recovery||recoveryError){
      document.documentElement.style.visibility='hidden';
      location.replace('reset-password.html'+(location.search||'')+(location.hash||''));
      return;
    }
  }catch(e){console.warn('[SOV auth] recovery landing skipped',e)}
})();

// Auth pre-check: protected member pages stay hidden until auth.js assigns an approved role.
// Prevents a logged-out visitor from seeing a flash of dashboard/member content before redirect.
(function(){
  try{
    const name=(location.pathname||'/').split('/').filter(Boolean).pop()||'index.html';
    const protectedExact=new Set(['dashboard.html','karta.html','pregled-baze.html','izleti.html','izleti-cloud.html','kalendar-izleta.html','dokumentacija.html','dokumenti.html','pregled-zapisnika.html','zapisnici-skupstine.html','zapisnici-aktualni-2026.html','zapisnici-arhiva-2017-2022.html','zapisnici-cijela-arhiva.html','zapisnici-import.html','zapisnici-najave.html','novi-zapisnik.html','speleo-zapisnik.html','topodroid.html','napisi-clanak.html','predaj-novu-jamu.html','admin-users.html','admin-notifications.html','role-manager.html','news-editor.html','sync-status.html','audit-status.html','system-status.html','sov-system-status.html','status.html']);
    const protectedPrefix=/^(arhivar|oruzar-master|oruzarstvo-import|speleo-sql-)/;
    if(!protectedExact.has(name)&&!protectedPrefix.test(name)) return;
    const st=document.createElement('style');
    st.id='sov-auth-precheck-style';
    st.textContent='html.sov-auth-precheck body{visibility:hidden!important}html.sov-auth-precheck{background:#e8e0d3!important}';
    document.head.appendChild(st);
    document.documentElement.classList.add('sov-auth-precheck');
    const approved=()=>{
      const b=document.body;
      if(!b) return false;
      return ['role-webmaster','role-admin','role-editor','role-oruzar','role-arhivar','role-user'].some(c=>b.classList.contains(c));
    };
    const release=()=>{if(approved())document.documentElement.classList.remove('sov-auth-precheck')};
    document.addEventListener('DOMContentLoaded',()=>{
      release();
      if(document.body)new MutationObserver(release).observe(document.body,{attributes:true,attributeFilter:['class']});
      document.addEventListener('click',e=>{if(e.target&&e.target.closest&&e.target.closest('[data-logout]'))document.documentElement.classList.add('sov-auth-precheck')},{capture:true});
      setTimeout(()=>{if(location.pathname.toLowerCase().endsWith(name.toLowerCase())&&approved())release()},1200);
    },{once:true});
  }catch(e){console.warn('[SOV auth] pre-check skipped',e)}
})();

// v5.30 Nacrti sync endpoint — SOV Drawings Index WebApp v2.0.2 FAST SEARCH.
window.SOV_DRAWINGS_SYNC_ENDPOINT = window.SOV_DRAWINGS_SYNC_ENDPOINT || 'https://script.google.com/macros/s/AKfycbx1Hg_s6mAdWgB7p559USC8dAMIhteJQ3RFhFgp8rkqzYEVqMfwZm-lrl2v7UmW8gvSyg/exec';
window.SOV_GOOGLE_DRIVE_API_KEY = window.SOV_GOOGLE_DRIVE_API_KEY || '';

// v5.24 hard-restored trips calendar Apps Script endpoint
window.SOV_TRIPS_WEBAPP_URL = window.SOV_TRIPS_WEBAPP_URL || 'https://script.google.com/macros/s/AKfycbybGi7p6_ImXAXEErJ6P9K0GYHy8lHW850K9cQe2py8yUV2oJO6UW1DJi00quorVTHOGQ/exec';

// v6.1.48: member Oružarstvo human UI + captured soft-refresh hook.
(function(){
  try{
    const path=String(location.pathname||'').toLowerCase();
    if(!path.endsWith('/oruzarstvo.html')&&!path.endsWith('oruzarstvo.html')) return;

    const nativeAdd=document.addEventListener.bind(document);
    document.addEventListener=function(type,handler,options){
      try{
        if(type==='DOMContentLoaded'&&typeof handler==='function'){
          const src=Function.prototype.toString.call(handler);
          if(src.includes('loadCatalog')&&src.includes('renderCart')){
            window.__SOV_ARMORY_PAGE_INIT=handler;
          }
        }
      }catch(e){}
      return nativeAdd(type,handler,options);
    };

    [
      'assets/sov-armory-human-v6148-compat-a.css?v=6.1.48',
      'assets/sov-armory-human-v6148-compat-b.css?v=6.1.48'
    ].forEach(href=>{
      const link=document.createElement('link');
      link.rel='stylesheet';
      link.href=href;
      link.setAttribute('data-sov-armory-human','');
      document.head.appendChild(link);
    });

    const load=()=>{
      document.addEventListener=nativeAdd;
      if(document.querySelector('script[data-sov-armory-human]'))return;
      const script=document.createElement('script');
      script.src='assets/sov-armory-human-v6148.js?v=6.1.48';
      script.setAttribute('data-sov-armory-human','');
      document.body.appendChild(script);
    };
    nativeAdd('DOMContentLoaded',load,{once:true});
  }catch(e){console.warn('[SOV armory] human layer skipped',e);}
})();
