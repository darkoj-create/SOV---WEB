
(function(){
  const BUCKET='sov-trip-assets';
  const state={tripId:null, assets:[], busy:false};
  const $=id=>document.getElementById(id);
  const clean=v=>String(v==null?'':v).trim();
  const esc=s=>clean(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const sb=()=>window.SOVAuth&&window.SOVAuth.getClient?window.SOVAuth.getClient():(window.SOVTripsCloud&&window.SOVTripsCloud.sb?window.SOVTripsCloud.sb():null);
  function toast(msg){ if(typeof window.toast==='function') window.toast(msg); else console.log(msg); }
  function safeFileName(name){return clean(name).normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-zA-Z0-9._-]+/g,'_').replace(/^_+|_+$/g,'').slice(0,120)||('file_'+Date.now());}
  function typeFromFile(file){const n=clean(file&&file.name).toLowerCase(); if(n.endsWith('.sovpkg')||n.endsWith('.zip'))return 'sovpkg'; if(n.endsWith('.gpx'))return 'gpx'; if(n.endsWith('.kml')||n.endsWith('.kmz'))return 'kml'; if(n.includes('topodroid'))return 'topodroid'; return 'other';}
  function sizeLabel(n){n=Number(n||0); if(!n)return '—'; if(n<1024*1024)return Math.max(1,Math.round(n/1024))+' KB'; return (n/1024/1024).toFixed(1)+' MB';}
  async function sha256(file){
    try{const buf=await file.arrayBuffer(); const hash=await crypto.subtle.digest('SHA-256',buf); return Array.from(new Uint8Array(hash)).map(b=>b.toString(16).padStart(2,'0')).join('');}catch(e){return ''}
  }
  async function signedUrl(path){const c=sb(); const res=await c.storage.from(BUCKET).createSignedUrl(path,60*60); if(res.error)throw res.error; return res.data.signedUrl;}
  async function listAssets(tripId){
    const c=sb(); if(!c)throw new Error('Supabase nije konfiguriran.');
    const rpc=await c.rpc('sov_trip_assets_for_trip',{p_trip_id:tripId});
    if(rpc.error) throw rpc.error;
    return Array.isArray(rpc.data)?rpc.data:[];
  }
  async function uploadAsset(){
    const tripId=state.tripId; const fileEl=$('tripAssetFile'); const file=fileEl&&fileEl.files&&fileEl.files[0];
    if(!tripId){toast('Odaberi izlet.');return} if(!file){toast('Odaberi paket ili GPX/KML datoteku.');return}
    const c=sb(); if(!c)throw new Error('Supabase nije konfiguriran.');
    setBusy(true,'Uploada se paket za teren…');
    try{
      const assetType=$('tripAssetType').value||typeFromFile(file);
      const title=clean($('tripAssetTitle').value)||file.name.replace(/\.[^.]+$/,'')||'Paket izleta';
      const description=clean($('tripAssetDescription').value);
      const checksum=await sha256(file);
      const path=[tripId,Date.now()+'_'+safeFileName(file.name)].join('/');
      const up=await c.storage.from(BUCKET).upload(path,file,{upsert:true,contentType:file.type||'application/octet-stream'});
      if(up.error)throw up.error;
      const payload={p_trip_id:tripId,p_asset_type:assetType,p_title:title,p_description:description,p_storage_path:path,p_original_filename:file.name,p_content_type:file.type||'application/octet-stream',p_size_bytes:file.size||0,p_checksum_sha256:checksum,p_metadata:{source:'web_v5_59_6_trip_assets_manager',checksum_sha256:checksum,offlineReady:true}};
      let reg=await c.rpc('sov_trip_asset_register_v2',payload);
      if(reg.error){
        reg=await c.rpc('sov_trip_asset_register',{p_trip_id:payload.p_trip_id,p_asset_type:payload.p_asset_type,p_title:payload.p_title,p_description:payload.p_description,p_storage_path:payload.p_storage_path,p_original_filename:payload.p_original_filename,p_content_type:payload.p_content_type,p_size_bytes:payload.p_size_bytes,p_metadata:payload.p_metadata});
      }
      if(reg.error)throw reg.error;
      fileEl.value=''; $('tripAssetTitle').value=''; $('tripAssetDescription').value='';
      toast('Paket za teren objavljen.'); await refresh();
    } finally { setBusy(false); }
  }
  async function downloadAsset(path,name){try{const url=await signedUrl(path); const a=document.createElement('a'); a.href=url; a.download=safeFileName(name||'sov-trip-asset'); document.body.appendChild(a); a.click(); a.remove();}catch(e){toast('Download nije uspio: '+(e.message||e));}}
  function render(){
    const host=$('tripAssetsPanel'); if(!host)return;
    const assets=state.assets||[];
    host.innerHTML=`<div class="trip-assets-card"><div class="trip-assets-head"><div><div class="eyebrow">Priprema za teren</div><h3>Paketi za teren</h3><p>Offline karta, GPX/KML i .sovpkg paket za članove izleta. Članovi ga skinu jednom; ako je checksum isti, app ga ne preuzima ponovno.</p></div><button class="btn small" id="tripAssetsRefresh">Osvježi</button></div><div class="trip-assets-grid"><div class="trip-assets-form"><label>Datoteka<input class="input" id="tripAssetFile" type="file" accept=".sovpkg,.zip,.gpx,.kml,.kmz,.json,.geojson" /></label><label>Tip<select class="select" id="tripAssetType"><option value="sovpkg">SOV paket / offline karta</option><option value="gpx">GPX ruta</option><option value="kml">KML točke/objekti</option><option value="topodroid">TopoDroid</option><option value="other">Ostalo</option></select></label><label>Naziv<input class="input" id="tripAssetTitle" placeholder="npr. Krasno teren — offline paket" /></label><label>Napomena<textarea id="tripAssetDescription" placeholder="Što je unutra i kada skinuti…"></textarea></label><button class="btn primary" id="tripAssetUpload">Objavi paket za izlet</button><div class="trip-assets-note">Prije odlaska na teren članovi trebaju otvoriti izlet u APK-u i skinuti paket. Offline radi samo ako je paket već preuzet.</div></div><div><div class="trip-assets-status" id="tripAssetsStatus">${state.busy?'Radim…':assets.length?assets.length+' paket(a) dostupno':'Nema objavljenih paketa.'}</div><div class="trip-assets-list">${assets.length?assets.map(a=>row(a)).join(''):'<div class="trip-assets-empty">Još nema paketa za ovaj izlet.</div>'}</div><div class="trip-assets-note trip-assets-ready">Status logika: app uspoređuje lokalni file/veličinu/checksum. Ako je isti paket već skinut, ne skida ga ponovno; ako je offline i paket postoji lokalno, otvara ga iz cachea.</div></div></div></div>`;
    $('tripAssetsRefresh').onclick=()=>refresh(); $('tripAssetUpload').onclick=()=>uploadAsset().catch(e=>{console.error(e);toast('Upload nije uspio: '+(e.message||e));setBusy(false);});
    host.querySelectorAll('[data-download-asset]').forEach(btn=>btn.onclick=()=>downloadAsset(btn.dataset.path,btn.dataset.name));
  }
  function row(a){const title=esc(a.title||a.original_filename||'Paket izleta'); const meta=[(a.asset_type||'sovpkg').toUpperCase(),sizeLabel(a.size_bytes),a.checksum_sha256?'checksum OK':''].filter(Boolean).join(' · '); return `<div class="trip-asset-row"><div class="trip-asset-row-top"><div><div class="trip-asset-title">${title}</div><div class="trip-asset-meta"><span>${esc(meta)}</span>${a.expires_at?`<span>vrijedi do ${esc(String(a.expires_at).slice(0,10))}</span>`:''}</div></div></div>${a.description?`<div class="muted">${esc(a.description)}</div>`:''}<div class="trip-asset-actions"><button class="btn small blue" data-download-asset="1" data-path="${esc(a.storage_path)}" data-name="${esc(a.original_filename||a.title||'sov-trip-asset')}">Download</button><span class="chip ok">Offline paket</span></div></div>`;}
  function setBusy(on,msg){state.busy=!!on; const s=$('tripAssetsStatus'); if(s)s.textContent=msg||''; render();}
  async function refresh(){
    const trip=(typeof selectedTrip!=='undefined')?selectedTrip:null; const id=trip&&trip.id; state.tripId=id||null;
    if(!id){state.assets=[]; render(); return}
    try{setBusy(true,'Učitavam pakete…'); state.assets=await listAssets(id); setBusy(false); render();}catch(e){console.warn(e); state.assets=[]; setBusy(false); const host=$('tripAssetsPanel'); if(host)host.innerHTML=`<div class="trip-assets-card"><b>Paketi za teren</b><p class="muted">Nije moguće učitati pakete: ${esc(e.message||e)}</p></div>`;}
  }

  // v6.1.45av: a real hard refresh must not reuse the normal in-flight request.
  function normalizeTripRows(data){
    if(!data)return [];
    if(Array.isArray(data))return data;
    if(typeof data==='string'){try{const parsed=JSON.parse(data);return Array.isArray(parsed)?parsed:[]}catch(e){return []}}
    if(Array.isArray(data.rows))return data.rows;
    if(Array.isArray(data.data))return data.data;
    return [];
  }
  function timed(promise,ms,label){let timer;const timeout=new Promise((_,reject)=>{timer=setTimeout(()=>reject(new Error((label||'Poziv')+' nije odgovorio na vrijeme.')),ms)});return Promise.race([promise,timeout]).finally(()=>clearTimeout(timer));}
  let forcedTripsInFlight=null;
  async function fetchFreshTrips(){
    const api=window.SOVTripsCloud; const c=api&&api.sb&&api.sb();
    if(!api||!c)throw new Error('Supabase nije konfiguriran.');
    if(window.SOVAuth&&window.SOVAuth.requireApproved)await timed(window.SOVAuth.requireApproved(),8000,'Provjera prijave');
    let lastError=null;
    try{
      const rpc=await timed(c.rpc('sov_list_trips_feed'),12000,'Baza izleta');
      if(!rpc.error){const rows=normalizeTripRows(rpc.data);api.saveCache(rows);return rows;}
      lastError=rpc.error;
    }catch(e){lastError=e;}
    try{
      const res=await timed(c.from('sov_trips').select('id,start_date,end_date,title,leader_name,leader_user_id,location_name,objective,description,status,visibility,trip_category,min_lat,max_lat,min_lon,max_lon,center_lat,center_lon,created_by,updated_by,created_at,updated_at,source,legacy_external_id,meta').neq('status','archived').order('start_date',{ascending:true}).limit(1500),12000,'Direktno čitanje izleta');
      if(!res.error){const rows=res.data||[];api.saveCache(rows);return rows;}
      lastError=res.error;
    }catch(e){lastError=e;}
    throw lastError||new Error('Izleti nisu dostupni.');
  }
  function installTripsRefreshFix(){
    const api=window.SOVTripsCloud;
    if(!api||typeof api.listTrips!=='function'||api.listTrips.__sovHardRefresh)return;
    const original=api.listTrips.bind(api);
    const wrapped=async function(options={}){
      const force=!!(options&&options.force)||!!api.__forceNextList;
      api.__forceNextList=false;
      if(!force)return original();
      if(forcedTripsInFlight)return forcedTripsInFlight;
      api.__lastForceError=null;
      forcedTripsInFlight=fetchFreshTrips().catch(error=>{api.__lastForceError=error;throw error;}).finally(()=>{forcedTripsInFlight=null;});
      return forcedTripsInFlight;
    };
    wrapped.__sovHardRefresh=true;
    api.listTrips=wrapped;
  }
  async function requestTripsRefresh(source='button'){
    installTripsRefreshFix();
    const api=window.SOVTripsCloud;
    const button=$('refreshBtn');
    if(!api||typeof window.loadTrips!=='function')return;
    if(button){button.disabled=true;button.setAttribute('aria-busy','true');}
    api.__forceNextList=true;
    try{
      await window.loadTrips({force:true});
      if(api.__lastForceError)throw api.__lastForceError;
      toast(source==='pull'?'Izleti osvježeni povlačenjem.':'Izleti osvježeni.');
    }catch(error){
      console.warn('[SOV trips] hard refresh failed',error);
      toast('Osvježavanje izleta nije uspjelo.');
    }finally{
      if(button){button.disabled=false;button.removeAttribute('aria-busy');}
    }
  }
  function installPullToRefresh(){
    if(!('ontouchstart' in window)||document.getElementById('sovTripsPullIndicator'))return;
    const indicator=document.createElement('div');
    indicator.id='sovTripsPullIndicator';
    indicator.setAttribute('aria-live','polite');
    indicator.style.cssText='position:fixed;z-index:80;left:50%;top:10px;transform:translate(-50%,-80px);padding:9px 14px;border:1px solid rgba(215,246,111,.32);border-radius:999px;background:rgba(5,12,14,.94);color:#eef8f5;font:800 13px/1 system-ui;box-shadow:0 12px 36px rgba(0,0,0,.35);transition:transform .18s ease;pointer-events:none';
    indicator.textContent='Povuci za osvježavanje';
    document.body.appendChild(indicator);
    let startY=0,pulling=false,armed=false,busy=false;
    const blockedTarget=target=>target&&target.closest&&target.closest('input,textarea,select,button,a,.modal,.modalCard,.tripForm');
    document.addEventListener('touchstart',event=>{
      if(busy||window.scrollY>1||event.touches.length!==1||blockedTarget(event.target))return;
      startY=event.touches[0].clientY;pulling=true;armed=false;
    },{passive:true});
    document.addEventListener('touchmove',event=>{
      if(!pulling||event.touches.length!==1)return;
      const dy=event.touches[0].clientY-startY;
      if(dy<=0){pulling=false;indicator.style.transform='translate(-50%,-80px)';return;}
      if(window.scrollY>1){pulling=false;indicator.style.transform='translate(-50%,-80px)';return;}
      const visible=Math.min(72,Math.max(0,dy*.55));
      indicator.style.transform=`translate(-50%,${visible-58}px)`;
      armed=dy>=82;
      indicator.textContent=armed?'Pusti za osvježavanje':'Povuci za osvježavanje';
      if(dy>12)event.preventDefault();
    },{passive:false});
    document.addEventListener('touchend',async()=>{
      if(!pulling)return;
      pulling=false;
      if(!armed){indicator.style.transform='translate(-50%,-80px)';return;}
      busy=true;indicator.textContent='Osvježavam izlete…';indicator.style.transform='translate(-50%,0)';
      try{await requestTripsRefresh('pull');}finally{setTimeout(()=>{indicator.style.transform='translate(-50%,-80px)';indicator.textContent='Povuci za osvježavanje';busy=false;armed=false;},500);}
    },{passive:true});
    document.addEventListener('touchcancel',()=>{pulling=false;armed=false;indicator.style.transform='translate(-50%,-80px)';},{passive:true});
  }
  function install(){
    const original=window.renderDetail;
    if(typeof original==='function'&&!original.__tripAssetsWrapped){window.renderDetail=function(t){original(t); setTimeout(refresh,0);}; window.renderDetail.__tripAssetsWrapped=true;}
    setInterval(()=>{const trip=(typeof selectedTrip!=='undefined')?selectedTrip:null; if(trip&&trip.id!==state.tripId) refresh();},1200);
    installTripsRefreshFix();
    const refreshButton=$('refreshBtn');
    if(refreshButton)refreshButton.onclick=()=>requestTripsRefresh('button');
    installPullToRefresh();
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install); else install();
  window.SOVTripAssetsManager={refresh,requestTripsRefresh};
})();
