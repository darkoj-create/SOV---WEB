(function(){
  if(window.SOVClientLogger) return;
  const VERSION='6.1.45ax';
  const IS_LOCAL=/^(localhost|127\.0\.0\.1|0\.0\.0\.0)$/i.test(location.hostname||'');
  let busy=false;
  const lastSent=Object.create(null);

  function safeText(v,max){return String(v==null?'':v).slice(0,max||500)}
  function deviceInfo(){return{userAgent:navigator.userAgent||'',language:navigator.language||'',viewport:(innerWidth||0)+'x'+(innerHeight||0),path:location.pathname,origin:location.origin,online:navigator.onLine!==false}}
  async function profile(){try{if(window.SOVAuth&&SOVAuth.getProfile)return await SOVAuth.getProfile()}catch(e){}return null}
  function client(){try{if(window.SOVAuth&&SOVAuth.getClient)return SOVAuth.getClient();if(window.supabase&&window.SOV_SUPABASE_URL&&window.SOV_SUPABASE_ANON_KEY){window.__sovLoggerSb=window.__sovLoggerSb||window.supabase.createClient(window.SOV_SUPABASE_URL,window.SOV_SUPABASE_ANON_KEY,{auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}});return window.__sovLoggerSb}}catch(e){}return null}
  function shouldIgnore(payload){
    if(IS_LOCAL||window.__SOV_AUDIT_MODE===true)return true;
    const msg=safeText(payload&&payload.message,2000).toLowerCase();
    const action=safeText(payload&&payload.action,200).toLowerCase();
    const details=payload&&payload.details||{};
    const url=safeText(details.url||details.url_path,600).toLowerCase();
    const stack=safeText(details.stack,2000).toLowerCase();
    if(stack.includes('127.0.0.1')||stack.includes('localhost'))return true;
    if(action==='fetch 401'||action==='fetch 404')return true;
    if(msg.includes('aborterror')||msg.includes('the user aborted')||msg.includes('signal is aborted'))return true;
    if((msg==='failed to fetch'||msg.includes('networkerror'))&&navigator.onLine===false)return true;
    if(url.includes('/rpc/sov_log_client_error'))return true;
    return false;
  }
  async function log(payload){
    try{
      payload=payload||{};
      if(shouldIgnore(payload))return null;
      const msg=safeText(payload.message||'Greška bez poruke',2000);
      const key=[payload.platform||'web',payload.screen||location.pathname,payload.action||'',payload.severity||'error',msg].join('|');
      const now=Date.now();
      if(lastSent[key]&&now-lastSent[key]<60000)return null;
      lastSent[key]=now;
      const sb=client();if(!sb||busy)return null;
      busy=true;
      const p=await profile();
      const args={
        p_platform:'web',
        p_app_version:payload.appVersion||VERSION,
        p_screen:safeText(payload.screen||location.pathname.split('/').pop()||'web',160),
        p_action:safeText(payload.action||'',180),
        p_severity:payload.severity||'error',
        p_message:msg,
        p_details:Object.assign({origin:location.origin},payload.details||{}),
        p_device_info:payload.deviceInfo||deviceInfo(),
        p_user_role:safeText(p&&p.role||'',40),
        p_trip_id:safeText(payload.tripId||'',120),
        p_team_id:safeText(payload.teamId||'',120),
        p_handled:payload.handled!==false
      };
      const res=await sb.rpc('sov_log_client_error',args);
      if(res&&res.error)console.warn('SOV error log nije spremljen:',res.error.message||res.error);
      return res&&res.data;
    }catch(e){try{console.warn('SOV client logger failed',e)}catch(_){}return null}
    finally{busy=false}
  }
  window.SOVClientLogger={
    log,
    info:(action,message,details)=>log({severity:'info',action,message,details}),
    warn:(action,message,details)=>log({severity:'warning',action,message,details}),
    error:(action,message,details)=>log({severity:'error',action,message,details}),
    fatal:(action,message,details)=>log({severity:'fatal',action,message,details,handled:false})
  };
  window.addEventListener('error',function(ev){
    log({severity:'error',handled:false,action:'window.error',message:ev.message||'JavaScript greška',details:{filename:ev.filename||'',lineno:ev.lineno||0,colno:ev.colno||0,stack:ev.error&&ev.error.stack||''}})
  });
  window.addEventListener('unhandledrejection',function(ev){
    const r=ev.reason||{};
    log({severity:'error',handled:false,action:'unhandledrejection',message:safeText(r.message||(r.toString&&r.toString())||'Neobrađena greška',2000),details:{stack:r.stack||'',reason:safeText(r,500)}})
  });
  try{
    const originalFetch=window.fetch&&window.fetch.bind(window);
    if(originalFetch&&!window.__sovFetchLoggerInstalled){
      window.__sovFetchLoggerInstalled=true;
      window.fetch=function(input,init){
        return originalFetch(input,init).then(function(res){
          try{
            const url=typeof input==='string'?input:(input&&input.url)||'';
            if(url&&url.includes('ncomefzkuixyfixisrhi.supabase.co')&&!res.ok&&!url.includes('/rpc/sov_log_client_error')){
              if(res.status===401||res.status===404)return res;
              log({severity:res.status>=500?'error':'warning',handled:true,action:'fetch '+res.status,message:'Poziv prema bazi nije uspio ('+res.status+').',details:{url:url.slice(0,500),status:res.status,statusText:res.statusText||''}})
            }
          }catch(e){}
          return res
        }).catch(function(err){
          try{if(!(err&&err.name==='AbortError'))log({severity:'error',handled:true,action:'fetch exception',message:err&&err.message||'Mrežni poziv nije uspio',details:{stack:err&&err.stack||''}})}catch(e){}
          throw err
        })
      }
    }
  }catch(e){}
})();
