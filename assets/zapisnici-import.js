(function(){
  'use strict';
  const fileInput = document.getElementById('files');
  const batchInput = document.getElementById('batch');
  const typeInput = document.getElementById('docType');
  const statusBox = document.getElementById('statusBox');
  const progressBar = document.getElementById('progressBar');
  const preview = document.getElementById('preview');
  const startBtn = document.getElementById('startUpload');
  const dryBtn = document.getElementById('dryRun');
  const logBox = document.getElementById('logBox');
  const monthByName = {'sijecanj':1,'siječanj':1,'veljaca':2,'veljača':2,'ozujak':3,'ožujak':3,'travanj':4,'svibanj':5,'lipanj':6,'srpanj':7,'kolovoz':8,'rujan':9,'listopad':10,'studeni':11,'prosinac':12};
  let queue = [];
  function esc(value){return String(value == null ? '' : value).replace(/[&<>"']/g, ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));}
  function slug(s){return String(s||'file').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[^a-z0-9.]+/g,'-').replace(/-+/g,'-').replace(/^-|-$/g,'').slice(0,140) || 'file';}
  function fmtBytes(n){n=Number(n||0); if(n>=1048576)return(n/1048576).toFixed(1).replace('.0','')+' MB'; if(n>=1024)return Math.round(n/1024)+' KB'; return n+' B';}
  function parseFile(file){
    const rel = file.webkitRelativePath || file.name;
    const name = file.name;
    const hay = rel + ' ' + name;
    const yMatch = hay.match(/(19[6-9][0-9]|20[0-2][0-9])/);
    const year = yMatch ? Number(yMatch[1]) : null;
    let date = null, month = null, day = null;
    let m = hay.match(/(19[6-9][0-9]|20[0-2][0-9])[-_. ]([01]?[0-9])[-_. ]([0-3]?[0-9])/);
    if(m){ date = `${m[1]}-${String(m[2]).padStart(2,'0')}-${String(m[3]).padStart(2,'0')}`; month=Number(m[2]); day=Number(m[3]); }
    if(!date){
      m = hay.match(/([0-3]?[0-9])[-_. ]([01]?[0-9])[-_. ](19[6-9][0-9]|20[0-2][0-9])/);
      if(m){ date = `${m[3]}-${String(m[2]).padStart(2,'0')}-${String(m[1]).padStart(2,'0')}`; month=Number(m[2]); day=Number(m[1]); }
    }
    if(!month){
      Object.keys(monthByName).some(k=>{ if(hay.toLowerCase().includes(k)){month=monthByName[k]; return true;} return false; });
    }
    const ext = (name.match(/\.([a-z0-9]+)$/i)||[])[1] || 'file';
    const cleanTitle = name.replace(/\.[a-z0-9]+$/i,'').replace(/[_-]+/g,' ').replace(/\s+/g,' ').trim();
    const finalYear = year || (date ? Number(date.slice(0,4)) : null);
    const safeName = slug(name);
    const storagePath = finalYear ? `zapisnici/${finalYear}/${safeName}` : `zapisnici/unsorted/${Date.now()}-${safeName}`;
    return {file, rel, title:cleanTitle, year:finalYear, month:month||null, day:day||null, date, ext:ext.toUpperCase(), storagePath, sizeLabel:fmtBytes(file.size)};
  }
  function buildQueue(){
    queue = Array.from(fileInput.files || []).filter(f=>!f.name.startsWith('.')).map(parseFile);
    preview.innerHTML = queue.length ? queue.slice(0,80).map(x=>`<tr><td>${esc(x.year||'?')}</td><td>${esc(x.date||'')}</td><td>${esc(x.title)}</td><td>${esc(x.sizeLabel)}</td><td>${esc(x.storagePath)}</td></tr>`).join('') : '<tr><td colspan="5">Nema odabranih datoteka.</td></tr>';
    statusBox.innerHTML = `<strong>${queue.length}</strong> datoteka spremno za provjeru/upload.`;
  }
  function log(msg, bad){
    const div = document.createElement('div'); div.className = bad ? 'bad' : 'ok'; div.textContent = msg; logBox.prepend(div);
  }
  async function uploadAll(dryRun){
    await SOVAuth.ready();
    const sb = SOVAuth.getClient();
    if(!sb) { alert('Supabase nije konfiguriran.'); return; }
    if(!queue.length) buildQueue();
    const invalid = queue.filter(x=>!x.year);
    if(invalid.length && !confirm(`${invalid.length} datoteka nema prepoznatu godinu. Nastaviti u unsorted?`)) return;
    startBtn.disabled = true; dryBtn.disabled = true; logBox.innerHTML = '';
    const batch = batchInput.value.trim() || `batch-${new Date().toISOString().slice(0,10)}`;
    let done = 0, failed = 0;
    for(const item of queue){
      try{
        if(!dryRun){
          const up = await sb.storage.from('sov-documents').upload(item.storagePath, item.file, {cacheControl:'3600', upsert:false, contentType:item.file.type || undefined});
          if(up.error){
            if(String(up.error.message || '').toLowerCase().includes('already exists')){
              item.storagePath = item.storagePath.replace(/(\.[a-z0-9]+)$/i, `-${Date.now()}$1`);
              const up2 = await sb.storage.from('sov-documents').upload(item.storagePath, item.file, {cacheControl:'3600', upsert:false, contentType:item.file.type || undefined});
              if(up2.error) throw up2.error;
            } else throw up.error;
          }
          const payload = {
            collection:'zapisnici_sastanaka', title:item.title, document_type:typeInput.value || 'zapisnik sastanka', document_date:item.date,
            year:item.year || 0, month:item.month, day:item.day, original_filename:item.file.name, storage_bucket:'sov-documents', storage_path:item.storagePath,
            mime_type:item.file.type || null, format:item.ext, size_bytes:item.file.size, source_batch:batch, status:'active', visibility:'members'
          };
          const ins = await sb.from('sov_document_archive').insert(payload);
          if(ins.error) throw ins.error;
        }
        done++; log((dryRun?'OK dry-run: ':'Upload OK: ')+item.file.name);
      }catch(err){ failed++; log('Greška: '+item.file.name+' — '+(err.message || err), true); }
      progressBar.style.width = Math.round(((done+failed)/queue.length)*100) + '%';
      statusBox.innerHTML = `<strong>${done}</strong> uspješno · <strong>${failed}</strong> grešaka · ${queue.length} ukupno`;
    }
    startBtn.disabled = false; dryBtn.disabled = false;
  }
  fileInput.addEventListener('change', buildQueue);
  dryBtn.addEventListener('click',()=>uploadAll(true));
  startBtn.addEventListener('click',()=>uploadAll(false));
})();
