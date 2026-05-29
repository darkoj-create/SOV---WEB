(function(){
const BUILD='5.57.8';
const state={items:[],filtered:[],selected:null,tab:'status',profile:null,loadingDetailId:null};
const $=s=>document.querySelector(s);
function esc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
function toast(msg){let el=document.querySelector('.aw-toast'); if(!el){el=document.createElement('div');el.className='aw-toast';document.body.appendChild(el);} el.textContent=msg; clearTimeout(el._t); el._t=setTimeout(()=>el.remove(),4200);}
function sb(){return window.SOVAuth&&SOVAuth.getClient&&SOVAuth.getClient();}
function norm(s){return String(s||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'').replace(/[čć]/g,'c').replace(/š/g,'s').replace(/ž/g,'z').replace(/đ/g,'d');}
function statusBadge(v, okLabel, missLabel){return `<span class="aw-badge ${v?'ok':'miss'}">${v?'✓ '+okLabel:'Fali '+missLabel}</span>`}
function workflowRawText(it){return [it.missing_categories_text,it.source_missing_text,it.field_tasks,it.workflow_raw,it.digital_survey_status,it.bibliography_status,it.gps_tracklog,it.georef_record,it.last_note].filter(Boolean).join(' | ');}
function hasToken(it,rx){return rx.test(norm(workflowRawText(it)));}
function boolVal(v){return v===true || v==='true' || v===1 || v==='1';}
function workflowChecks(it){
  const hasPlate = (it.has_plate!==undefined && it.has_plate!==null) ? boolVal(it.has_plate) : (it.missing_plate ? false : true);
  const photoToken = hasToken(it,/(^|[^a-z0-9])(fotka|foto|slika|fotograf)([^a-z0-9]|$)/);
  const redrawToken = hasToken(it,/(ponoviti|ponovno|los|loš|necitljiv|nečitljiv|stari).{0,50}(nacrt|tlocrt|profil|crtez|crtež)|ponoviti_nacrt|nacrt_ponoviti/);
  const hasPhoto = (it.has_photo!==undefined && it.has_photo!==null) ? boolVal(it.has_photo) : !photoToken;
  const redrawOk = (it.needs_redraw!==undefined && it.needs_redraw!==null) ? !boolVal(it.needs_redraw) : !redrawToken;
  return {
    plate: hasPlate,
    coordinates: !!it.has_coordinates,
    drawing: !!it.has_drawing,
    record: !!it.has_record,
    photo: hasPhoto,
    redrawOk: redrawOk,
    photoToken,
    redrawToken
  };
}
function missingText(it){const fromText=String(it.missing_categories_text||'').trim(); const arr=[]; if(fromText)arr.push(...fromText.split(/[,;]+/).map(x=>x.trim()).filter(Boolean)); if(Array.isArray(it.missing_categories))arr.push(...it.missing_categories.filter(Boolean)); const c=workflowChecks(it); if(!c.plate)arr.push('pločica'); if(!c.coordinates)arr.push('koordinate'); if(!c.drawing)arr.push('nacrt'); if(!c.record)arr.push('zapisnik'); if(!c.photo)arr.push('fotka'); if(!c.redrawOk)arr.push('ponoviti nacrt'); return [...new Set(arr)].join(', ');}
function baseMissingBadges(it){const c=workflowChecks(it); const parts=[['plate','pločica'],['coordinates','koordinate'],['drawing','nacrt'],['record','zapisnik'],['photo','fotka'],['redrawOk','ponoviti nacrt']].filter(([k])=>!c[k]); if(!parts.length)return '<span class="aw-badge ok">✓ Baza ne navodi falinke</span>'; return parts.map(([,l])=>`<span class="aw-badge miss">Fali ${esc(l)}</span>`).join('');}
function readinessLabel(it){const r=it.katastar_readiness||'';if(r==='u_katastru')return 'Objekt je u katastru';if(r==='spremno_za_katastar')return 'Baza kaže: spremno za unos';if(r==='nije_u_katastru_provjeriti')return 'Nije u katastru · provjeriti što fali';if(r==='nepotpuno')return 'Nepotpuno za katastar';return 'Provjeriti status';}

function fmtBlock(text){
  const clean=String(text||'').trim();
  if(!clean)return '<div class="aw-empty small">Nema dodatnog teksta u bazi.</div>';
  return `<div class="aw-text-block">${esc(clean).replace(/\n{2,}/g,'</p><p>').replace(/\n/g,'<br>')}</div>`;
}
function stripTechnicalDump(text){
  let clean=String(text||'').trim();
  if(!clean)return '';
  clean=clean.split('--- SVA SQL POLJA')[0].split('--- RAW IMPORT JSON')[0].trim();
  return clean;
}
function parseLabelText(text){
  const out={}; let current='';
  stripTechnicalDump(text).split(/\r?\n/).forEach(line=>{
    const raw=String(line||'').trim();
    if(!raw)return;
    const m=raw.match(/^([^:]{2,70}):\s*(.*)$/);
    if(m){current=m[1].trim(); const value=(m[2]||'').trim(); if(value)out[current]=out[current]?out[current]+'\n'+value:value;}
    else if(current){out[current]=out[current]?out[current]+'\n'+raw:raw;}
  });
  return out;
}
function val(map){
  for(const key of arguments){const t=String(map[key]||'').trim(); if(t && t!=='null' && t!=='undefined' && t!=='{}')return t;}
  return '';
}
function kvHuman(label,value){
  const clean=String(value||'').trim(); if(!clean)return '';
  return `<div class="aw-human-item"><span>${esc(label)}</span><b>${esc(clean).replace(/\n/g,'<br>')}</b></div>`;
}
function textHuman(label,value){
  const clean=String(value||'').trim(); if(!clean)return '';
  return `<div class="aw-human-text"><h4>${esc(label)}</h4>${fmtBlock(clean)}</div>`;
}
function humanSection(title,html,open=true){
  const clean=String(html||'').trim(); if(!clean)return '';
  return `<details class="aw-section aw-human-section" ${open?'open':''}><summary>${esc(title)}</summary><div class="aw-human-body">${clean}</div></details>`;
}
function reportSection(title,text,open=false){
  const clean=String(text||'').trim(); if(!clean)return '';
  return `<details class="aw-section aw-human-section" ${open?'open':''}><summary>${esc(title)}</summary><div class="aw-human-body">${fmtBlock(clean)}</div></details>`;
}
function detailErrorBlock(it){
  if(!it || !it.detail_error)return '';
  return `<div class="aw-alert"><b>Detail RPC nije vratio puni objekt.</b><br>${esc(it.detail_error)}<br><small>Lista radi, ali za puni opis mora biti pokrenut najnoviji SQL v${BUILD}. Ovaj ekran se više ne smije zalediti na učitavanju.</small></div>`;
}
function firstNonEmpty(){for(const v of arguments){const t=String(v||'').trim(); if(t && t!=='{}')return t;} return '';}
function hasCoords(it){return Number.isFinite(Number(it&&it.lat)) && Number.isFinite(Number(it&&it.lon));}
function tk25Url(it){const lat=Number(it.lat),lon=Number(it.lon);const params=new URLSearchParams({lat:String(lat),lon:String(lon),zoom:'15',base:'tk25',name:it.object_name||'Objekt'}); if(it.plate_number)params.set('plate',it.plate_number); return 'baza.html?'+params.toString();}
function tk25Button(it){return hasCoords(it)?`<a class="aw-btn primary" href="${esc(tk25Url(it))}" target="_blank" rel="noopener">Otvori na TK25 karti</a>`:'<button class="aw-btn" disabled title="Nema koordinata">Nema koordinata za TK25</button>';}

async function init(){
  const vb=document.getElementById('awVersionBadge'); if(vb)vb.textContent='Arhivar HTML v'+BUILD+' · normalni opis + workflow checkovi + TK25';
  if(window.SOVAuth&&SOVAuth.requireArchive){const ok=await SOVAuth.requireArchive(); if(!ok)return;}
  try{state.profile=await SOVAuth.getProfile();}catch(e){}
  $('#refreshBtn').onclick=load;
  $('#newObjectBtn').onclick=()=>newObjectForm();
  $('#searchBox').addEventListener('input',applyFilters);
  $('#filterBox').addEventListener('change',applyFilters);
  await load();
}
async function load(){
  const client=sb(); if(!client){toast('Supabase nije konfiguriran.');return;}
  $('#objectList').innerHTML='<div class="aw-empty">Učitavam brzi arhivarski popis...</div>';
  try{
    const dash=await client.from('sov_arhivar_dashboard').select('*').maybeSingle();
    renderStats(dash.data||{});
  }catch(e){renderStats({});}
  const {data,error}=await client.from('sov_arhivar_worklist').select('*').order('priority_score',{ascending:false}).order('object_name',{ascending:true}).limit(1500);
  if(error){$('#objectList').innerHTML=`<div class="aw-empty">Greška učitavanja: ${esc(error.message)}</div>`;return;}
  state.items=data||[]; applyFilters();
  if(!state.selected && state.filtered[0]) selectObject(state.filtered[0].object_id);
  toast(`Učitano ${state.items.length} objekata.`);
}
function renderStats(d){
  const rows=[['Ukupno',d.total_objects||state.items.length||0],['U katastru',d.in_katastar||0],['Fale koordinate',d.missing_coordinates||0],['Fali nacrt',d.missing_drawings||0],['Fali zapisnik',d.missing_records||0],['Fali pločica',d.missing_plates||0]];
  if(d.missing_photos!==undefined)rows.push(['Fali fotka',d.missing_photos||0]);
  if(d.needs_redraw_count!==undefined)rows.push(['Ponoviti nacrt',d.needs_redraw_count||0]);
  rows.push(['Spremno za unos',d.ready_for_katastar||0]);
  $('#stats').innerHTML=rows.map(([l,v])=>`<div class="aw-stat"><b>${esc(v)}</b><span>${esc(l)}</span></div>`).join('');
}
function applyFilters(){
  const q=norm($('#searchBox').value); const f=$('#filterBox').value;
  state.filtered=state.items.filter(it=>{
    const hay=norm([it.object_name,it.plate_number,it.nearest_place,it.object_type,it.search_text].join(' '));
    if(q && !hay.includes(q))return false;
    if(f==='missing')return it.missing_coordinates||it.missing_drawing||it.missing_record||it.missing_plate;
    if(f==='missing_drawing')return !!it.missing_drawing;
    if(f==='missing_coordinates')return !!it.missing_coordinates;
    if(f==='missing_record')return !!it.missing_record;
    if(f==='missing_plate')return !!it.missing_plate;
    if(f==='ready')return it.katastar_readiness==='spremno_za_katastar';
    return true;
  });
  renderList();
}
function renderList(){
  const box=$('#objectList');
  if(!state.filtered.length){box.innerHTML='<div class="aw-empty">Nema rezultata za filter.</div>';return;}
  box.innerHTML=state.filtered.map(it=>`<div class="aw-object ${state.selected&&state.selected.object_id===it.object_id?'active':''}" data-id="${esc(it.object_id)}"><b>${esc(it.object_name||'Bez naziva')}</b><small>${esc([it.object_type,it.plate_number?'pločica '+it.plate_number:'',it.nearest_place].filter(Boolean).join(' · ')||'—')}</small><div class="aw-badges">${baseMissingBadges(it)}</div>${missingText(it)?`<small><b>Baza kaže da fali:</b> ${esc(missingText(it))}</small>`:''}<small>${esc(readinessLabel(it))}</small></div>`).join('');
  box.querySelectorAll('.aw-object').forEach(el=>el.onclick=()=>selectObject(el.dataset.id));
}
async function selectObject(id){
  const base=state.items.find(x=>String(x.object_id)===String(id))||null;
  state.selected=base;
  renderList();
  renderDetail(true);
  if(!base)return;
  await loadObjectDetail(id);
}
async function loadObjectDetail(id){
  const client=sb(); if(!client)return;
  state.loadingDetailId=String(id);
  try{
    const {data,error}=await client.rpc('sov_arhivar_get_object_detail',{p_object_id:String(id)});
    if(error){
      const msg='Detalji nisu učitani: '+error.message;
      toast(msg);
      const idx=state.items.findIndex(x=>String(x.object_id)===String(id));
      if(idx>=0){state.items[idx]={...state.items[idx],details_loaded:true,detail_error:error.message};state.selected=state.items[idx];}
      else state.selected={...(state.selected||{}),details_loaded:true,detail_error:error.message};
      renderDetail(false);
      return;
    }
    const detail=(data&&typeof data==='object')?data:{};
    const idx=state.items.findIndex(x=>String(x.object_id)===String(id));
    if(idx>=0){state.items[idx]={...state.items[idx],...detail,details_loaded:true,detail_error:null};state.selected=state.items[idx];}
    else state.selected={...(state.selected||{}),...detail,details_loaded:true,detail_error:null};
    renderList();
    renderDetail(false);
  }catch(e){
    const msg=(e&&e.message)||String(e);
    toast('Detalji nisu učitani: '+msg);
    const idx=state.items.findIndex(x=>String(x.object_id)===String(id));
    if(idx>=0){state.items[idx]={...state.items[idx],details_loaded:true,detail_error:msg};state.selected=state.items[idx];}
    else state.selected={...(state.selected||{}),details_loaded:true,detail_error:msg};
    renderDetail(false);
  }
  finally{state.loadingDetailId=null;}
}
function renderDetail(isLoading=false){
  const it=state.selected; if(!it){$('#detailPanel').innerHTML='<div class="aw-empty">Odaberi objekt.</div>';$('#actionPanel').innerHTML='';return;}
  if(isLoading && !it.details_loaded){
    $('#detailPanel').innerHTML=`<h2>${esc(it.object_name||'Objekt')}</h2><div class="aw-empty">Učitavam puni tekst za odabrani objekt...</div>`;
    $('#actionPanel').innerHTML='';
    return;
  }
  const labelMap=parseLabelText(it.base_details_text || it.full_details_text || '');
  const opisHtml=[
    textHuman('Opis objekta', val(labelMap,'Opis')),
    textHuman('Pristup', val(labelMap,'Pristup')),
    textHuman('Istraživanje / povijest', val(labelMap,'Istraživanje / povijest')),
    textHuman('Autori / ekipa', val(labelMap,'Autori / ekipa')),
    textHuman('Hidrologija', val(labelMap,'Hidrologija')),
    textHuman('Geologija / morfologija', val(labelMap,'Geologija / morfologija')),
    textHuman('Opasnosti / zaštita', val(labelMap,'Opasnosti / zaštita')),
    textHuman('Napomena', val(labelMap,'Napomena'))
  ].filter(Boolean).join('');
  const statusHtml=`<div class="aw-human-grid">${[
    kvHuman('Katastarski status', val(labelMap,'Katastarski status')||it.cadastre_status),
    kvHuman('Status zapisa', val(labelMap,'Status zapisa')||it.record_status),
    kvHuman('Zadaci / što fali', val(labelMap,'Zadaci / što fali')||it.field_tasks||missingText(it)),
    kvHuman('Workflow', val(labelMap,'Workflow')||it.workflow_raw),
    kvHuman('Digitalni nacrt', val(labelMap,'Digitalni nacrt')||it.digital_survey_status),
    kvHuman('Bibliografija / zapisnik', val(labelMap,'Bibliografija/zapisnik')||it.bibliography_status),
    kvHuman('GPS tracklog', val(labelMap,'GPS tracklog')||it.gps_tracklog),
    kvHuman('Georef zapis', val(labelMap,'Georef zapis')||it.georef_record)
  ].filter(Boolean).join('')}</div>`;
  const osnovnoHtml=`<div class="aw-human-grid">${[
    kvHuman('Naziv', val(labelMap,'Naziv')||it.object_name),
    kvHuman('Tip', val(labelMap,'Tip')||it.object_type),
    kvHuman('Pločica', val(labelMap,'Pločica')||it.plate_number||'—'),
    kvHuman('Najbliže mjesto', val(labelMap,'Najbliže mjesto')||it.nearest_place),
    kvHuman('Županija/regija', val(labelMap,'Županija/regija')||it.county),
    kvHuman('Općina', val(labelMap,'Općina')||it.municipality),
    kvHuman('Koordinate', it.lat&&it.lon?`${it.lat}, ${it.lon}`:''),
    kvHuman('Baza kaže da fali', missingText(it)||'—')
  ].filter(Boolean).join('')}</div>`;
  const fullDetails = [
    detailErrorBlock(it),
    humanSection('Osnovni podaci', osnovnoHtml, true),
    humanSection('Opis i sadržaj iz baze', opisHtml || '<div class="aw-empty small">Za ovaj objekt baza nema dodatni opisni tekst, samo statusna/tehnička polja.</div>', true),
    humanSection('Katastar i arhivarski status', statusHtml, true),
    reportSection('Zapisnici / istraživanja / tko je bio', it.report_details_text || '', false),
    reportSection('Predani nacrti i prilozi', it.drawing_details_text || '', false)
  ].filter(Boolean).join('') || '<div class="aw-empty small">Za ovaj objekt nema dodatnog teksta u bazi.</div>';
  $('#detailPanel').innerHTML=`<h2>${esc(it.object_name)}</h2><div class="aw-detail-version">HTML v${BUILD} · čitljiv opis iz SQL-a</div><div class="aw-kv"><span>ID</span><b>${esc(it.object_id)}</b><span>Broj pločice</span><b>${esc(it.plate_number||'—')}</b><span>Tip</span><b>${esc(it.object_type||'—')}</b><span>Mjesto</span><b>${esc(it.nearest_place||'—')}</b><span>Koordinate</span><b>${it.lat&&it.lon?esc(`${it.lat}, ${it.lon}`):'—'}</b><span>Nacrti u SOV arhivi</span><b>${esc(it.archive_drawing_count??it.drawing_count??0)}</b><span>Zapisnici u SOV arhivi</span><b>${esc(it.archive_report_count??it.report_count??0)}</b><span>Status iz baze</span><b>${esc(readinessLabel(it))}</b><span>Baza kaže da fali</span><b>${esc(missingText(it)||'—')}</b></div><div class="aw-object-actions">${tk25Button(it)}<button class="aw-btn" onclick="navigator.clipboard&&navigator.clipboard.writeText('${hasCoords(it)?esc(String(it.lat)+', '+String(it.lon)):''}')">Kopiraj koordinate</button></div><div class="aw-badges" style="margin-top:14px">${baseMissingBadges(it)}</div><p class="aw-muted">Prikaz je sada normalan arhivarski opis: osnovni podaci, opis/pristup/istraživanje i statusi. Sirova SQL staging polja više se ne guraju u desni panel.</p><div class="aw-full-details"><h3>Tekst i podaci iz baze</h3>${fullDetails}</div>`;
  renderActionPanel();
}
function renderActionPanel(){
  const it=state.selected; if(!it)return;
  const tabs=[['status','Status'],['drawing','Dodaj nacrt'],['report','Dodaj zapisnik'],['edit','Edit objekt']];
  $('#actionPanel').innerHTML=`<div class="aw-tabs">${tabs.map(([id,l])=>`<button class="aw-tab ${state.tab===id?'active':''}" data-tab="${id}">${l}</button>`).join('')}</div><div id="tabBody"></div>`;
  $('#actionPanel').querySelectorAll('[data-tab]').forEach(b=>b.onclick=()=>{state.tab=b.dataset.tab;renderActionPanel();});
  if(state.tab==='status')statusForm(it); else if(state.tab==='drawing')drawingForm(it); else if(state.tab==='report')reportForm(it); else editForm(it);
}
function statusForm(it){const c=workflowChecks(it); const extra=[['Digitalni nacrt',it.digital_survey_status],['Bibliografija / zapisnik',it.bibliography_status],['GPS tracklog',it.gps_tracklog],['Georef zapis',it.georef_record]].filter(([,v])=>String(v||'').trim()).map(([l,v])=>`<div class="aw-mini-kv"><span>${esc(l)}</span><b>${esc(v)}</b></div>`).join(''); $('#tabBody').innerHTML=`<div class="aw-forms"><h2>Što baza kaže da imamo za katastar?</h2><p class="aw-muted">Ovo je sada puni arhivarski checklist. Prva četiri polja su ključna za katastar, a fotka/ponoviti nacrt dolaze iz field_tasks/workflow_raw kad ih baza navodi.</p><div class="aw-muted"><b>Baza kaže da fali:</b> ${esc(missingText(it)||'ništa eksplicitno')}</div><div class="aw-badges">${baseMissingBadges(it)}</div><div class="aw-check-grid"><label class="aw-check ${c.plate?'ok':''}"><input id="hasPlate" type="checkbox" ${c.plate?'checked':''}> Pločica</label><label class="aw-check ${c.coordinates?'ok':''}"><input id="hasCoords" type="checkbox" ${c.coordinates?'checked':''}> Koordinate</label><label class="aw-check ${c.drawing?'ok':''}"><input id="hasDrawing" type="checkbox" ${c.drawing?'checked':''}> Nacrt</label><label class="aw-check ${c.record?'ok':''}"><input id="hasRecord" type="checkbox" ${c.record?'checked':''}> Zapisnik</label><label class="aw-check ${c.photo?'ok':''}"><input id="hasPhoto" type="checkbox" ${c.photo?'checked':''}> Fotka</label><label class="aw-check ${c.redrawOk?'ok':''}"><input id="redrawOk" type="checkbox" ${c.redrawOk?'checked':''}> Nacrt ne treba ponoviti</label></div><div class="aw-form-grid"><input id="plateInput" class="aw-input" placeholder="Broj pločice / katastarski broj" value="${esc(it.plate_number||'')}"><select id="priority" class="aw-select"><option value="normal">Normalno</option><option value="high">Prioritetno</option><option value="low">Niski prioritet</option></select></div>${extra?`<div class="aw-extra-status">${extra}</div>`:''}<textarea id="statusNote" class="aw-textarea" rows="3" placeholder="Napomena: što fali, tko ima nacrt, gdje je zapisnik...">${esc(it.last_note||'')}</textarea><button class="aw-btn primary" id="saveStatus">Spremi status</button></div>`;$('#priority').value=it.priority||'normal';$('#saveStatus').onclick=saveStatus;}
async function saveStatus(){const it=state.selected; const client=sb(); const plate=$('#plateInput').value.trim(); const hasPlate=$('#hasPlate').checked; const hasCoords=$('#hasCoords').checked; const hasDrawing=$('#hasDrawing').checked; const hasRecord=$('#hasRecord').checked; const hasPhoto=$('#hasPhoto').checked; const redrawOk=$('#redrawOk').checked; const ready=hasPlate&&hasCoords&&hasDrawing&&hasRecord&&hasPhoto&&redrawOk; const basePayload={p_object_id:it.object_id,p_object_name:it.object_name,p_plate_number:plate||it.plate_number||null,p_has_coordinates:hasCoords,p_has_drawing:hasDrawing,p_has_record:hasRecord,p_archive_status:ready?'ready':'needs_review',p_priority:$('#priority').value,p_note:$('#statusNote').value.trim()}; const v2={...basePayload,p_has_plate:hasPlate,p_has_photo:hasPhoto,p_needs_redraw:!redrawOk,p_extra_checks:{plate:hasPlate,coordinates:hasCoords,drawing:hasDrawing,record:hasRecord,photo:hasPhoto,redraw_ok:redrawOk,source:'arhivar_html_'+BUILD}}; let res=await client.rpc('sov_archive_update_object_status_v2',v2); if(res.error){console.warn('v2 status RPC failed, fallback old RPC',res.error); res=await client.rpc('sov_archive_update_object_status',basePayload);} if(res.error)return toast('Greška: '+res.error.message); if(plate && plate!==String(it.plate_number||'')){try{await client.from('speleo_object_overrides').upsert({object_id:it.object_id,data:{plate_number:plate,cadastral_number:plate},updated_at:new Date().toISOString()},{onConflict:'object_id'});}catch(e){console.warn('plate override skipped',e)}} toast('Status arhive spremljen.');await load();selectObject(it.object_id);}
function drawingForm(it){$('#tabBody').innerHTML=`<div class="aw-forms"><h2>Dodaj predani nacrt</h2><input id="drTitle" class="aw-input" placeholder="Naziv nacrta" value="${esc(it.object_name||'Nacrt')}"><input id="drUrl" class="aw-input" placeholder="Drive/Supabase/file URL"><div class="aw-form-grid"><input id="drAuthor" class="aw-input" placeholder="Autor"><input id="drYear" class="aw-input" placeholder="Godina"></div><textarea id="drNote" class="aw-textarea" rows="3" placeholder="Napomena"></textarea><button class="aw-btn primary" id="saveDrawing">Spremi nacrt</button></div>`;$('#saveDrawing').onclick=saveDrawing;}
async function saveDrawing(){const it=state.selected,client=sb();const row={object_id:it.object_id,object_name:it.object_name,plate_number:it.plate_number||null,drawing_title:$('#drTitle').value.trim()||it.object_name,drawing_type:'nacrt',archive_status:'verified',drive_url:$('#drUrl').value.trim()||null,preview_url:$('#drUrl').value.trim()||null,source:'arhivar_web',author_name:$('#drAuthor').value.trim()||null,survey_year:parseInt($('#drYear').value,10)||null,match_status:'verified',public_visible:true,note:$('#drNote').value.trim()||null,metadata:{module:'arhivar',added_from:'web_5.57.8',note:'Upload u SOV arhivu; ne mijenja automatski bazni katastarski status osim kroz ručni RPC.'}};const {error}=await client.from('speleo_object_drawings').insert(row);if(error)return toast('Greška nacrta: '+error.message);await client.rpc('sov_archive_update_object_status',{p_object_id:it.object_id,p_object_name:it.object_name,p_plate_number:it.plate_number||null,p_has_coordinates:!!it.has_coordinates,p_has_drawing:true,p_has_record:!!it.has_record,p_archive_status:'needs_review',p_priority:it.priority||'normal',p_note:'Dodan nacrt: '+row.drawing_title});toast('Nacrt dodan.');await load();selectObject(it.object_id);}
function reportForm(it){$('#tabBody').innerHTML=`<div class="aw-forms"><h2>Dodaj zapisnik / izvještaj</h2><div class="aw-form-grid"><input id="rpStart" class="aw-input" type="date"><input id="rpEnd" class="aw-input" type="date"></div><textarea id="rpDesc" class="aw-textarea" rows="4" placeholder="Opis zapisnika / zahvata"></textarea><input id="rpMembers" class="aw-input" placeholder="Članovi"><textarea id="rpNote" class="aw-textarea" rows="3" placeholder="Interna napomena"></textarea><button class="aw-btn primary" id="saveReport">Spremi zapisnik</button></div>`;$('#saveReport').onclick=saveReport;}
async function saveReport(){const it=state.selected,client=sb();const row={object_name:it.object_name,plate_number:it.plate_number||null,object_type:it.object_type||null,nearest_place:it.nearest_place||null,coordinate_system:(it.lat&&it.lon)?'WGS84':null,x_coord:it.lon?String(it.lon):null,y_coord:it.lat?String(it.lat):null,date_start:$('#rpStart').value||null,date_end:$('#rpEnd').value||null,purpose:'Speleološka istraživanja',activity_description:$('#rpDesc').value.trim()||null,members:$('#rpMembers').value.trim()||null,note:$('#rpNote').value.trim()||null,raw:{module:'arhivar',object_id:it.object_id}};const {error}=await client.from('speleo_activity_reports').insert(row);if(error)return toast('Greška zapisnika: '+error.message);await client.rpc('sov_archive_update_object_status',{p_object_id:it.object_id,p_object_name:it.object_name,p_plate_number:it.plate_number||null,p_has_coordinates:!!it.has_coordinates,p_has_drawing:!!it.has_drawing,p_has_record:true,p_archive_status:'needs_review',p_priority:it.priority||'normal',p_note:'Dodan zapisnik.'});toast('Zapisnik dodan.');await load();selectObject(it.object_id);}
function editForm(it){$('#tabBody').innerHTML=`<div class="aw-forms"><h2>Edit objekta</h2><p class="aw-muted">Sigurni override preko arhivarskog sloja. Ne briše raw import.</p><div class="aw-form-grid"><input id="edName" class="aw-input" placeholder="Naziv" value="${esc(it.object_name||'')}"><input id="edPlate" class="aw-input" placeholder="Pločica" value="${esc(it.plate_number||'')}"><input id="edType" class="aw-input" placeholder="Tip" value="${esc(it.object_type||'')}"><input id="edPlace" class="aw-input" placeholder="Najbliže mjesto" value="${esc(it.nearest_place||'')}"><input id="edLat" class="aw-input" placeholder="Lat" value="${esc(it.lat||'')}"><input id="edLon" class="aw-input" placeholder="Lon" value="${esc(it.lon||'')}"><textarea id="edNote" class="aw-textarea wide" rows="3" placeholder="Napomena promjene"></textarea></div><button class="aw-btn primary" id="saveEdit">Spremi edit</button></div>`;$('#saveEdit').onclick=saveEdit;}
async function saveEdit(){const it=state.selected,client=sb();const data={name:$('#edName').value.trim(),plate_number:$('#edPlate').value.trim(),object_type:$('#edType').value.trim(),nearest_place:$('#edPlace').value.trim(),lat:$('#edLat').value.trim(),lon:$('#edLon').value.trim()};let {error}=await client.from('speleo_object_overrides').upsert({object_id:it.object_id,data,updated_at:new Date().toISOString()},{onConflict:'object_id'});if(error)return toast('Greška edita: '+error.message);await client.from('speleo_object_edits').insert({object_id:it.object_id,changed_fields:Object.keys(data),new_values:data,note:$('#edNote').value.trim()||'Arhivar edit'});toast('Edit spremljen.');await load();selectObject(it.object_id);}
function newObjectForm(){state.selected=null;renderList();$('#detailPanel').innerHTML='<h2>Novi objekt</h2><p class="aw-muted">Dodaje se u speleo staging bazu i odmah dobiva arhivarski status.</p>';$('#actionPanel').innerHTML=`<div class="aw-forms"><input id="nwName" class="aw-input" placeholder="Naziv objekta"><div class="aw-form-grid"><input id="nwType" class="aw-input" placeholder="Tip objekta"><input id="nwPlate" class="aw-input" placeholder="Pločica"><input id="nwPlace" class="aw-input" placeholder="Najbliže mjesto"><input id="nwCounty" class="aw-input" placeholder="Županija/regija"><input id="nwLat" class="aw-input" placeholder="Lat"><input id="nwLon" class="aw-input" placeholder="Lon"></div><textarea id="nwNote" class="aw-textarea" rows="3" placeholder="Napomena"></textarea><button class="aw-btn primary" id="saveNew">Dodaj objekt</button></div>`;$('#saveNew').onclick=saveNewObject;}
async function saveNewObject(){const client=sb();const name=$('#nwName').value.trim(); if(!name)return toast('Unesi naziv objekta.');const id='manual_'+Date.now();const row={source_id:id,source_system:'arhivar_manual',name,object_type_final:$('#nwType').value.trim()||null,cadastral_number:$('#nwPlate').value.trim()||null,nearest_place:$('#nwPlace').value.trim()||null,county:$('#nwCounty').value.trim()||null,lat:parseFloat($('#nwLat').value)||null,lon:parseFloat($('#nwLon').value)||null,record_status:'arhivar_manual',cadastre_status:'needs_review',raw:{note:$('#nwNote').value.trim(),module:'arhivar'}};const {error}=await client.from('speleo_objects_staging').insert(row);if(error)return toast('Greška novog objekta: '+error.message);await client.rpc('sov_archive_update_object_status',{p_object_id:id,p_object_name:name,p_plate_number:row.cadastral_number,p_has_coordinates:!!(row.lat&&row.lon),p_has_drawing:false,p_has_record:false,p_archive_status:'needs_review',p_priority:'high',p_note:$('#nwNote').value.trim()||'Novi objekt dodan kroz Arhivar'});toast('Novi objekt dodan.');await load();selectObject(id);}
document.addEventListener('DOMContentLoaded',init);
})();
