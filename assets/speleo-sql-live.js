(function(){
  const LIVE_TABLE='speleo_objects_live_sql';
  const STAGING_TABLE='speleo_objects_staging';
  const SELECT_COLS='source_id,source_system,name,lat,lon,cadastre_status,cadastral_number,record_status,object_type_final,county,municipality,nearest_place,locality,depth_m,length_m,field_tasks,workflow_raw,edit_note,raw,updated_at,promoted_from_staging_at,promotion_batch_id';
  function sb(){return window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();}
  function toObj(r){
    const raw=(r&&r.raw&&typeof r.raw==='object')?r.raw:{};
    const idNum=Number(r.source_id);
    const out=Object.assign({}, raw, {
      id:Number.isFinite(idNum)?idNum:r.source_id,
      source_id:String(r.source_id||raw.id||''),
      sql_source_system:r.source_system||'sql',
      sql_updated_at:r.updated_at||r.promoted_from_staging_at||null,
      name:r.name||raw.name,
      lat:r.lat ?? raw.lat,
      lon:r.lon ?? raw.lon,
      cadastre_status:r.cadastre_status ?? raw.cadastre_status,
      cadastral_number:r.cadastral_number ?? raw.cadastral_number,
      record_status:r.record_status ?? raw.record_status,
      object_type_final:r.object_type_final ?? raw.object_type_final,
      county:r.county ?? raw.county,
      municipality:r.municipality ?? raw.municipality,
      nearest_place:r.nearest_place ?? raw.nearest_place,
      locality:r.locality ?? raw.locality,
      depth_m:r.depth_m ?? raw.depth_m,
      length_m:r.length_m ?? raw.length_m,
      field_tasks:r.field_tasks ?? raw.field_tasks,
      workflow_raw:r.workflow_raw ?? raw.workflow_raw,
      edit_note:r.edit_note ?? raw.edit_note
    });
    return out;
  }
  async function readAll(table,limit){
    const client=sb(); if(!client) throw new Error('Supabase client nije dostupan');
    let out=[],from=0,step=1000,max=limit||12000;
    while(out.length<max){
      const to=Math.min(from+step-1,max-1);
      const {data,error}=await client.from(table).select(SELECT_COLS).range(from,to).order('name',{ascending:true});
      if(error) throw error;
      out=out.concat(data||[]);
      if(!data||data.length<step) break;
      from+=step;
    }
    return out;
  }
  async function loadAppObjects(fallbackUrl){
    try{
      let rows=await readAll(LIVE_TABLE,15000);
      let source='SQL live';
      if(!rows.length){ rows=await readAll(STAGING_TABLE,15000); source='SQL staging fallback'; }
      if(rows.length){
        const objects=rows.map(toObj).filter(o=>o&&o.name&&Number.isFinite(Number(o.lat))&&Number.isFinite(Number(o.lon)));
        window.SOV_SPELEO_DATA_SOURCE=source;
        console.info('[SOV Speleo] loaded '+objects.length+' from '+source);
        return objects;
      }
      throw new Error('SQL tablice su prazne');
    }catch(e){
      console.warn('[SOV Speleo] SQL load failed, using JSON fallback:', e);
      const r=await fetch(fallbackUrl||'data/sov-baza.json',{cache:'no-store'});
      const data=await r.json();
      window.SOV_SPELEO_DATA_SOURCE='JSON fallback';
      return data;
    }
  }
  async function updateLiveObject(id,patch,before){
    const client=sb(); if(!client) throw new Error('Supabase client nije dostupan');
    const sourceId=String(id);
    const current=before||{};
    const raw=Object.assign({}, current, patch, {id: current.id ?? (Number.isFinite(Number(sourceId))?Number(sourceId):sourceId)});
    const payload={
      source_id:sourceId,
      source_system:'sql_live_edit',
      name:patch.name ?? current.name ?? ('Objekt '+sourceId),
      lat:patch.lat ?? current.lat ?? null,
      lon:patch.lon ?? current.lon ?? null,
      cadastre_status:patch.cadastre_status ?? current.cadastre_status ?? null,
      cadastral_number:patch.cadastral_number ?? current.cadastral_number ?? null,
      record_status:patch.record_status ?? current.record_status ?? null,
      object_type_final:patch.object_type_final ?? current.object_type_final ?? null,
      county:patch.county ?? current.county ?? null,
      municipality:patch.municipality ?? current.municipality ?? null,
      nearest_place:patch.nearest_place ?? current.nearest_place ?? null,
      locality:patch.locality ?? current.locality ?? null,
      depth_m:patch.depth_m ?? current.depth_m ?? null,
      length_m:patch.length_m ?? current.length_m ?? null,
      field_tasks:patch.field_tasks ?? current.field_tasks ?? null,
      workflow_raw:patch.workflow_raw ?? current.workflow_raw ?? null,
      edit_note:patch.note ?? current.note ?? patch.edit_note ?? null,
      raw,
      promoted_by:(window.SOVAuth&&SOVAuth.currentUser&&SOVAuth.currentUser.email)||'web-open-preview'
    };
    const {data,error}=await client.from(LIVE_TABLE).upsert(payload,{onConflict:'source_id'}).select('*').single();
    if(error) throw error;
    try{await client.from('speleo_sql_promotion_audit').insert({source_id:sourceId,action:'manual_note',actor:payload.promoted_by,before_live:before||null,after_live:payload,staging_snapshot:null});}catch(_e){}
    return toObj(data);
  }
  window.SOVSpeleoSQL={loadAppObjects,updateLiveObject,toObj,readAll};
})();
