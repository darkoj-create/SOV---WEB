(function(){
  const CACHE_KEY='sov_trips_cloud_cache_v5_56';
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
    const iso=isoDate(v); const [y,m,d]=iso.split('-'); return `${d}/${m}/${y}`;
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
      visibility: row.visibility || 'club',
      trip_category: row.trip_category || meta.trip_category || meta.category || row.category || row.tripType || row.event_type || row.objective || 'Izlet'
    };
  }
  function loadCache(){
    try{return JSON.parse(localStorage.getItem(CACHE_KEY)||'[]')}catch(e){return []}
  }
  function saveCache(rows){try{localStorage.setItem(CACHE_KEY,JSON.stringify(rows||[]))}catch(e){}}
  function normalizeRpcRows(data){
    if(!data) return [];
    if(Array.isArray(data)) return data;
    if(typeof data === 'string'){
      try{const parsed=JSON.parse(data); return Array.isArray(parsed)?parsed:[];}catch(e){return []}
    }
    if(Array.isArray(data.rows)) return data.rows;
    if(Array.isArray(data.data)) return data.data;
    if(Array.isArray(data.sov_list_trips_feed)) return data.sov_list_trips_feed;
    return [];
  }
  function withTimeout(promise, ms, label){
    let timer;
    const timeout=new Promise((_,reject)=>{ timer=setTimeout(()=>reject(new Error((label||'Poziv')+' nije odgovorio na vrijeme.')), ms); });
    return Promise.race([promise, timeout]).finally(()=>clearTimeout(timer));
  }
  async function listTrips(){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    if(window.SOVAuth && window.SOVAuth.requireApproved){
      await withTimeout(window.SOVAuth.requireApproved(), 8000, 'Provjera prijave');
    }

    // v6.1.23: never let the dashboard stay stuck on loading.
    // Try DB-owned feed first, but with timeout and tolerant JSON parsing.
    let lastError=null;
    try{
      const rpc=await withTimeout(c.rpc('sov_list_trips_feed'), 12000, 'Baza izleta');
      if(!rpc.error){
        const rows=normalizeRpcRows(rpc.data);
        saveCache(rows);
        return rows;
      }
      lastError=rpc.error;
      console.warn('[SOV trips] RPC feed failed, falling back', rpc.error);
    }catch(e){
      lastError=e;
      console.warn('[SOV trips] RPC feed timed out/failed, falling back', e);
    }

    // Direct table fallback: fewer columns first. This survives broken views/RPCs.
    try{
      const res=await withTimeout(
        c.from('sov_trips')
          .select('id,start_date,end_date,title,leader_name,leader_user_id,location_name,objective,description,status,visibility,trip_category,min_lat,max_lat,min_lon,max_lon,center_lat,center_lon,created_by,updated_by,created_at,updated_at,source,legacy_external_id,meta')
          .neq('status','archived')
          .order('start_date',{ascending:true})
          .limit(1500),
        12000,
        'Direktno čitanje izleta'
      );
      if(!res.error){ saveCache(res.data||[]); return res.data||[]; }
      lastError=res.error;
      console.warn('[SOV trips] direct table failed', res.error);
    }catch(e){ lastError=e; console.warn('[SOV trips] direct table timed out/failed', e); }

    // Old view fallback last.
    try{
      const res=await withTimeout(c.from('sov_trips_mobile_feed').select('*').order('start_date',{ascending:true}).limit(1500), 12000, 'Feed izleta');
      if(!res.error){ saveCache(res.data||[]); return res.data||[]; }
      lastError=res.error;
      console.warn('[SOV trips] mobile feed failed', res.error);
    }catch(e){ lastError=e; console.warn('[SOV trips] mobile feed timed out/failed', e); }

    throw lastError || new Error('Izleti nisu dostupni.');
  }
  function payloadFromTripForm(payload, extra={}){
    const meta={source:'sov_web_v5_56_trip_signup_transport', legacyPayload:payload||{}};
    if(payload.weatherCity) meta.weatherCity=payload.weatherCity;
    if(payload.rasporedUrl) meta.rasporedUrl=payload.rasporedUrl;
    const tripCategory = clean(payload.trip_category || payload.category || payload.event_type || payload.type || payload.goal || 'Izlet');
    meta.trip_category = tripCategory;
    const start=isoDate(payload.date || payload.from || payload.start_date);
    let end=isoDate(payload.endDate || payload.to || payload.end_date || start);
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
      trip_category: tripCategory,
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
  function dbErrorText(error){
    return String((error && (error.message || error.details || error.hint || error.code)) || '').toLowerCase();
  }
  function isColumnError(error){
    const t=dbErrorText(error);
    return t.includes('column') || t.includes('schema cache') || t.includes('pgrst204') || t.includes('42703');
  }
  function missingColumnName(error){
    const raw=String((error&&(error.message||error.details||error.hint||error.code))||'');
    let m=raw.match(/['"]([a-zA-Z_][a-zA-Z0-9_]*)['"] column/i); if(m) return m[1];
    m=raw.match(/column\s+[^.]+\.([a-zA-Z_][a-zA-Z0-9_]*)\s+does not exist/i); if(m) return m[1];
    m=raw.match(/column\s+['"]?([a-zA-Z_][a-zA-Z0-9_]*)['"]?\s+does not exist/i); if(m) return m[1];
    return '';
  }
  function legacySafeTripRow(row){
    const safe={...row};
    delete safe.trip_category;
    if(safe.meta) safe.meta={...safe.meta, trip_category: row.trip_category || safe.meta.trip_category || 'Izlet'};
    return safe;
  }
  function withoutNewColumns(row){
    const safe=legacySafeTripRow(row);
    delete safe.end_date;
    return safe;
  }
  function minimalTripRow(row){
    return {
      start_date: row.start_date,
      leader_name: row.leader_name,
      location_name: row.location_name,
      objective: row.objective,
      description: row.description,
      status: row.status || 'planned',
      visibility: row.visibility || 'club',
      meta: row.meta || {}
    };
  }
  async function insertTryingSchemas(c, row){
    const attempts=[row, legacySafeTripRow(row), withoutNewColumns(row), minimalTripRow(row)];
    let last=null;
    for(const attempt0 of attempts){
      let attempt={...attempt0};
      for(let i=0;i<5;i++){
        let res=await c.from('sov_trips').insert(attempt).select('*').maybeSingle();
        if(!res.error) return res.data || attempt;
        last=res.error;
        const miss=missingColumnName(res.error);
        if(miss && Object.prototype.hasOwnProperty.call(attempt, miss)){ delete attempt[miss]; continue; }
        res=await c.from('sov_trips').insert(attempt);
        if(!res.error) return attempt;
        last=res.error;
        const miss2=missingColumnName(res.error);
        if(miss2 && Object.prototype.hasOwnProperty.call(attempt, miss2)){ delete attempt[miss2]; continue; }
        break;
      }
    }
    throw last || new Error('Spremanje izleta nije uspjelo.');
  }
  async function updateTryingSchemas(c, id, row){
    const attempts=[row, legacySafeTripRow(row), withoutNewColumns(row), minimalTripRow(row)];
    let last=null;
    for(const attempt0 of attempts){
      let attempt={...attempt0};
      for(let i=0;i<5;i++){
        let res=await c.from('sov_trips').update(attempt).eq('id',id).select('*').maybeSingle();
        if(!res.error) return res.data || attempt;
        last=res.error;
        const miss=missingColumnName(res.error);
        if(miss && Object.prototype.hasOwnProperty.call(attempt, miss)){ delete attempt[miss]; continue; }
        res=await c.from('sov_trips').update(attempt).eq('id',id);
        if(!res.error) return attempt;
        last=res.error;
        const miss2=missingColumnName(res.error);
        if(miss2 && Object.prototype.hasOwnProperty.call(attempt, miss2)){ delete attempt[miss2]; continue; }
        break;
      }
    }
    throw last || new Error('Spremanje izleta nije uspjelo.');
  }
  async function createTripFromForm(payload, extra={}){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const row=scrub(payloadFromTripForm(payload, extra));
    if(!row.start_date || !row.leader_name || !row.location_name) throw new Error('Upiši barem datum, voditelja i lokaciju.');

    // v6.1.20: DB-owned save RPC first. It also repairs old RLS/select issues.
    const rpc=await c.rpc('sov_save_trip', {p_trip_id:null, p_payload:row});
    if(!rpc.error) return rpc.data || row;
    console.warn('[SOV trips] save RPC failed, falling back to direct insert', rpc.error);
    return await insertTryingSchemas(c, row);
  }
  async function updateTrip(id, patch){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const full={...patch, updated_at:new Date().toISOString()};

    const rpc=await c.rpc('sov_save_trip', {p_trip_id:id, p_payload:full});
    if(!rpc.error) return rpc.data || full;
    console.warn('[SOV trips] update RPC failed, falling back to direct update', rpc.error);
    return await updateTryingSchemas(c, id, full);
  }
  async function deleteTrip(id){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const tripId=clean(id);
    if(!tripId) throw new Error('Izlet nema ID.');

    // Preferred path: SECURITY DEFINER RPC that deletes the real DB row and
    // returns an explicit result. This avoids the silent Supabase/RLS case
    // where delete() returns no error but deletes 0 rows.
    const rpc=await c.rpc('sov_delete_trip_admin', {p_trip_id:tripId});
    if(!rpc.error){
      const result=rpc.data || {};
      if(result.deleted === false) throw new Error(result.message || 'Izlet nije obrisan.');
      saveCache(loadCache().filter(t=>String(t.id)!==tripId));
      return result;
    }

    // Backward-compatible fallback if SQL patch was not applied yet.
    const res=await c.from('sov_trips').delete().eq('id',tripId).select('id');
    if(res.error) throw res.error;
    if(!res.data || !res.data.length){
      throw new Error('Izlet nije obrisan. Provjeri ovlasti ili pokreni SQL patch za brisanje izleta.');
    }
    saveCache(loadCache().filter(t=>String(t.id)!==tripId));
    return {deleted:true, id:tripId, mode:'direct'};
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
    const payload={p_trip_id:tripId,p_file_type:type,p_file_name:file.name,p_storage_path:path,p_public_url:null,p_meta:{...meta,size:file.size,lastModified:file.lastModified,source:'web_v5_56_trip_signup_transport'}};
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

  function normalizeSignupPayload(tripId, p){
    const transportMode=clean(p.transport_mode || p.transportMode || (p.driving?'driver':'needs_ride')) || 'needs_ride';
    const seats=Math.max(0, Number(p.seats_available ?? p.freeSeats ?? p.seats ?? 0)||0);
    return {
      p_trip_id: tripId,
      p_attendance_status: clean(p.attendance_status || p.status || 'confirmed') || 'confirmed',
      p_transport_mode: transportMode,
      p_seats_available: transportMode==='driver' ? seats : 0,
      p_departure_place: clean(p.departure_place || p.departure || ''),
      p_note: clean(p.note || ''),
      p_member_name: clean(p.member_name || p.name || ''),
      p_member_email: clean(p.member_email || p.email || '')
    };
  }
  async function listTripMembers(tripId){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    if(!tripId) return [];
    let res=await c.from('sov_trip_members_transport_view').select('*').eq('trip_id',tripId).order('created_at',{ascending:true}).limit(300);
    if(res.error){
      res=await c.from('sov_trip_members').select('id,trip_id,user_id,member_name,member_email,role,attendance_status,meta,created_at,updated_at').eq('trip_id',tripId).order('created_at',{ascending:true}).limit(300);
      if(res.error) throw res.error;
      return (res.data||[]).map(row=>{const meta=row.meta||{}; return {...row, transport_mode:meta.transport_mode||'', has_car:!!meta.has_car, seats_available:Number(meta.seats_available||0)||0, departure_place:meta.departure_place||'', note:meta.note||''};});
    }
    return res.data||[];
  }
  async function saveTripSignup(tripId, payload){
    const c=sb(); if(!c) throw new Error('Supabase nije konfiguriran.');
    const rpcPayload=normalizeSignupPayload(tripId,payload||{});
    const rpc=await c.rpc('sov_trip_signup', rpcPayload);
    if(!rpc.error) return rpc.data;
    const row={trip_id:tripId, member_name:rpcPayload.p_member_name, member_email:rpcPayload.p_member_email, role:rpcPayload.p_transport_mode==='driver'?'driver':'participant', attendance_status:rpcPayload.p_attendance_status, meta:{transport_mode:rpcPayload.p_transport_mode, has_car:rpcPayload.p_transport_mode==='driver', seats_available:rpcPayload.p_seats_available, departure_place:rpcPayload.p_departure_place, note:rpcPayload.p_note, source:'web_v5_56_signup_fallback'}};
    const ins=await c.from('sov_trip_members').insert(row).select('*').single();
    if(ins.error) throw rpc.error || ins.error;
    return ins.data;
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
  window.SOVTripsCloud={sb,listTrips,loadCache,saveCache,mapToLegacy:toLegacy,createTripFromForm,updateTrip,deleteTrip,signupTrip,listTripMembers,saveTripSignup,manifest,isoDate,hrDate,uploadTripFile,uploadTripFiles,listTripFiles,fileTypeFromName,announcementText:null};
})();
