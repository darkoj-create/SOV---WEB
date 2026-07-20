(function(){
  'use strict';
  const BUILD='6.1.45ax-system-status-v3';
  const state={checks:[],snapshot:null};
  const $=(sel,root=document)=>root.querySelector(sel);
  const fmt=new Intl.NumberFormat('hr-HR');
  const esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const safeNumber=v=>{if(v===null||v===undefined||v==='')return null;const n=Number(v);return Number.isFinite(n)?n:null};
  const ms=n=>`${Math.round(n)} ms`;
  const fmtDate=v=>{if(!v)return'—';const d=new Date(v);return Number.isNaN(d.getTime())?'—':d.toLocaleString('hr-HR')};
  function log(line){const el=$('#sov-status-log');if(el){el.textContent+=`[${new Date().toLocaleTimeString('hr-HR')}] ${line}\n`;el.scrollTop=el.scrollHeight}}
  function statusClass(s){return s==='ok'?'ok':s==='bad'?'bad':'warn'}
  function setText(id,val){const el=$(id);if(el){el.textContent=val;el.classList.remove('sov-skeleton')}}
  function setChip(id,label,status){const el=$(id);if(!el)return;el.className=`sov-status-chip ${statusClass(status)}`;el.textContent=label}
  function markScore(status,title,subtitle){const dot=$('#overall-dot');if(dot)dot.className=`sov-score-dot ${statusClass(status)}`;setText('#overall-title',title);setText('#overall-subtitle',subtitle);setChip('#overall-chip',title,status)}
  async function timeout(promise,limitMs,label){let timer;const killer=new Promise((_,reject)=>{timer=setTimeout(()=>reject(new Error(`${label}: nema odgovora ${limitMs} ms`)),limitMs)});try{return await Promise.race([promise,killer])}finally{clearTimeout(timer)}}
  async function measure(name,fn,critical=false){const started=performance.now();try{const data=await timeout(Promise.resolve().then(fn),9000,name);const elapsed=performance.now()-started;state.checks.push({name,status:'ok',ms:elapsed,critical});log(`RADI ${name} (${ms(elapsed)})`);return{ok:true,data,ms:elapsed}}catch(error){const elapsed=performance.now()-started;state.checks.push({name,status:critical?'bad':'warn',ms:elapsed,critical,error:String(error&&error.message||error)});log(`${critical?'NE RADI':'POZOR'} ${name}: ${error&&error.message?error.message:error}`);return{ok:false,error,ms:elapsed}}}
  function renderChecks(){const tbody=$('#checks-body');if(!tbody)return;tbody.innerHTML=state.checks.map(c=>`<tr><td><b>${esc(c.name)}</b>${c.error?`<div class="sov-muted">${esc(c.error)}</div>`:''}</td><td>${c.critical?'Da':'Ne'}</td><td><span class="sov-status-chip ${statusClass(c.status)}">${c.status==='ok'?'Radi':c.status==='bad'?'Ne radi':'Pozor'}</span></td><td>${ms(c.ms)}</td></tr>`).join('')}
  async function countRows(sb,table,label,critical=false){return measure(label||table,async()=>{const{count,error}=await sb.from(table).select('*',{count:'exact',head:true});if(error)throw error;return count||0},critical)}
  async function selectOne(sb,table,label,critical=false){return measure(label||table,async()=>{const{data,error}=await sb.from(table).select('*').limit(1).maybeSingle();if(error)throw error;return data||null},critical)}
  function severityStatus(row){if(row.severity==='fatal'&&row.handled===false)return'bad';if(row.severity==='error'||row.handled===false)return'warn';return'ok'}
  function eventType(row){if(row.platform==='android'&&row.severity==='fatal'&&row.handled===false)return'potvrđen fatalni događaj';if(row.handled===false)return'neobrađena greška';if(row.severity==='warning')return'obrađeno upozorenje';return'obrađena greška'}
  function renderCrashTable(rows){const tbody=$('#crash-body');if(!tbody)return;const list=Array.isArray(rows)?rows:[];if(!list.length){tbody.innerHTML='<tr><td colspan="5" class="sov-muted">Nema prijavljenih događaja u odabranom razdoblju.</td></tr>';return}tbody.innerHTML=list.map(row=>{const device=row.device||{};const deviceLine=[device.manufacturer,device.model,device.android?`Android ${device.android}`:''].filter(Boolean).join(' · ');const context=[row.screen,row.action].filter(Boolean).join(' / ')||'—';const exception=[row.exception_class,row.fingerprint?`#${String(row.fingerprint).slice(0,16)}`:''].filter(Boolean).join(' · ');return`<tr><td>${esc(fmtDate(row.created_at))}</td><td><span class="sov-platform ${esc(row.platform||'unknown')}">${esc(row.platform||'unknown')}</span><span class="sov-crash-meta">${esc(row.app_version||'—')}</span></td><td>${esc(context)}${deviceLine?`<span class="sov-crash-meta">${esc(deviceLine)}</span>`:''}</td><td><span class="sov-status-chip ${statusClass(severityStatus(row))}">${esc(eventType(row))}</span></td><td class="sov-crash-message">${esc(row.message||'Bez poruke')}${exception?`<span class="sov-crash-meta">${esc(exception)}</span>`:''}</td></tr>`}).join('')}
  function renderSnapshot(profile,localManifest,live){
    const snapshot=live.snapshot||{};
    const manifest=snapshot.manifest||live.manifest||localManifest||{};
    const catalogManifest=live.catalogManifest||{};
    const armory=snapshot.armory||{};
    const security=snapshot.security||{};
    const errors=snapshot.client_errors||{};
    const gmail=snapshot.gmail_sync||{};
    const armoryCatalog=safeNumber(armory.catalog??live.catalogCount?.data??catalogManifest.grouped_row_count);
    const requests=safeNumber(armory.requests??live.requestsCount?.data);
    const pending=safeNumber(armory.pending_requests??live.pendingRequests?.data);
    const loans=safeNumber(armory.loans??live.loansCount?.data);
    const noRls=safeNumber(security.tables_without_rls??live.tablesWithoutRls?.data);
    const publicPolicies=safeNumber(security.public_write_policies??live.openPolicies?.data);
    const hardening=safeNumber(security.hardening_log_rows??live.hardeningRows?.data);
    const currentIncidents=safeNumber(errors.current_incidents_30m??errors.actionable_current??0)??0;
    const androidWarnings24=safeNumber(errors.android_warning_24h??errors.android_error_24h)??0;
    const androidFatal24=safeNumber(errors.android_fatal_24h)??0;
    const androidFatal7=safeNumber(errors.android_unhandled_7d)??0;
    const webUnhandled24=safeNumber(errors.web_unhandled_24h??errors.web_fatal_24h)??0;
    setText('#user-line',profile?`${profile.full_name||profile.email||'Admin'} · ${profile.role||'admin'}`:'Nije učitano');
    setText('#web-version',BUILD);
    setText('#backend-contract',manifest.backend_contract||snapshot.schema_version||'nije upisano');
    setText('#apk-target',manifest.apk_target_version||manifest.apk_version||manifest.apk_target||'nije upisano');
    setText('#project-ref',manifest.supabase_project_ref||(window.SOV_SUPABASE_URL?String(window.SOV_SUPABASE_URL).replace('https://','').replace('.supabase.co',''):'nije konfigurirano'));
    setText('#kpi-catalog',armoryCatalog==null?'—':fmt.format(armoryCatalog));
    setText('#kpi-requests',requests==null?'—':fmt.format(requests));
    setText('#kpi-pending',pending==null?'—':fmt.format(pending));
    setText('#kpi-loans',loans==null?'—':fmt.format(loans));
    setText('#kpi-android-fatal',fmt.format(androidFatal24));
    setText('#kpi-client-events',fmt.format(currentIncidents));
    setText('#crash-android-fatal',fmt.format(androidFatal24));
    setText('#crash-android-errors',fmt.format(androidWarnings24));
    setText('#crash-android-unhandled',fmt.format(androidFatal7));
    setText('#crash-web-fatal',fmt.format(webUnhandled24));
    setText('#crash-last-apk',errors.last_android_version||'Nema zapisa');
    setText('#crash-last-seen',fmtDate(errors.last_android_at));
    setText('#catalog-version',catalogManifest.catalog_version||'—');
    setText('#catalog-changed',catalogManifest.last_changed_at?fmtDate(catalogManifest.last_changed_at):'—');
    setText('#requests-changed',catalogManifest.requests_changed_at?fmtDate(catalogManifest.requests_changed_at):'—');
    setText('#security-rls',noRls==null?'—':fmt.format(noRls));
    setText('#security-policies',publicPolicies==null?'—':fmt.format(publicPolicies));
    setText('#security-log',hardening==null?'—':fmt.format(hardening));
    const gmailState=String(gmail.status||'unknown');
    setText('#gmail-status',gmailState==='ok'?'Radi':gmailState==='waiting'?'Čeka sljedeću provjeru':gmailState==='problem'?'Zahtjev je zapeo':'Nema podataka');
    setText('#gmail-queue',fmt.format(safeNumber(gmail.queued_requests)??0));
    setText('#gmail-last-import',fmtDate(gmail.last_success_at));
    setText('#crash-contract','APK pad je potvrđen samo ako aplikacija pošalje fatalan, neobrađen događaj. Obrađeni 403/401 odgovori nisu crash.');
    renderCrashTable(errors.recent_history||errors.recent||[]);
    const criticalBad=state.checks.some(c=>c.critical&&c.status!=='ok');
    const gmailProblem=gmailState==='problem';
    if(criticalBad||androidFatal24>0)markScore('bad','Problem',androidFatal24>0?'Android aplikacija prijavila je fatalni događaj u zadnja 24 sata.':'Kritična provjera nije prošla.');
    else if(currentIncidents>0||gmailProblem)markScore('warn','Pozor',gmailProblem?'Gmail zahtjev je zapeo i treba obradu.':'Postoji novi incident u zadnjih 30 minuta.');
    else markScore('ok','Radi','Web, prijava i baza trenutačno odgovaraju.');
    const payload={build:BUILD,checked_at:new Date().toISOString(),profile,local_manifest:localManifest,live_snapshot:snapshot,catalog_manifest:catalogManifest,checks:state.checks};
    const raw=$('#raw-json');if(raw)raw.textContent=JSON.stringify(payload,null,2)
  }
  async function loadFallback(sb,live){
    live.manifest=(await selectOne(sb,'sov_ecosystem_manifest_current','Manifest sustava',true)).data;
    live.catalogManifest=(await selectOne(sb,'sov_equipment_catalog_manifest','Manifest opreme')).data;
    live.catalogCount=await countRows(sb,'sov_equipment_app_catalog_grouped','Katalog opreme',true);
    live.requestsCount=await countRows(sb,'equipment_requests','Zahtjevi opreme');
    live.pendingRequests=await measure('Zahtjevi na čekanju',async()=>{const{count,error}=await sb.from('equipment_requests').select('*',{count:'exact',head:true}).eq('status','pending');if(error)throw error;return count||0});
    live.loansCount=await countRows(sb,'equipment_loans','Posudbe');
    live.hardeningRows=await countRows(sb,'sov_security_hardening_log','Sigurnosni zapisi');
    live.tablesWithoutRls={ok:true,data:null};live.openPolicies={ok:true,data:null}
  }
  async function init(){
    $('#refresh-status')?.addEventListener('click',()=>location.reload());
    log(`Pokrećem ${BUILD}`);setText('#web-version',BUILD);
    let localManifest=null;
    await measure('Lokalni manifest',async()=>{const r=await fetch('/assets/sov-ecosystem-manifest.json',{cache:'no-store'});if(!r.ok)throw new Error(`HTTP ${r.status}`);localManifest=await r.json();return localManifest});
    const configured=!!(window.SOV_SUPABASE_URL&&window.SOV_SUPABASE_ANON_KEY&&window.supabase&&window.supabase.createClient);
    if(!configured){setChip('#auth-chip','Nema konfiguracije','bad');markScore('bad','Nema konfiguracije','Supabase konfiguracija nije učitana.');renderChecks();renderSnapshot(null,localManifest,{});return}
    const sb=(window.SOVAuth&&window.SOVAuth.getClient)?window.SOVAuth.getClient():window.supabase.createClient(window.SOV_SUPABASE_URL,window.SOV_SUPABASE_ANON_KEY);
    let session=null,profile=null;
    const sessionCheck=await measure('Aktivna prijava',async()=>{const{data,error}=await sb.auth.getSession();if(error)throw error;if(!data.session)throw new Error('Nema aktivne prijave');session=data.session;return data.session.user.email},true);
    if(!sessionCheck.ok){setChip('#auth-chip','Nije prijavljen','bad');markScore('bad','Prijava je potrebna','Prijavi se i ponovno otvori status.');$('#login-card')?.classList.remove('sov-hidden');renderChecks();renderSnapshot(null,localManifest,{});return}
    setChip('#auth-chip','Prijavljen','ok');
    const profileCheck=await measure('Profil i ovlasti',async()=>{if(window.SOVAuth&&window.SOVAuth.getProfile)return await window.SOVAuth.getProfile(true);const{data,error}=await sb.from('profiles').select('*').eq('id',session.user.id).maybeSingle();if(error)throw error;return data},true);
    profile=profileCheck.data||{email:session.user.email,role:'user'};
    const role=String(profile.role||'').toLowerCase();
    if(!['admin','webmaster'].includes(role)){setChip('#auth-chip','Nema ovlasti','bad');markScore('bad','Nema ovlasti','Status je samo za administratora i webmastera.');$('#denied-card')?.classList.remove('sov-hidden');renderChecks();renderSnapshot(profile,localManifest,{});return}
    const live={};
    let snapshotCheck=await measure('Status baze v3',async()=>{const{data,error}=await sb.rpc('sov_system_status_snapshot_v3',{p_recent_limit:40});if(error)throw error;if(!data||typeof data!=='object')throw new Error('Prazan odgovor');return data});
    if(!snapshotCheck.ok){snapshotCheck=await measure('Status baze v2',async()=>{const{data,error}=await sb.rpc('sov_system_status_snapshot',{p_recent_limit:40});if(error)throw error;return data})}
    if(snapshotCheck.ok){live.snapshot=snapshotCheck.data;state.snapshot=snapshotCheck.data;live.catalogManifest=(await selectOne(sb,'sov_equipment_catalog_manifest','Manifest opreme')).data}else{log('Status RPC nije dostupan; koristim osnovne provjere.');await loadFallback(sb,live)}
    renderChecks();renderSnapshot(profile,localManifest,live);setText('#last-refresh',new Date().toLocaleString('hr-HR'));setChip('#db-chip',snapshotCheck.ok?'Veza radi':'Osnovna veza','ok')
  }
  document.addEventListener('DOMContentLoaded',init)
})();
