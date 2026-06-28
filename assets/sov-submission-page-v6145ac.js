(function(){
const BUILD='6.1.45ac';
const BUCKET='speleo-submissions';
const DRAFT_KEY='sov_predaj_novu_jamu_draft_v609';
const LEGACY_KEYS=['sov_predaj_novu_jamu_draft_v605','sov_predaj_novu_jamu_draft_v603'];
const HR_BBOX={latMin:42,latMax:47,lonMin:13,lonMax:20};
const FILE_RULES={
 photos:{label:'Fotografije',ext:['jpg','jpeg','png','webp'],max:25*1024*1024},
 drawings:{label:'Nacrti',ext:['pdf','jpg','jpeg','png','webp','tif','tiff','svg'],max:60*1024*1024},
 kml:{label:'KML/KMZ',ext:['kml','kmz'],max:25*1024*1024},
 gpx:{label:'GPX',ext:['gpx'],max:25*1024*1024},
 topodroid:{label:'TopoDroid',ext:['zip','th','th2','tdr','tro'],max:200*1024*1024},
 other_files:{label:'Ostalo',ext:null,max:120*1024*1024}
};
const $=s=>document.querySelector(s);
const $$=s=>Array.from(document.querySelectorAll(s));
function esc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function toast(msg,kind='info'){
  let el=document.querySelector('.sf-toast');
  if(!el){el=document.createElement('div');el.className='sf-toast';document.body.appendChild(el);}
  el.textContent=msg;
  el.dataset.kind=kind;
  clearTimeout(el._t);
  el._t=setTimeout(()=>el.remove(),5200);
}
function status(msg,kind){
  const el=$('#submitStatus'); if(!el)return;
  el.textContent=msg;
  el.classList.remove('ok','bad','warn');
  if(kind)el.classList.add(kind);
}
function sb(){return window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();}
async function currentUser(){return window.SOVAuth&&SOVAuth.currentUser?SOVAuth.currentUser():null;}
function cleanNumber(v){const s=String(v||'').trim().replace(',','.'); if(!s)return null; const n=Number(s); return Number.isFinite(n)?n:null;}
function formDataObj(form){const fd=new FormData(form); const get=k=>String(fd.get(k)||'').trim(); const checked=k=>Array.from(form.querySelectorAll(`input[name="${k}"]:checked`)).map(x=>x.value); return {
 object_name:get('object_name'), object_type:get('object_type'), county:get('county'), municipality:get('municipality'), nearest_place:get('nearest_place'), locality:get('locality'),
 lat:cleanNumber(get('lat')), lon:cleanNumber(get('lon')), htrs_x:get('htrs_x'), htrs_y:get('htrs_y'), coordinate_source:get('coordinate_source'), entrance_altitude_m:cleanNumber(get('entrance_altitude_m')),
 depth_m:cleanNumber(get('depth_m')), length_m:cleanNumber(get('length_m')), survey_date:get('survey_date'), team:get('team'), surveyors:get('surveyors'), drawn_by:get('drawn_by'), submitter_phone:get('submitter_phone'),
 plate_code:get('plate_code'), cadastre_hint:get('cadastre_hint'), access_description:get('access_description'), technical_description:get('technical_description'), research_history:get('research_history'),
 morphology_hydrology:get('morphology_hydrology'), hazards_protection:get('hazards_protection'), biology_archaeology:get('biology_archaeology'), equipment_note:get('equipment_note'), protection_note:get('protection_note'), notes:get('notes'),
 missing_expected:checked('missing_expected'), form_version:BUILD, submitted_from:'predaj-novu-jamu.html'
};}
function collectFiles(form){const out=[]; const map={photos:'photo',drawings:'drawing',kml:'kml',gpx:'gpx',topodroid:'topodroid_zip',other_files:'other'}; Object.entries(map).forEach(([field,type])=>{const input=form.elements[field]; if(input&&input.files){Array.from(input.files).forEach(file=>out.push({type,file}));}}); return out;}
function setBusy(b){document.querySelectorAll('[data-submit-btn],#saveDraftBtn,#clearDraftBtn,#downloadDraftBtn').forEach(btn=>{btn.disabled=b; btn.style.opacity=b?'.62':'';});}
function progress(current,total,label){const wrap=$('#uploadProgress'); if(!wrap)return; wrap.classList.add('on'); const pct=total?Math.round((current/total)*100):0; wrap.querySelector('[data-progress-bar]').style.width=pct+'%'; wrap.querySelector('[data-progress-label]').textContent=label||`${current}/${total}`;}
function nowTime(){return new Date().toLocaleTimeString('hr-HR',{hour:'2-digit',minute:'2-digit'});}
function setDraftInfo(text){const info=$('#draftInfo'); if(info)info.textContent=text;}
function saveDraft(silent=false){
  const form=$('#newCaveForm'); if(!form)return;
  const data=formDataObj(form);
  try{
    localStorage.setItem(DRAFT_KEY,JSON.stringify({...data,saved_at:new Date().toISOString()}));
    setDraftInfo('Skica spremljena u '+nowTime()+'.');
    if(!silent){status('Skica spremljena u '+nowTime()+'.','ok');toast('Skica spremljena lokalno.','ok');}
  }catch(e){if(!silent){status('Ne mogu spremiti skicu: '+(e.message||e),'bad');toast('Ne mogu spremiti skicu: '+(e.message||e),'bad');}}
}
function autoSave(){clearTimeout(autoSave._t); autoSave._t=setTimeout(()=>saveDraft(true),750);}
function loadDraft(){try{let raw=localStorage.getItem(DRAFT_KEY); if(!raw){for(const k of LEGACY_KEYS){raw=localStorage.getItem(k); if(raw)break;}} if(!raw)return; const data=JSON.parse(raw); const form=$('#newCaveForm'); if(!form)return; Object.entries(data).forEach(([k,v])=>{if(k==='missing_expected'&&Array.isArray(v)){v.forEach(x=>{const el=form.querySelector(`input[name="missing_expected"][value="${CSS.escape(x)}"]`); if(el)el.checked=true;}); return;} const el=form.elements[k]; if(el&&el.type!=='file'&&v!=null)el.value=v;}); status('Učitana je lokalna skica. Provjeri podatke pa predaj Arhivaru.','ok'); setDraftInfo('Učitana lokalna skica iz browsera.');}catch(e){console.warn(e);}}
function clearDraft(){try{localStorage.removeItem(DRAFT_KEY); LEGACY_KEYS.forEach(k=>localStorage.removeItem(k));}catch(e){}}
function downloadDraft(){const form=$('#newCaveForm'); if(!form)return; const data=formDataObj(form); const blob=new Blob([JSON.stringify(data,null,2)],{type:'application/json'}); const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download='sov-predaja-skica-'+(data.object_name||'nova-jama').replace(/[^a-z0-9_-]+/gi,'_')+'.json'; document.body.appendChild(a); a.click(); a.remove(); setTimeout(()=>URL.revokeObjectURL(a.href),800); status('Skica preuzeta kao JSON datoteka.','ok');}
async function uploadFile(client,submissionId,type,file,index,total){const safeName=file.name.replace(/[^a-zA-Z0-9._-]+/g,'_').slice(-150); const path=`${submissionId}/${type}/${Date.now()}_${safeName}`; progress(index,total,`Upload ${index}/${total}: ${file.name}`); const up=await client.storage.from(BUCKET).upload(path,file,{upsert:false,contentType:file.type||'application/octet-stream'}); if(up.error)throw up.error; const pub=client.storage.from(BUCKET).getPublicUrl(path); const row={submission_id:submissionId,file_type:type,file_name:file.name,mime_type:file.type||'',size_bytes:file.size||0,storage_bucket:BUCKET,storage_path:path,public_url:pub&&pub.data?pub.data.publicUrl:null,metadata:{source:'predaj-novu-jamu.html',build:BUILD,uploaded_at:new Date().toISOString()}}; const ins=await client.from('speleo_object_submission_files').insert(row); if(ins.error)throw ins.error; return row;}
function setInvalid(el,on=true){
  if(!el)return;
  el.classList.toggle('sf-invalid',!!on);
  if(on)el.setAttribute('aria-invalid','true'); else el.removeAttribute('aria-invalid');
}
function clearValidationMarks(form){form.querySelectorAll('.sf-invalid').forEach(el=>setInvalid(el,false));}
function firstControlForField(form,name){return form.elements[name] || form.querySelector(`[name="${CSS.escape(name)}"]`);}
function checkCoordinateWarning(record){
  const lat=record.lat, lon=record.lon;
  if(lat==null&&lon==null)return '';
  if((lat==null)!=(lon==null))return 'Unesi i lat i lon, ili ostavi oba prazna i priloži KML/GPX.';
  if(lat>=HR_BBOX.lonMin&&lat<=HR_BBOX.lonMax&&lon>=HR_BBOX.latMin&&lon<=HR_BBOX.latMax){return 'Koordinate izgledaju zamijenjeno: lat izgleda kao longitude, a lon kao latitude.';}
  if(lat<HR_BBOX.latMin||lat>HR_BBOX.latMax||lon<HR_BBOX.lonMin||lon>HR_BBOX.lonMax){return 'WGS84 koordinate su izvan očekivanog raspona za Hrvatsku (lat 42–47, lon 13–20). Provjeri upis.';}
  return '';
}
function validateDetailed(form,record,files){
  clearValidationMarks(form);
  const missing=[]; const invalid=[];
  if(!record.object_name)missing.push({name:'object_name',label:'Naziv'});
  if(!record.access_description)missing.push({name:'access_description',label:'Pristup'});
  if(!record.technical_description)missing.push({name:'technical_description',label:'Opis'});
  if(!form.elements.confirm_accuracy.checked)missing.push({name:'confirm_accuracy',label:'potvrda'});
  const hasCoords=(record.lat!=null&&record.lon!=null)||(record.htrs_x&&record.htrs_y);
  if(!hasCoords&&!files.some(f=>['kml','gpx'].includes(f.type)))missing.push({name:'lat',label:'koordinate ili KML/GPX'});
  const coordWarn=checkCoordinateWarning(record);
  const first=missing[0] ? firstControlForField(form,missing[0].name) : null;
  missing.forEach(m=>setInvalid(firstControlForField(form,m.name),true));
  if(missing.length){
    const onlyConsent = missing.length===1 && missing[0].name==='confirm_accuracy';
    return {ok:false, first, message: onlyConsent ? 'Označi potvrdu prije predaje.' : 'Fali: '+missing.map(m=>m.label).join(', ')+'.'};
  }
  return {ok:true, warning:coordWarn};
}
function focusValidationError(first,message){
  status(message,'bad'); toast(message,'bad');
  if(first){
    const target=first.closest('.sf-panel')||first.closest('.sf-field')||first;
    target.scrollIntoView({behavior:'smooth',block:'center'});
    setTimeout(()=>{try{first.focus({preventScroll:true});}catch(e){}},350);
  }
}
function validate(record, files){
  // Fallback for old callers; rich validation happens in submit().
  if(!record.object_name)return 'Naziv objekta je obavezan.';
  if(!record.access_description)return 'Pristup je obavezan.';
  if(!record.technical_description)return 'Opis je obavezan.';
  const hasCoords=(record.lat!=null&&record.lon!=null)||(record.htrs_x&&record.htrs_y);
  if(!hasCoords&&!files.some(f=>['kml','gpx'].includes(f.type))) return 'Unesi koordinate ili priloži KML/GPX.';
  return null;
}
function successBanner(id){
  const box=$('#successBox');
  if(!box)return;
  box.classList.add('on');
  box.style.display='block';
  box.innerHTML=`<b>Predano Arhivaru.</b><br>Predaja je spremljena u Arhivarski inbox. Objekt još nije u javnoj bazi dok ga Arhivar ne odobri.<br>ID predaje: <code>${esc(id)}</code> · <a href="arhivar-predane-jame.html">Otvori Arhivarski inbox</a>`;
}
async function submit(ev){
  ev.preventDefault();
  const form=ev.currentTarget; form.noValidate=true;
  const client=sb(); if(!client){status('Supabase nije konfiguriran. Ne mogu spremiti predaju.','bad');return;}
  const record=formDataObj(form); const files=collectFiles(form);
  const detail=validateDetailed(form,record,files);
  if(!detail.ok){focusValidationError(detail.first,detail.message);return;}
  if(detail.warning){status(detail.warning+' Predaju možeš nastaviti ako si siguran.','warn'); toast(detail.warning,'warn');}
  setBusy(true); status('Spremam zapisnik u Arhivarski inbox...'); progress(0,Math.max(files.length,1),'Priprema predaje...');
  try{
    const user=await currentUser();
    const payload={record_json:record,object_name:record.object_name,object_type:record.object_type,county:record.county,municipality:record.municipality,nearest_place:record.nearest_place,locality:record.locality,lat:record.lat,lon:record.lon,htrs_x:record.htrs_x,htrs_y:record.htrs_y,depth_m:record.depth_m,length_m:record.length_m,survey_date:record.survey_date||null,team:record.team,access_description:record.access_description,technical_description:record.technical_description,research_history:record.research_history,notes:record.notes,status:'submitted',submitted_by:user&&user.id||null,submitter_email:user&&user.email||null,submitter_name:user&&user.full_name||null,source:'predaj-novu-jamu.html',metadata:{form_version:BUILD,submitter_phone:record.submitter_phone,coordinate_source:record.coordinate_source,missing_expected:record.missing_expected,client_time:new Date().toISOString()}};
    const res=await client.from('speleo_object_submissions').insert(payload).select('id').single(); if(res.error)throw res.error;
    const id=res.data.id;
    let done=0; for(const f of files){await uploadFile(client,id,f.type,f.file,done+1,files.length); done++;}
    progress(files.length,files.length,files.length?'Privitci su uploadani.':'Predaja bez privitaka.');
    clearDraft(); form.reset(); renderAllFileLists();
    status(`Predano Arhivaru. ID predaje: ${id}\nObjekt još nije u javnoj bazi dok ga Arhivar ne odobri.`,'ok');
    successBanner(id);
    setDraftInfo('Nema aktivne skice. Zadnja predaja je spremljena.');
    toast('Predaja je spremljena u Arhivarski inbox.','ok');
  }catch(e){console.error(e); status('Greška spremanja: '+(e.message||e),'bad'); toast('Greška: '+(e.message||e),'bad');}
  finally{setBusy(false);}
}
function formatBytes(n){if(!Number.isFinite(n))return ''; const u=['B','KB','MB','GB']; let i=0, v=n; while(v>=1024&&i<u.length-1){v/=1024;i++;} return `${v.toFixed(v>=10||i===0?0:1)} ${u[i]}`;}
function fileExt(name){const m=String(name||'').toLowerCase().match(/\.([a-z0-9]+)$/); return m?m[1]:'';}
function setInputFiles(input,files){const dt=new DataTransfer(); files.forEach(f=>dt.items.add(f)); input.files=dt.files;}
function validateFileInput(input,{announce=true}={}){
  const rule=FILE_RULES[input.name]||{};
  const original=Array.from(input.files||[]);
  const kept=[]; const problems=[];
  original.forEach(file=>{
    const ext=fileExt(file.name);
    if(rule.ext && !rule.ext.includes(ext)){problems.push(`${file.name}: krivi tip za ${rule.label}`); return;}
    if(rule.max && file.size>rule.max){problems.push(`${file.name}: velika datoteka (${formatBytes(file.size)}), preporuka max ${formatBytes(rule.max)}`);}
    kept.push(file);
  });
  if(kept.length!==original.length)setInputFiles(input,kept);
  if(problems.length&&announce){status(problems.join('\n'),'warn'); toast('Provjeri odabrane datoteke.','warn');}
  renderFileList(input,problems);
}
function renderFileList(input,problems=[]){
  const box=input.closest('.sf-filebox'); if(!box)return;
  let list=box.querySelector('.sf-file-list');
  if(!list){list=document.createElement('div'); list.className='sf-file-list'; box.appendChild(list);}
  const files=Array.from(input.files||[]);
  if(!files.length && !problems.length){list.innerHTML='<small>Nema odabranih datoteka.</small>'; return;}
  const rows=files.map((file,i)=>`<div class="sf-file-row"><span><b>${esc(file.name)}</b><small>${formatBytes(file.size)}</small></span><button type="button" class="sf-file-remove" data-file-field="${esc(input.name)}" data-file-index="${i}" aria-label="Ukloni ${esc(file.name)}">Ukloni</button></div>`).join('');
  const warns=problems.length?`<div class="sf-file-warnings">${problems.map(esc).join('<br>')}</div>`:'';
  list.innerHTML=rows+warns;
}
function renderAllFileLists(){$$('input[type="file"]').forEach(input=>renderFileList(input));}
function initFileUX(){
  $$('input[type="file"]').forEach(input=>{validateFileInput(input,{announce:false}); input.addEventListener('change',()=>validateFileInput(input));});
  document.addEventListener('click',ev=>{
    const btn=ev.target.closest('.sf-file-remove'); if(!btn)return;
    const input=document.querySelector(`input[type="file"][name="${CSS.escape(btn.dataset.fileField)}"]`); if(!input)return;
    const idx=Number(btn.dataset.fileIndex);
    const files=Array.from(input.files||[]).filter((_,i)=>i!==idx);
    setInputFiles(input,files); renderFileList(input); autoSave();
  });
}
function initCoordUX(){
  const form=$('#newCaveForm'); if(!form)return;
  const lat=form.elements.lat, lon=form.elements.lon;
  function check(){const record=formDataObj(form); const warn=checkCoordinateWarning(record); [lat,lon].forEach(el=>el&&el.classList.toggle('sf-warn-field',!!warn)); let box=$('#coordWarning'); if(!box){box=document.createElement('div'); box.id='coordWarning'; box.className='sf-inline-warning'; lon?.closest('.sf-form-grid')?.appendChild(box);} if(box){box.textContent=warn; box.style.display=warn?'block':'none';}}
  lat?.addEventListener('input',check); lon?.addEventListener('input',check); check();
}
function initScrollSpy(){
  const links=$$('.sf-progress a[href^="#"]');
  if(!links.length)return;
  const map=new Map(links.map(a=>[a.getAttribute('href').slice(1),a]));
  links.forEach(a=>a.addEventListener('click',ev=>{const id=a.getAttribute('href').slice(1); const sec=document.getElementById(id); if(sec){ev.preventDefault(); sec.scrollIntoView({behavior:'smooth',block:'start'}); history.replaceState(null,'','#'+id);}}));
  const io=new IntersectionObserver(entries=>{entries.forEach(entry=>{if(!entry.isIntersecting)return; links.forEach(a=>a.classList.remove('active')); const a=map.get(entry.target.id); if(a)a.classList.add('active');});},{rootMargin:'-28% 0px -62% 0px',threshold:0.01});
  map.forEach((_,id)=>{const sec=document.getElementById(id); if(sec)io.observe(sec);});
}

function initProgressiveDisclosure(){
  const details=$('#optionalDetails');
  const more=$('#moreDetailsBtn');
  function setOpen(open){
    if(!details||!more)return;
    details.classList.toggle('sf-collapsed',!open);
    more.setAttribute('aria-expanded',open?'true':'false');
    more.innerHTML=open?'Sakrij dodatne detalje <span>(nije obavezno)</span>':'Dodaj više detalja <span>(nije obavezno)</span>';
    if(open) setTimeout(()=>details.scrollIntoView({behavior:'smooth',block:'start'}),60);
  }
  more?.addEventListener('click',()=>setOpen(details?.classList.contains('sf-collapsed')));
  $$('[data-toggle-target]').forEach(btn=>{
    const target=document.getElementById(btn.dataset.toggleTarget);
    btn.addEventListener('click',()=>{
      if(!target)return;
      const open=target.classList.toggle('sf-collapsed')===false;
      btn.classList.toggle('on',open);
    });
  });
}

function initButtons(){
  const d=$('#downloadDraftBtn'); if(d)d.textContent='Preuzmi skicu JSON';
  const s=$('#saveDraftBtn'); if(s)s.textContent='Spremi skicu';
  const c=$('#clearDraftBtn'); if(c)c.textContent='Obriši skicu';
}
function initStepHints(){const form=$('#newCaveForm'); if(!form)return; form.noValidate=true; form.addEventListener('input',autoSave); form.addEventListener('change',autoSave); form.addEventListener('input',ev=>{if(ev.target.matches('input,textarea,select'))setInvalid(ev.target,false);});}
async function boot(){
  if(window.SOVAuth&&SOVAuth.requireApproved){const ok=await SOVAuth.requireApproved(); if(!ok)return;}
  if(window.SOVAuth&&SOVAuth.renderUserBadge) await SOVAuth.renderUserBadge();
  initButtons(); initProgressiveDisclosure(); initStepHints(); initFileUX(); initCoordUX(); initScrollSpy();
  $('#newCaveForm')?.addEventListener('submit',submit);
  $('#saveDraftBtn')?.addEventListener('click',()=>saveDraft(false));
  $('#clearDraftBtn')?.addEventListener('click',()=>{if(!confirm('Obrisati lokalnu skicu iz ovog browsera?'))return; clearDraft(); $('#newCaveForm')?.reset(); renderAllFileLists(); status('Lokalna skica obrisana.','ok'); setDraftInfo('Skica obrisana.');});
  $('#downloadDraftBtn')?.addEventListener('click',downloadDraft);
  loadDraft();
}
document.addEventListener('DOMContentLoaded',boot);
})();
