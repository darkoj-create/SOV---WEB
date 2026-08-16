from pathlib import Path
import json
import re
import time
import urllib.parse
import urllib.request

BASE='https://script.google.com/macros/s/AKfycbx1Hg_s6mAdWgB7p559USC8dAMIhteJQ3RFhFgp8rkqzYEVqMfwZm-lrl2v7UmW8gvSyg/exec'

def get(params):
    url=BASE+'?'+urllib.parse.urlencode(params)
    with urllib.request.urlopen(url,timeout=120) as r:
        return json.loads(r.read().decode('utf-8'))

print('REBUILD',get({'action':'rebuildDrawingsIndex'}))
time.sleep(1)
idx=get({'action':'listDrawings','limit':2000})
count=int(idx.get('count') or 0)
print('CANONICAL_COUNT',count)
if count < 1084:
    raise SystemExit(f'Canonical count too low: {count}')

for q in ['Kopitarka','Kozje tajne','Ledeno gnijezdo','Magični portal','Uske muke']:
    p=get({'action':'searchDrawings','query':q})
    rows=p.get('drawings') or p.get('results') or []
    print(q,'RESULTS',len(rows),[(x.get('fileName') or x.get('name')) for x in rows[:5]])
    if not rows:
        raise SystemExit('Missing representative drawing: '+q)

p=Path('karta.html')
s=p.read_text(encoding='utf-8')
new=r'''async function loadDrawings(){
  drawingsByObject.clear();
  let androidMap={};
  try{androidMap=await(window.SOVAndroidDrawingMap||Promise.resolve({}))}catch(e){console.warn('Android nacrti map nije učitan',e)}
  const normName=s=>String(s||'').normalize('NFC').toLowerCase().replace(/\.(png|jpg|jpeg|tif|tiff|gif)$/i,'.webp');
  const androidByNorm=new Map(Object.entries(androidMap||{}).map(([name,id])=>[normName(name),String(id)]));
  const seen=new Set();
  let canonicalLoaded=0,canonicalMatched=0,supabaseLoaded=0,remapped=0;
  const add=(r,webId='')=>{
    const file=String(r.fileName||r.drive_file_name||r.name||'');
    const androidId=(androidMap&&androidMap[file])||androidByNorm.get(normName(file))||'';
    const k=String(androidId||webId||'');
    if(!k)return false;
    const driveId=String(r.fileId||r.drive_file_id||r.id||'');
    const dedupe=driveId||k+'|'+file;
    if(seen.has(dedupe))return false;
    seen.add(dedupe);
    if(androidId&&webId&&webId!==String(androidId))remapped++;
    const row={...r,object_id:k,drive_file_id:driveId,drive_file_name:file,name:file,mime_type:r.mimeType||r.mime_type||'',file_size:r.sizeBytes||r.file_size||'',android_record_id:androidId||null};
    if(!drawingsByObject.has(k))drawingsByObject.set(k,[]);
    drawingsByObject.get(k).push(row);
    return !!androidId;
  };
  try{
    const base=String(window.SOV_DRAWINGS_SYNC_ENDPOINT||'').trim();
    if(base){
      const res=await fetch(base+(base.includes('?')?'&':'?')+'action=listDrawings&limit=2000',{cache:'no-store'});
      if(!res.ok)throw new Error('Drawings index HTTP '+res.status);
      const payload=await res.json();
      const rows=Array.isArray(payload?.drawings)?payload.drawings:[];
      canonicalLoaded=rows.length;
      rows.forEach(r=>{if(add(r,''))canonicalMatched++});
    }
  }catch(e){console.warn('Canonical nacrti index nije dostupan, koristim Supabase fallback',e)}
  try{
    const sb=window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();
    if(sb){
      const {data,error}=await sb.from('speleo_object_drawings').select('object_id,drive_file_id,drive_file_name,mime_type,file_size,match_score,match_status,metadata').limit(10000);
      if(error)throw error;
      supabaseLoaded=(data||[]).length;
      (data||[]).forEach(r=>add(r,String(r.object_id||'')));
    }
  }catch(e){console.warn('Supabase nacrti fallback nije učitan',e)}
  window.SOV_DRAWINGS_STATUS={source:canonicalLoaded?'canonical+supabase':'supabase',canonicalLoaded,canonicalMatched,supabaseLoaded,androidVerified:Object.keys(androidMap||{}).length,remapped,objects:drawingsByObject.size,total:[...drawingsByObject.values()].reduce((n,a)=>n+a.length,0)};
}'''
pattern=r'async function loadDrawings\(\)\{.*?\}\nfunction applyPointFromParams'
out,n=re.subn(pattern,new+'\nfunction applyPointFromParams',s,count=1,flags=re.S)
if n != 1:
    raise SystemExit(f'Expected one loadDrawings block, got {n}')
p.write_text(out,encoding='utf-8')
print('KARTA_PATCHED')
