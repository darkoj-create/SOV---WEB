
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
  function install(){
    const original=window.renderDetail;
    if(typeof original==='function'&&!original.__tripAssetsWrapped){window.renderDetail=function(t){original(t); setTimeout(refresh,0);}; window.renderDetail.__tripAssetsWrapped=true;}
    setInterval(()=>{const trip=(typeof selectedTrip!=='undefined')?selectedTrip:null; if(trip&&trip.id!==state.tripId) refresh();},1200);
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install); else install();
  window.SOVTripAssetsManager={refresh};
})();
