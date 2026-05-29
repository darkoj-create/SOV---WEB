(function(){
  const CACHE_KEY='sov_trips_cloud_cache_v5_49';
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
    const meta={source:'sov_web_v5_49', legacyPayload:payload||{}};
    if(payload.weatherCity) meta.weatherCity=payload.weatherCity;
    if(payload.rasporedUrl) meta.rasporedUrl=payload.rasporedUrl;
    if(extra.files) meta.localFiles=(extra.files||[]).map(f=>({name:f.name,size:f.size,type:f.type||''}));
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
      status:'planned',
      visibility:'club',
      min_lat: payload.minLat===''?null:Number(payload.minLat ?? extra.area?.south ?? NaN),
      max_lat: payload.maxLat===''?null:Number(payload.maxLat ?? extra.area?.north ?? NaN),
      min_lon: payload.minLon===''?null:Number(payload.minLon ?? extra.area?.west ?? NaN),
      max_lon: payload.maxLon===''?null:Number(payload.maxLon ?? extra.area?.east ?? NaN),
      center_lat: payload.centerLat===''?null:Number(payload.centerLat ?? NaN),
      center_lon: payload.centerLon===''?null:Number(payload.centerLon ?? NaN),
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
  window.SOVTripsCloud={sb,listTrips,loadCache,saveCache,mapToLegacy:toLegacy,createTripFromForm,updateTrip,deleteTrip,signupTrip,manifest,isoDate,hrDate};
})();
