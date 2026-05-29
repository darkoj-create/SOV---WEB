(function(){
  const MAP_RPC='sov_map_objects';
  const STAGING_TABLE='speleo_objects_staging';
  const LIVE_TABLE='speleo_objects_live_sql';
  const MAX_ROWS=25000;
  function sb(){return window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();}
  function normKey(s){return String(s||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9]+/g,'_').replace(/^_|_$/g,'');}
  function asObj(v){return v&&typeof v==='object'&&!Array.isArray(v)?v:{};}
  function allSources(row){const raw=asObj(row.raw); const meta=asObj(row.metadata); return [row, raw, meta];}
  function pick(row, keys){
    const sources=allSources(row);
    const wanted=keys.map(normKey);
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
  function str(v){return v===null||v===undefined?'':String(v).trim();}
  function normalize(row){
    row = row && row.data ? row.data : row;
    row = asObj(row);
    const raw=asObj(row.raw);
    const id = pick(row,['id','object_id','source_id','sifra','broj','katastarski_broj']) || row.id || row.source_id || cryptoId(row);
    const name = pick(row,['name','naziv','object_name','ime','naziv_objekta','object_title']) || ('Objekt '+id);
    const lat = num(pick(row,['lat','latitude','wgs84_lat','gps_lat','y','coord_lat','koordinata_lat','n']));
    const lon = num(pick(row,['lon','lng','longitude','wgs84_lon','wgs84_lng','gps_lon','gps_lng','x','coord_lon','koordinata_lon','e']));
    const out={
      ...raw,
      ...row,
      id:id,
      source_id: str(row.source_id||id),
      name: str(name),
      lat, lon,
      object_type_final: str(pick(row,['object_type_final','object_type','tip','vrsta','type'])),
      county: str(pick(row,['county','zupanija','županija'])),
      municipality: str(pick(row,['municipality','opcina','općina','grad'])),
      nearest_place: str(pick(row,['nearest_place','najblize_mjesto','najbliže_mjesto','mjesto','lokacija'])),
      locality: str(pick(row,['locality','predio','lokalitet'])),
      depth_m: pick(row,['depth_m','dubina_m','dubina','depth']),
      length_m: pick(row,['length_m','duljina_m','duzina_m','duljina','duzina','length']),
      cadastral_number: str(pick(row,['cadastral_number','katastarski_broj','kbr','broj_plocice','broj_pločice','plocica','pločica'])),
      cadastre_status: str(pick(row,['cadastre_status','katastar_status','status_katastra'])),
      record_status: str(pick(row,['record_status','status','status_zapisa'])),
      access_description: str(pick(row,['access_description','pristup','opis_pristupa','access'])),
      technical_description: str(pick(row,['technical_description','opis','opis_objekta','description','tehnicki_opis','tehnički_opis','morfologija'])),
      note: str(pick(row,['note','napomena','notes','remarks'])),
      field_tasks: pick(row,['field_tasks','zadaci','workflow_tasks']),
      workflow_raw: pick(row,['workflow_raw','workflow','raw_status']),
      sql_source_system: str(row.source_system||row.sql_source_system||'supabase'),
      sql_updated_at: row.updated_at||row.promoted_from_staging_at||row.created_at||''
    };
    return out;
  }
  function cryptoId(row){
    const s=JSON.stringify(row||{}); let h=0; for(let i=0;i<s.length;i++){h=((h<<5)-h+s.charCodeAt(i))|0;} return 'row_'+Math.abs(h);
  }
  async function readTable(table){
    const client=sb(); if(!client) throw new Error('Supabase client nije dostupan');
    let out=[]; const step=1000;
    for(let from=0; from<MAX_ROWS; from+=step){
      const {data,error}=await client.from(table).select('*').range(from,from+step-1);
      if(error) throw error;
      out=out.concat(data||[]);
      if(!data || data.length<step) break;
    }
    return out;
  }
  async function loadObjects(fallbackUrl){
    const client=sb();
    if(client){
      try{
        const {data,error}=await client.rpc(MAP_RPC,{p_limit:MAX_ROWS});
        if(error) throw error;
        const rows=(data||[]).map(r=>r&&r.data?r.data:r);
        const objects=rows.map(normalize).filter(o=>o.name&&Number.isFinite(o.lat)&&Number.isFinite(o.lon));
        if(objects.length){window.SOV_MAP_DATA_SOURCE='Supabase · prava baza objekata'; return objects;}
      }catch(e){console.warn('[SOV Karta] RPC fallback',e);}
      try{
        const rows=await readTable(STAGING_TABLE);
        const objects=rows.map(normalize).filter(o=>o.name&&Number.isFinite(o.lat)&&Number.isFinite(o.lon));
        if(objects.length){window.SOV_MAP_DATA_SOURCE='Supabase · speleo_objects_staging'; return objects;}
      }catch(e){console.warn('[SOV Karta] staging fallback',e);}
      try{
        const rows=await readTable(LIVE_TABLE);
        const objects=rows.map(normalize).filter(o=>o.name&&Number.isFinite(o.lat)&&Number.isFinite(o.lon));
        if(objects.length){window.SOV_MAP_DATA_SOURCE='Supabase · speleo_objects_live_sql'; return objects;}
      }catch(e){console.warn('[SOV Karta] live fallback',e);}
    }
    const r=await fetch(fallbackUrl||'data/sov-baza.json',{cache:'no-store'});
    if(!r.ok) throw new Error('JSON fallback HTTP '+r.status);
    const data=await r.json();
    window.SOV_MAP_DATA_SOURCE='JSON fallback';
    return (Array.isArray(data)?data:[]).map(normalize).filter(o=>o.name&&Number.isFinite(o.lat)&&Number.isFinite(o.lon));
  }
  window.SOVMapDB={loadObjects,normalize,pick,num};
})();
