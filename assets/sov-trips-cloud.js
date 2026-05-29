(function(){
  const CACHE_KEY='sov_trips_cloud_cache_v5_55';
  function sb(){
    if(window.SOVAuth && window.SOVAuth.getClient) return window.SOVAuth.getClient();
    if(window.supabase && window.SOV_SUPABASE_URL && window.SOV_SUPABASE_ANON_KEY){
      window.__sovTripsSb = window.__sovTripsSb || window.supabase.createClient(window.SOV_SUPABASE_URL, window.SOV_SUPABASE_ANON_KEY, {auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}});
      return window.__sovTripsSb;
    }
    return null;
  }
  function clean(v){return String(v==null?'':v).trim()}
  function isoDate(v){
    const s=clean(v); if(!s) return new Date().toISOString().slice(0,10);
    let m=s.match(/^(\d{4})-(\d{2})-(\d{2})/); if(m) return `${m[1]}-${m[2]}-${m[3]}`;
    m=s.match(/(\d{1,2})[.\/ -](\d{1,2})[.\/ -](\d{4})/); if(m) return `${m[3]}-${String(m[2]).padStart(2,'0')}-${String(m[1]).padStart(2,'0')}`;
    const d=new Date(s); return isNaN(d)?new Date().toISOString().slice(0,10):d.toISOString().slice(0,10);
  }
  function hrDate(v){
    const iso=isoDate(v); const [y,m,d]=iso.split('-'); return `${d}.${m}.${y}.`;
  }
  function toLegacy(row, i){
    const meta=row.meta || {};
    const start=row.start_date || row.startDate || new Date().toISOString().slice(0,10);
    const end=row.end_date || row.endDate || start;
    return {
      cloudId: row.id || '',
      rowNumber: i+2,
      date: end && end!==start ? `${hrDate(start)} - ${hrDate(end)}` : hrDate(start),
      leader: row.leader_name || row.leaderName || '',
      location: row.location_name || row.locationName || row.title || '',
      description: row.description || '',
      goal: row.objective || row.goal || '',
      participants: row.participants || (row.member_count ? `${row.member_count} prijavljenih` : ''),
      drivers: meta.drivers || meta.vozaci || '',
      rasporedUrl: meta.rasporedUrl || meta.raspored_url || '',
      weatherCity: meta.weatherCity || meta.weather_city || row.location_name || '',
      centerLat: row.center_lat ?? meta.centerLat ?? null,
      centerLon: row.center_lon ?? meta.centerLon ?? null,
      minLat: row.min_lat ?? meta.minLat ?? null,
      maxLat: row.max_lat ?? meta.maxLat ?? null,
      minLon: row.min_lon ?? meta.minLon ?? null,
      maxLon: row.max_lon ?? meta.maxLon ?? null,
      status: row.status || 'planned',
      visibility: row.visibility || 'club'
    };
  }
  function loadCache(){
    try{return JSON.parse(localStorage.getItem(CACHE_KEY)||'[]')}catch(e){return []}
  }
  function saveCache(rows){try{localStorage.setItem(CACHE_KEY,JSON.stringify(rows||[]))}catch(e){}}
  async function listTrips(){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    if(window.SOVAuth && window.SOVAuth.requireApproved) await window.SOVAuth.requireApproved();
    const {data,error}=await c.from('sov_trips_mobile_feed').select('*').order('start_date',{ascending:true}).limit(1500);
    if(error) throw error;
    saveCache(data||[]);
    return data||[];
  }
  function payloadFromTripForm(payload, extra={}){
    const meta={source:'sov_web_v5_55_premium_trip_ui', legacyPayload:payload||{}};
    if(payload.weatherCity) meta.weatherCity=payload.weatherCity;
    if(payload.rasporedUrl) meta.rasporedUrl=payload.rasporedUrl;
    const start=isoDate(payload.date || payload.from || payload.start_date);
    let end=start;
    const range=clean(payload.date).split(/\s+-\s+/);
    if(range.length>1) end=isoDate(range[1]);
    return {
      start_date:start,
      end_date:end,
      leader_name:clean(payload.leader),
      location_name:clean(payload.location),
      objective:clean(payload.goal),
      description:clean(payload.description),
      status: clean(payload.status) || 'planned',
      visibility: clean(payload.visibility) || 'club',
      min_lat: null,
      max_lat: null,
      min_lon: null,
      max_lon: null,
      center_lat: null,
      center_lon: null,
      source:'web',
      legacy_external_id: payload.packageId || ('web_'+Date.now()),
      meta
    };
  }
  function scrub(obj){
    const out={}; Object.entries(obj).forEach(([k,v])=>{ if(Number.isNaN(v)) v=null; out[k]=v; }); return out;
  }
  async function createTripFromForm(payload, extra={}){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const row=scrub(payloadFromTripForm(payload, extra));
    if(!row.start_date || !row.leader_name || !row.location_name) throw new Error('Upiši barem datum, voditelja i lokaciju.');
    const {data,error}=await c.from('sov_trips').insert(row).select('*').single();
    if(error) throw error;
    return data;
  }
  async function updateTrip(id, patch){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const {data,error}=await c.from('sov_trips').update({...patch, updated_at:new Date().toISOString()}).eq('id',id).select('*').single();
    if(error) throw error; return data;
  }
  async function deleteTrip(id){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const {error}=await c.from('sov_trips').delete().eq('id',id); if(error) throw error; return true;
  }

  function fileTypeFromName(name){
    const n=clean(name).toLowerCase();
    if(n.endsWith('.gpx')) return 'gpx';
    if(n.endsWith('.kml')) return 'kml';
    if(n.endsWith('.kmz')) return 'kmz';
    if(n.endsWith('.geojson') || n.endsWith('.json')) return 'geojson';
    if(n.endsWith('.zip')) return 'zip';
    if(/\.(jpg|jpeg|png|webp)$/i.test(n)) return 'photo';
    if(n.endsWith('.pdf')) return 'pdf';
    return 'other';
  }
  function safeFileName(name){
    return clean(name).normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-zA-Z0-9._-]+/g,'_').replace(/^_+|_+$/g,'').slice(0,120) || ('file_'+Date.now());
  }
  async function uploadTripFile(tripId, file, meta={}){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    if(!tripId) throw new Error('Izlet nema ID. Spremi izlet prije dodavanja filea.');
    const type=fileTypeFromName(file.name);
    const path=`${tripId}/${Date.now()}_${safeFileName(file.name)}`;
    const bucket='sov-trip-files';
    const up=await c.storage.from(bucket).upload(path, file, {upsert:true, contentType:file.type || 'application/octet-stream'});
    if(up.error) throw up.error;
    const payload={p_trip_id:tripId,p_file_type:type,p_file_name:file.name,p_storage_path:path,p_public_url:null,p_meta:{...meta,size:file.size,lastModified:file.lastModified,source:'web_v5_55_premium_trip_ui'}};
    const rpc=await c.rpc('sov_add_trip_file', payload);
    if(rpc.error){
      const row={trip_id:tripId,file_type:type,file_name:file.name,storage_bucket:bucket,storage_path:path,mime_type:file.type||'',size_bytes:file.size||null,meta:payload.p_meta};
      const ins=await c.from('sov_trip_files').insert(row).select('*').single();
      if(ins.error) throw ins.error;
      return ins.data;
    }
    return rpc.data;
  }
  async function uploadTripFiles(tripId, files, meta={}){
    const out=[];
    for(const file of Array.from(files||[])) out.push(await uploadTripFile(tripId,file,meta));
    return out;
  }
  async function listTripFiles(tripId){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const {data,error}=await c.from('sov_trip_file_list').select('*').eq('trip_id',tripId).order('created_at',{ascending:false}).limit(200);
    if(error) throw error;
    return data||[];
  }

  async function signupTrip(trip, member){
    const id=trip.cloudId || trip.id; if(!id) throw new Error('Ovaj izlet nema cloud ID. Osvježi raspored.');
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const row={trip_id:id, member_name:member.name, member_email:member.email||'', role:member.driving?'driver':'participant', attendance_status:'confirmed', meta:{phone:member.phone||'', driving:!!member.driving, source:'web_v5_49'}};
    const {error}=await c.from('sov_trip_members').insert(row); if(error) throw error; return true;
  }
  async function manifest(){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const {data,error}=await c.from('sov_trips_sync_manifest').select('*').maybeSingle(); if(error) throw error; return data;
  }
  window.SOVTripsCloud={sb,listTrips,loadCache,saveCache,mapToLegacy:toLegacy,createTripFromForm,updateTrip,deleteTrip,signupTrip,manifest,isoDate,hrDate,uploadTripFile,uploadTripFiles,listTripFiles,fileTypeFromName,announcementText:null};
})();
