const SQL_DATA_URL='data/baza_velebit_2026_appready.json';
const SPELEO_IMPORT_STATE_KEY='sov_speleo_sql_import_state_v503';
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
async function loadSpeleoObjectsSqlFirst(){
  const sb=window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();
  if(sb){
    try{
      const {data,error}=await sb.from('speleo_objects').select('*').order('name',{ascending:true}).limit(20000);
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
    const total=payload.length;
    let state={started_at:new Date().toISOString(),total,done:0,errors:[]};
    localStorage.setItem(SPELEO_IMPORT_STATE_KEY,JSON.stringify(state));
    speleoSetImportStatus(`Import kreće u batchovima od ${SPELEO_IMPORT_CHUNK_SIZE}. Ukupno ${total} objekata.`,2);
    for(let i=0;i<total;i+=SPELEO_IMPORT_CHUNK_SIZE){
      const chunk=payload.slice(i,i+SPELEO_IMPORT_CHUNK_SIZE).map(normalizeSpeleoSqlRow);
      const pct=(i/total)*100;
      speleoSetImportStatus(`Upisujem ${i+1}–${Math.min(i+chunk.length,total)} od ${total} objekata...`,pct);
      await new Promise(r=>setTimeout(r,20));
      const {error}=await sb.from('speleo_objects').upsert(chunk,{onConflict:'id'});
      if(error) throw error;
      state.done=Math.min(i+chunk.length,total);
      localStorage.setItem(SPELEO_IMPORT_STATE_KEY,JSON.stringify(state));
    }
    speleoSetImportStatus(`Import gotov: ${total} objekata upisano/azurirano u SQL. Refresham prikaz...`,100);
    localStorage.removeItem(SPELEO_IMPORT_STATE_KEY);
    await new Promise(r=>setTimeout(r,650));
    location.reload();
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
