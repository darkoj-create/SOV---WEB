(function(){
  const MAP_PAGE_RPC='sov_map_objects_page';
  const MAP_RPC='sov_map_objects';
  const ARHIVAR_VIEW='sov_arhivar_worklist';
  const STAGING_TABLE='speleo_objects_staging';
  const LIVE_TABLE='speleo_objects_live_sql';
  const PAGE_SIZE=1000;
  const MAX_ROWS=100000;
  function sb(){return window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();}
  function normKey(s){return String(s||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9]+/g,'_').replace(/^_|_$/g,'');}
  function asObj(v){return v&&typeof v==='object'&&!Array.isArray(v)?v:{};}
  function allSources(row){const raw=asObj(row.raw); const meta=asObj(row.metadata); return [row, raw, meta];}
  function pick(row, keys){
    const sources=allSources(row); const wanted=keys.map(normKey);
    for(const src of sources){
      const by={}; Object.keys(src||{}).forEach(k=>by[normKey(k)]=src[k]);
      for(const k of wanted){ if(by[k]!==undefined && by[k]!==null && String(by[k]).trim()!=='') return by[k]; }
    }
    return '';
  }
  function num(v){
    if(v===null||v===undefined||v==='') return NaN;
    if(typeof v==='number') return Number.isFinite(v)?v:NaN;
    let s=String(v).trim().replace(',', '.');
    const m=s.match(/-?\d+(?:\.\d+)?/); if(!m)return NaN;
    const n=Number(m[0]); return Number.isFinite(n)?n:NaN;
  }
  function bool(v){
    if(v===true||v===false)return v;
    const s=String(v??'').trim().toLowerCase();
    if(['true','t','1','da','yes','y'].includes(s))return true;
    if(['false','f','0','ne','no','n'].includes(s))return false;
    return undefined;
  }
  function str(v){return v===null||v===undefined?'':String(v).trim();}
  function arr(v){if(Array.isArray(v))return v.map(str).filter(Boolean); if(typeof v==='string'&&v.trim()){return v.split(/[,;]+/).map(str).filter(Boolean);} return [];}
  function cryptoId(row){const s=JSON.stringify(row||{}); let h=0; for(let i=0;i<s.length;i++){h=((h<<5)-h+s.charCodeAt(i))|0;} return 'row_'+Math.abs(h);}
  function normalize(row){
    row = row && row.data ? row.data : row;
    row = asObj(row); const raw=asObj(row.raw);
    const id = pick(row,['object_id','id','source_id','sifra','broj','katastarski_broj']) || row.object_id || row.id || row.source_id || cryptoId(row);
    const name = pick(row,['object_name','name','naziv','object_name','ime','naziv_objekta','object_title']) || ('Objekt '+id);
    const lat = num(pick(row,['lat','latitude','wgs84_lat','gps_lat','y','coord_lat','koordinata_lat','n']));
    const lon = num(pick(row,['lon','lng','longitude','wgs84_lon','wgs84_lng','gps_lon','gps_lng','x','coord_lon','koordinata_lon','e']));
    const missingCategories = arr(row.missing_categories||row.missing_categories_text);
    let baseInCadastre = bool(row.base_in_cadastre);
    const readyRaw = str(row.katastar_readiness||'');
    const negativeReady = /nije[_\s-]*u[_\s-]*katastru|nepotpuno|provjeriti/i.test(readyRaw);
    if(negativeReady) baseInCadastre = false;
    const baseReady = bool(row.base_ready_for_katastar);
    const out={
      ...raw,
      ...row,
      id:String(id),
      object_id:String(row.object_id||id),
      source_id: str(row.source_id||row.object_id||id),
      name: str(name),
      lat, lon,
      has_map_point:Number.isFinite(lat)&&Number.isFinite(lon),
      object_type_final: str(pick(row,['object_type','object_type_final','tip','vrsta','type'])),
      county: str(pick(row,['county','zupanija','županija'])),
      municipality: str(pick(row,['municipality','opcina','općina','grad'])),
      nearest_place: str(pick(row,['nearest_place','najblize_mjesto','najbliže_mjesto','mjesto','lokacija'])),
      locality: str(pick(row,['locality','predio','lokalitet'])),
      depth_m: pick(row,['depth_m','dubina_m','dubina','depth']),
      length_m: pick(row,['length_m','duljina_m','duzina_m','duljina','duzina','length']),
      cadastral_number: str(pick(row,['plate_number','cadastral_number','katastarski_broj','kbr','broj_plocice','broj_pločice','plocica','pločica'])),
      cadastre_status: str(pick(row,['cadastre_status','katastar_status','status_katastra'])),
      record_status: str(pick(row,['record_status','status','status_zapisa'])),
      archive_status: str(row.archive_status||''),
      katastar_readiness: str(row.katastar_readiness||''),
      base_in_cadastre: baseInCadastre,
      base_ready_for_katastar: baseReady,
      missing_categories: missingCategories,
      missing_categories_text: str(row.missing_categories_text||missingCategories.join(', ')),
      missing_plate: !!bool(row.missing_plate),
      missing_coordinates: !!bool(row.missing_coordinates),
      missing_drawing: !!bool(row.missing_drawing),
      missing_record: !!bool(row.missing_record),
      has_coordinates: bool(row.has_coordinates),
      has_drawing: bool(row.has_drawing),
      has_record: bool(row.has_record),
      has_plate: bool(row.has_plate),
      has_photo: bool(row.has_photo),
      needs_redraw: !!bool(row.needs_redraw),
      access_description: str(pick(row,['access_description','pristup','opis_pristupa','access'])),
      technical_description: str(pick(row,['technical_description','opis','opis_objekta','description','tehnicki_opis','tehnički_opis','morfologija'])),
      note: str(pick(row,['last_note','note','napomena','notes','remarks'])),
      field_tasks: pick(row,['field_tasks','zadaci','workflow_tasks']),
      workflow_raw: pick(row,['workflow_raw','workflow','raw_status']),
      priority_score: Number(row.priority_score||0),
      sql_source_system: str(row.source_system||row.sql_source_system||'supabase'),
      sql_updated_at: row.updated_at||row.promoted_from_staging_at||row.created_at||''
    };
    return out;
  }
  function uniqueRows(rows){const seen=new Set(); const out=[]; for(const r of rows){const o=normalize(r); const k=String(o.id||o.object_id||''); if(!k||seen.has(k))continue; seen.add(k); out.push(o);} return out;}
  async function readViewPaged(){
    const client=sb(); if(!client) throw new Error('Supabase client nije dostupan');
    const out=[];
    for(let offset=0; offset<MAX_ROWS; offset+=PAGE_SIZE){
      const {data,error}=await client.rpc(MAP_PAGE_RPC,{p_offset:offset,p_limit:PAGE_SIZE});
      if(error) throw error;
      const rows=Array.isArray(data?.rows)?data.rows:(Array.isArray(data)?data:[]);
      out.push(...rows);
      if(!rows.length || rows.length<PAGE_SIZE) break;
    }
    return out;
  }
  async function readTablePaged(table, orderCol){
    const client=sb(); if(!client) throw new Error('Supabase client nije dostupan');
    const out=[];
    for(let from=0; from<MAX_ROWS; from+=PAGE_SIZE){
      let q=client.from(table).select('*').range(from,from+PAGE_SIZE-1);
      if(orderCol) q=q.order(orderCol,{ascending:false});
      const {data,error}=await q;
      if(error) throw error;
      out.push(...(data||[]));
      if(!data || data.length<PAGE_SIZE) break;
    }
    return out;
  }
  async function loadObjects(fallbackUrl){
    const client=sb();
    if(client){
      try{
        const rows=await readViewPaged();
        const objects=uniqueRows(rows);
        if(objects.length){window.SOV_MAP_DATA_SOURCE='Arhivar worklist · cijela baza ('+objects.length+')'; return objects;}
      }catch(e){console.warn('[SOV Karta] arhivar paged RPC fallback',e);}
      try{
        const rows=await readTablePaged(ARHIVAR_VIEW,'priority_score');
        const objects=uniqueRows(rows);
        if(objects.length){window.SOV_MAP_DATA_SOURCE='Arhivar worklist · tablica ('+objects.length+')'; return objects;}
      }catch(e){console.warn('[SOV Karta] arhivar view fallback',e);}
      try{
        // Legacy set-returning RPC can be capped by PostgREST; keep only as fallback.
        const {data,error}=await client.rpc(MAP_RPC,{p_limit:25000});
        if(error) throw error;
        const objects=uniqueRows((data||[]).map(r=>r&&r.data?r.data:r));
        if(objects.length){window.SOV_MAP_DATA_SOURCE='Legacy map RPC · ograničeni fallback ('+objects.length+')'; return objects;}
      }catch(e){console.warn('[SOV Karta] legacy RPC fallback',e);}
      try{
        const rows=await readTablePaged(STAGING_TABLE);
        const objects=uniqueRows(rows);
        if(objects.length){window.SOV_MAP_DATA_SOURCE='Supabase · speleo_objects_staging ('+objects.length+')'; return objects;}
      }catch(e){console.warn('[SOV Karta] staging fallback',e);}
      try{
        const rows=await readTablePaged(LIVE_TABLE);
        const objects=uniqueRows(rows);
        if(objects.length){window.SOV_MAP_DATA_SOURCE='Supabase · speleo_objects_live_sql ('+objects.length+')'; return objects;}
      }catch(e){console.warn('[SOV Karta] live fallback',e);}
    }
    const r=await fetch(fallbackUrl||'data/sov-baza.json',{cache:'no-store'});
    if(!r.ok) throw new Error('JSON fallback HTTP '+r.status);
    const data=await r.json();
    window.SOV_MAP_DATA_SOURCE='JSON fallback';
    return uniqueRows(Array.isArray(data)?data:[]);
  }
  window.SOVMapDB={loadObjects,normalize,pick,num,bool,build:'5.58.19'};
})();
