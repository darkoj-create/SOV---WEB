

const SQL_DATA_URL='data/baza_velebit_2026_appready.json';
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
    }catch(e){console.warn('Speleo SQL load fallback:',e.message||e)}
  }
  const res=await fetch(SQL_DATA_URL,{cache:'no-store'});
  if(!res.ok) throw new Error('Ne mogu učitati '+SQL_DATA_URL);
  window.__SOV_SPELEO_SOURCE='JSON fallback';
  return await res.json();
}
async function importSpeleoJsonToSql(){
  const status=document.getElementById('speleoSqlImportStatus'); const statusCard=document.getElementById('speleoSqlImportStatusCard');
  try{
    if(status) status.textContent='SQL import kreće...';
    const sb=window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();
    if(!sb) throw new Error('Supabase nije konfiguriran.');
    const payload=await fetch(SQL_DATA_URL,{cache:'no-store'}).then(r=>r.json());
    const {data,error}=await sb.rpc('import_speleo_objects_from_json',{payload,source_name:'baza_velebit_2026_appready.json'});
    if(error) throw error;
    if(status) status.textContent='Import gotov: '+(data&&data.rows_upserted||payload.length)+' objekata upisano/azurirano u SQL.';
    alert('Import gotov. Refresham bazu.');
    location.reload();
  }catch(e){console.error(e); if(status) status.textContent='GREŠKA: '+(e.message||e); alert('GREŠKA: '+(e.message||e));}
}

