const SQL_DATA_URL='data/baza_velebit_2026_appready.json';
const SPELEO_IMPORT_STATE_KEY='sov_speleo_sql_import_state_v507';
const SPELEO_IMPORT_CHUNK_SIZE=150;
function speleoImportEls(){
  return [document.getElementById('speleoSqlImportStatus'),document.getElementById('speleoSqlImportStatusCard')].filter(Boolean);
}
function speleoSetImportStatus(txt, pct){
  speleoImportEls().forEach(el=>{
    el.innerHTML = String(txt||'');
    if(typeof pct==='number'){
      const safe=Math.max(0,Math.min(100,Math.round(pct)));
      el.innerHTML += `<div style="height:9px;border-radius:999px;background:#10191d;overflow:hidden;margin-top:8px"><div style="height:100%;width:${safe}%;background:#d7f66f"></div></div><small>${safe}%</small>`;
    }
  });
}
function normalizeSpeleoSqlRow(o){
  const row={...o};
  // Supabase columns are lower snake_case and already match appready JSON.
  // Keep id stable; strip undefined because PostgREST rejects undefined-ish payloads.
  Object.keys(row).forEach(k=>{ if(row[k]===undefined) delete row[k]; });
  return row;
}

function speleoNormalizeNameForKey(v){
  return String(v||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9]+/g,' ').trim();
}
function speleoHash32(str){
  let h=2166136261;
  for(let i=0;i<String(str).length;i++){
    h^=String(str).charCodeAt(i);
    h=Math.imul(h,16777619);
  }
  return h>>>0;
}
function prepareSpeleoSqlImportRows(payload){
  const idCounts=new Map();
  payload.forEach(o=>{ const raw=String(o&&o.id!=null?o.id:''); idCounts.set(raw,(idCounts.get(raw)||0)+1); });
  const seenOrdinal=new Map();
  const byFinalId=new Map();
  let duplicateSourceIds=0;
  let generatedIds=0;
  let skippedNoId=0;
  for(const original of payload){
    if(!original || original.id==null || original.id==='') { skippedNoId++; continue; }
    const rawId=String(original.id);
    const count=idCounts.get(rawId)||0;
    const ordinal=(seenOrdinal.get(rawId)||0)+1;
    seenOrdinal.set(rawId,ordinal);
    const row=normalizeSpeleoSqlRow(original);
    // The source JSON currently contains repeated numeric IDs for different objects.
    // Postgres cannot upsert two rows with the same conflict key in one statement.
    // Keep the first occurrence on the original ID; give later occurrences a stable synthetic bigint ID.
    if(count>1){
      duplicateSourceIds++;
      if(ordinal>1){
        const nameKey=speleoNormalizeNameForKey(original.name||'object');
        row.id=900000000000 + speleoHash32(rawId+'|'+nameKey+'|'+ordinal);
        generatedIds++;
      }
    }
    // If an exact duplicate still collapses to the same final ID, last one wins locally before upsert.
    byFinalId.set(String(row.id),row);
  }
  return {rows:[...byFinalId.values()], duplicateSourceIds, generatedIds, skippedNoId, uniqueFinalIds:byFinalId.size};
}

async function loadSpeleoObjectsSqlFirst(){
  const sb=window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();
  if(sb){
    try{
      const {data,error}=await sb.from('speleo_objects').select('*').order('id',{ascending:true}).limit(20000);
      if(error) throw error;
      if(data&&data.length){
        window.__SOV_SPELEO_SOURCE='Supabase SQL';
        return data.map(r=>({...r,id:Number(r.id)}));
      }
      window.__SOV_SPELEO_SOURCE='SQL prazan — JSON fallback';
    }catch(e){
      console.warn('Speleo SQL load fallback:',e.message||e);
      window.__SOV_SPELEO_SOURCE='SQL greška — JSON fallback';
    }
  }else{
    window.__SOV_SPELEO_SOURCE='Bez Supabasea — JSON fallback';
  }
  const res=await fetch(SQL_DATA_URL,{cache:'no-store'});
  if(!res.ok) throw new Error('Ne mogu učitati '+SQL_DATA_URL);
  return await res.json();
}
async function importSpeleoJsonToSql(){
  try{
    const sb=window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();
    if(!sb) throw new Error('Supabase nije konfiguriran.');
    speleoSetImportStatus('Učitavam JSON bazu...', 1);
    await new Promise(r=>setTimeout(r,50));
    const payload=await fetch(SQL_DATA_URL,{cache:'no-store'}).then(r=>{if(!r.ok)throw new Error('Ne mogu učitati '+SQL_DATA_URL);return r.json();});
    if(!Array.isArray(payload)||!payload.length) throw new Error('JSON baza je prazna ili nije lista objekata.');
    const prepared=prepareSpeleoSqlImportRows(payload);
    const rows=prepared.rows;
    const total=rows.length;
    let state={started_at:new Date().toISOString(),source_total:payload.length,total,done:0,errors:[],generated_ids:prepared.generatedIds};
    localStorage.setItem(SPELEO_IMPORT_STATE_KEY,JSON.stringify(state));
    speleoSetImportStatus(`Import kreće u batchovima od ${SPELEO_IMPORT_CHUNK_SIZE}. JSON ima ${payload.length} redova, za SQL je pripremljeno ${total} jedinstvenih objekata. ${prepared.generatedIds?('Za '+prepared.generatedIds+' duplih source ID-jeva dodijeljen je stabilan interni ID. '):''}`,2);
    for(let i=0;i<total;i+=SPELEO_IMPORT_CHUNK_SIZE){
      const rawChunk=rows.slice(i,i+SPELEO_IMPORT_CHUNK_SIZE);
      const chunk=[...new Map(rawChunk.map(r=>[String(r.id),r])).values()];
      const pct=(i/total)*100;
      speleoSetImportStatus(`Upisujem ${i+1}–${Math.min(i+chunk.length,total)} od ${total} objekata...`,pct);
      await new Promise(r=>setTimeout(r,20));
      const {error}=await sb.from('speleo_objects').upsert(chunk,{onConflict:'id'});
      if(error) throw error;
      state.done=Math.min(i+chunk.length,total);
      localStorage.setItem(SPELEO_IMPORT_STATE_KEY,JSON.stringify(state));
    }
    speleoSetImportStatus(`Import gotov: ${total} objekata upisano/azurirano u SQL. Preskočeno bez ID-a: ${prepared.skippedNoId}. Osvježavam prikaz baze...`,100);
    localStorage.removeItem(SPELEO_IMPORT_STATE_KEY);
    await new Promise(r=>setTimeout(r,250));
    if(window.refreshSpeleoBaza){
      await window.refreshSpeleoBaza({afterImport:true});
      speleoSetImportStatus(`Gotovo. SQL baza je učitana i prikaz osvježen.`,100);
    }else{
      location.reload();
    }
  }catch(e){
    console.error(e);
    speleoSetImportStatus('GREŠKA: '+(e.message||e));
    alert('GREŠKA: '+(e.message||e));
  }
}
async function continueSpeleoJsonToSql(){
  // Kept as alias for future UI. Current importer is safe to re-run: upsert by id.
  return importSpeleoJsonToSql();
}
