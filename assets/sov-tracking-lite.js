(function(){
  const VERSION='6.0.5';
  const $=id=>document.getElementById(id);
  let sb=null,map=null,markerLayer=null,lineLayer=null;
  let fieldEvents=[],sourceTrips=[],latest=[],points=[];
  let selectedTrip='';
  let selectedTeam='';
  let hours=6;
  let timer=null;
  const colors=['#d7f66f','#83e6c2','#78d7ff','#ffc46b','#c7a7ff','#ff8f8f','#8ee6ff','#b8f27a','#ffb0e8','#b6d4ff'];

  function client(){
    if(sb) return sb;
    if(window.SOVAuth&&window.SOVAuth.getClient) sb=window.SOVAuth.getClient();
    else if(window.supabase&&window.SOV_SUPABASE_URL&&window.SOV_SUPABASE_ANON_KEY) sb=window.supabase.createClient(window.SOV_SUPABASE_URL,window.SOV_SUPABASE_ANON_KEY,{auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}});
    return sb;
  }
  function esc(v){return String(v==null?'':v).replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
  function sid(v){return String(v||'');}
  function shortId(v){v=sid(v);return v?v.slice(0,8):'—'}
  function getTripId(t){return sid(t.id||t.cloudId||t.trip_id||t.source_trip_id);}
  function eventTripId(e){return sid(e.source_trip_id||'');}
  function fmtTime(v){try{return v?new Date(v).toLocaleString('hr-HR',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'}):''}catch(e){return ''}}
  function ageText(v){const d=(Date.now()-new Date(v).getTime())/60000;if(!isFinite(d))return '—'; if(d<1)return 'upravo'; if(d<60)return 'prije '+Math.round(d)+' min'; const h=d/60; if(h<24)return 'prije '+Math.round(h)+' h'; return 'prije '+Math.round(h/24)+' d'}
  function setStatus(t){const el=$('trackingStatus'); if(el) el.textContent=t;}
  function selectedMode(){return document.querySelector('input[name="defaultMode"]:checked')?.value||'lite'}
  function tripTitle(t){return [t.title||t.name||t.trip_title||t.location_name||t.location||('Izlet '+shortId(getTripId(t))), fmtTime(t.start_date||t.date||t.start_at)].filter(Boolean).join(' · ')}
  function findTripTitle(tripId){const t=sourceTrips.find(x=>getTripId(x)===sid(tripId)); return t?tripTitle(t):('Izlet '+shortId(tripId));}
  function eventTitle(e){return e.title||e.location_text||('Team '+shortId(e.id));}
  function eventLabel(e){const parts=[eventTitle(e), e.join_code?('kod '+e.join_code):'', e.default_tracking_mode==='route'?'ruta/GPX':'lite'].filter(Boolean); return parts.join(' · ');}

  function initMap(){
    if(map) return;
    map=L.map('trackingMap',{zoomControl:true}).setView([44.8,15.2],8);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);
    markerLayer=L.layerGroup().addTo(map);
    lineLayer=L.layerGroup().addTo(map);
  }

  async function loadFieldEvents(c){
    const events=await c.rpc('sov_tracking_get_my_field_events');
    if(events.error) throw events.error;
    fieldEvents=(events.data||[]).map(e=>({...e,id:sid(e.id),source_trip_id:e.source_trip_id?sid(e.source_trip_id):'',kind:'field_event'}));
  }

  async function loadSourceTrips(c){
    sourceTrips=[];
    const tryQueries=[
      async()=>c.from('sov_trips_mobile_feed').select('*').order('start_date',{ascending:false}).limit(500),
      async()=>c.from('sov_trips').select('*').order('start_date',{ascending:false}).limit(500)
    ];
    for(const qfn of tryQueries){
      try{const q=await qfn(); if(!q.error && Array.isArray(q.data)){sourceTrips=q.data; break;}}catch(e){}
    }
    const seen=new Set(sourceTrips.map(getTripId).filter(Boolean));
    fieldEvents.forEach(e=>{const tid=eventTripId(e); if(tid && !seen.has(tid)){sourceTrips.push({id:tid,title:'Izlet '+shortId(tid),start_date:e.start_at}); seen.add(tid);}});
  }

  function eventsForTrip(){
    if(!selectedTrip) return [];
    if(selectedTrip==='no-trip') return fieldEvents.filter(e=>!eventTripId(e));
    return fieldEvents.filter(e=>eventTripId(e)===selectedTrip || sid(e.id)===selectedTrip);
  }
  function selectedEvents(){
    const evs=eventsForTrip();
    if(!evs.length) return [];
    if(selectedTeam==='all-teams') return evs;
    return evs.filter(e=>sid(e.id)===sid(selectedTeam));
  }

  function renderTrips(){
    const sel=$('tripSelect'); if(!sel) return;
    const opts=[];
    if(!sourceTrips.length && !fieldEvents.length){ opts.push('<option value="">Nema izleta/terena</option>'); }
    else opts.push('<option value="">Odaberi izlet…</option>');
    sourceTrips.forEach(t=>{const id=getTripId(t); if(id) opts.push(`<option value="${esc(id)}">${esc(tripTitle(t))}</option>`);});
    if(fieldEvents.some(e=>!eventTripId(e))) opts.push('<option value="no-trip">Tereni bez vezanog izleta</option>');
    sel.innerHTML=opts.join('');

    const urlTrip=new URLSearchParams(location.search).get('trip')||'';
    const firstEventTrip=fieldEvents.find(e=>eventTripId(e));
    const firstTrip=getTripId(sourceTrips[0]||{});
    const valid=new Set(['', 'no-trip', ...sourceTrips.map(getTripId), ...fieldEvents.map(eventTripId).filter(Boolean)]);
    if(urlTrip && valid.has(urlTrip)) selectedTrip=urlTrip;
    else if(!selectedTrip) selectedTrip = firstEventTrip ? eventTripId(firstEventTrip) : (firstTrip || (fieldEvents.length?'no-trip':''));
    if(valid.has(selectedTrip)) sel.value=selectedTrip;
  }

  function renderTeams(){
    const sel=$('teamSelect'); if(!sel) return;
    const evs=eventsForTrip();
    if(!selectedTrip){sel.innerHTML='<option value="">Prvo odaberi izlet</option>'; selectedTeam=''; updateSelectionLabel(); return;}
    if(!evs.length){sel.innerHTML='<option value="">Nema teama — klikni “Dodaj team”</option>'; selectedTeam=''; updateSelectionLabel(); return;}
    const opts=[];
    if(evs.length>1) opts.push('<option value="all-teams">Svi teamovi u izletu</option>');
    evs.forEach(e=>opts.push(`<option value="${esc(e.id)}">${esc(eventLabel(e))}</option>`));
    sel.innerHTML=opts.join('');
    const valid=new Set(['all-teams',...evs.map(e=>sid(e.id))]);
    if(!valid.has(selectedTeam)) selectedTeam=evs.length>1?'all-teams':sid(evs[0].id);
    sel.value=selectedTeam;
    updateSelectionLabel();
  }

  function updateSelectionLabel(){
    const el=$('currentSelectionLabel'); if(!el) return;
    const evs=selectedEvents();
    const tripText=selectedTrip==='no-trip'?'Tereni bez izleta':(selectedTrip?findTripTitle(selectedTrip):'Nije odabran izlet');
    const teamText=!selectedTeam?'nema teama':selectedTeam==='all-teams'?'svi teamovi':(evs[0]?eventTitle(evs[0]):'team');
    el.textContent=tripText+' · '+teamText;
  }

  async function loadTrips(){
    const c=client(); if(!c) throw new Error('Supabase nije konfiguriran.');
    setStatus('Učitavam izlete i teamove…');
    await loadFieldEvents(c);
    await loadSourceTrips(c);
    renderTrips();
    renderTeams();
    if(selectedEvents().length) await refresh();
    else { clearMap(); renderEmpty(selectedTrip?'Za ovaj izlet još nema teama. Klikni “Dodaj team”.':'Odaberi izlet ili otvori prvi team.'); setStatus('Spremno.'); }
  }

  function clearMap(){
    if(markerLayer) markerLayer.clearLayers();
    if(lineLayer) lineLayer.clearLayers();
    latest=[]; points=[];
    updateKpis([]);
  }
  function renderEmpty(msg){
    if($('peopleList')) $('peopleList').innerHTML='<div class="tracking-empty">'+esc(msg)+'</div>';
  }

  async function createTerrain(){
    const c=client(); if(!c)return;
    if(!selectedTrip){alert('Prvo odaberi izlet.');return;}
    const src = selectedTrip==='no-trip' ? null : selectedTrip;
    const existing=eventsForTrip().length;
    const title=($('createTerrainTitle')?.value||'').trim() || ('Team '+(existing+1));
    const loc=($('createTerrainLocation')?.value||'').trim();
    setStatus('Otvaram team…');
    let res=await c.rpc('sov_tracking_create_field_event_v2',{p_source_trip_id:src,p_title:title,p_location:loc,p_start_at:null,p_default_tracking_mode:selectedMode()});
    if(res.error){ res=await c.rpc('sov_tracking_create_field_event',{p_source_trip_id:src,p_title:title,p_location:loc,p_start_at:null,p_default_tracking_mode:selectedMode()}); }
    if(res.error) throw res.error;
    const d=res.data||{};
    if($('joinCodeOut')) $('joinCodeOut').textContent='Kod ekipe: '+(d.join_code||'—');
    await loadFieldEvents(c);
    await loadSourceTrips(c);
    selectedTrip = d.source_trip_id || src || selectedTrip || 'no-trip';
    selectedTeam = sid(d.field_event_id||'');
    renderTrips(); if($('tripSelect')) $('tripSelect').value=selectedTrip; renderTeams(); if($('teamSelect')) $('teamSelect').value=selectedTeam;
    setStatus('Team otvoren. Podijeli kod ekipi.');
    await refresh();
  }

  async function joinTerrain(){
    const c=client(); if(!c)return;
    const code=($('joinCodeInput')?.value||'').trim(); if(!code){alert('Upiši kod ekipe.');return;}
    setStatus('Pridružujem te teamu…');
    const res=await c.rpc('sov_tracking_join_field_event',{p_join_code:code}); if(res.error) throw res.error;
    const d=res.data||{};
    if($('joinCodeOut')) $('joinCodeOut').textContent='Kod ekipe: '+(d.join_code||code.toUpperCase());
    await loadFieldEvents(c);
    await loadSourceTrips(c);
    const ev=fieldEvents.find(e=>sid(e.id)===sid(d.field_event_id));
    selectedTrip = d.source_trip_id || (ev?eventTripId(ev):selectedTrip) || 'no-trip';
    selectedTeam = sid(d.field_event_id||selectedTeam);
    renderTrips(); if($('tripSelect')) $('tripSelect').value=selectedTrip; renderTeams(); if($('teamSelect')) $('teamSelect').value=selectedTeam;
    await refresh();
  }

  async function fetchEventData(c,e){
    const eventId=sid(e.id);
    const lp=await c.rpc('sov_tracking_get_latest_positions',{p_trip_id:eventId}); if(lp.error) throw lp.error;
    let tp=await c.rpc('sov_tracking_get_trip_points_v2',{p_trip_id:eventId,p_hours:hours,p_user_id:null});
    if(tp.error){ tp=await c.rpc('sov_tracking_get_trip_points',{p_trip_id:eventId,p_hours:hours,p_user_id:null}); if(tp.error) throw tp.error; }
    return {
      latest:(lp.data||[]).map(x=>({...x,_team_id:eventId,_team_title:eventTitle(e),_source_trip_id:eventTripId(e),tracking_mode:x.tracking_mode||e.default_tracking_mode||'lite'})),
      points:(tp.data||[]).map(x=>({...x,_team_id:eventId,_team_title:eventTitle(e),_source_trip_id:eventTripId(e)}))
    };
  }

  async function refresh(){
    const c=client(); if(!c)return;
    const evs=selectedEvents();
    if(!evs.length){clearMap(); renderEmpty(selectedTrip?'Za ovaj izlet još nema teama. Klikni “Dodaj team”.':'Odaberi izlet i team.'); setStatus('Nema odabranog teama.'); return;}
    setStatus('Osvježavam pozicije…');
    latest=[]; points=[];
    for(const e of evs){
      const data=await fetchEventData(c,e);
      latest.push(...data.latest); points.push(...data.points);
    }
    render(evs);
    setStatus('Osvježeno '+new Date().toLocaleTimeString('hr-HR',{hour:'2-digit',minute:'2-digit'}));
  }

  function statusClass(s){s=String(s||'offline').toLowerCase(); if(s==='sos')return 'sos'; if(s==='online')return 'online'; if(s==='stale')return 'stale'; return 'offline'}
  function modeLabel(m){m=String(m||'lite').toLowerCase(); return m==='route'?'ruta/GPX':m==='sos'?'SOS':'lite ping'}
  function updateKpis(evs){
    const online=latest.filter(p=>String(p.live_status||'').toLowerCase()==='online').length;
    if($('kpiTeams')) $('kpiTeams').textContent=String(evs.length||0);
    if($('kpiPeople')) $('kpiPeople').textContent=String(latest.length||0);
    if($('kpiOnline')) $('kpiOnline').textContent=String(online||0);
    if($('kpiPoints')) $('kpiPoints').textContent=String(points.length||0);
    updateSelectionLabel();
  }
  function pointKey(p){return (p._team_id||'')+'|'+(p.user_id||p.device_id||'unknown')}
  function render(evs){
    initMap(); markerLayer.clearLayers(); lineLayer.clearLayers(); updateKpis(evs);
    if(!latest.length && !points.length){renderEmpty('Nema primljenih točaka za odabrani prikaz. Kad netko uključi tracking u APK-u, pojavit će se ovdje.'); return;}
    const grouped=new Map();
    points.forEach(p=>{const key=pointKey(p); if(!grouped.has(key)) grouped.set(key,[]); grouped.get(key).push(p);});
    const bounds=[];
    Array.from(grouped.values()).forEach((rows,idx)=>{
      rows=rows.filter(p=>isFinite(Number(p.lat))&&isFinite(Number(p.lng))).sort((a,b)=>new Date(a.recorded_at)-new Date(b.recorded_at));
      if(rows.length<2) return;
      const coords=rows.map(p=>[Number(p.lat),Number(p.lng)]);
      L.polyline(coords,{color:colors[idx%colors.length],weight:4,opacity:.86}).addTo(lineLayer);
      coords.forEach(c=>bounds.push(c));
    });
    latest.forEach((p,idx)=>{
      if(!isFinite(Number(p.lat))||!isFinite(Number(p.lng))) return;
      const cls=statusClass(p.live_status), name=p.display_name||p.device_id||'Član';
      const color=cls==='online'?'#59f58d':cls==='stale'?'#ffd36c':cls==='sos'?'#ff4c4c':'#89919b';
      const html=`<div style="width:18px;height:18px;border-radius:50%;background:${color};border:3px solid #08100f;box-shadow:0 0 0 3px rgba(255,255,255,.25)"></div>`;
      const icon=L.divIcon({html,className:'sov-track-marker',iconSize:[24,24],iconAnchor:[12,12]});
      L.marker([Number(p.lat),Number(p.lng)],{icon}).bindPopup(`<b>${esc(name)}</b><br>${esc(p._team_title||'Team')}<br>${esc(p.live_status||'offline')} · ${esc(ageText(p.recorded_at))}<br>Baterija: ${p.battery_pct??'—'}%<br>GPS: ±${p.accuracy_m??'—'} m`).addTo(markerLayer);
      bounds.push([Number(p.lat),Number(p.lng)]);
    });
    if(bounds.length) map.fitBounds(bounds,{padding:[28,28],maxZoom:15});
    if($('peopleList')) $('peopleList').innerHTML=latest.map(p=>{
      const cls=statusClass(p.live_status), name=p.display_name||p.device_id||'Član', mode=String(p.tracking_mode||'lite').toLowerCase();
      return `<div class="tracking-person" data-user="${esc(p.user_id||'')}" data-device="${esc(p.device_id||'')}"><strong><span class="tracking-dot ${cls}"></span>${esc(name)} <span class="tracking-badge ${mode==='route'?'route':'lite'}">${esc(modeLabel(mode))}</span></strong><small>${esc(p._team_title||'Team')} · ${esc(p.live_status||'offline')} · zadnje ${esc(ageText(p.recorded_at))} · baterija ${p.battery_pct??'—'}% · GPS ±${p.accuracy_m??'—'} m</small></div>`;
    }).join('') || '<div class="tracking-empty">Nema članova u odabranom prikazu.</div>';
  }

  function gpxFor(rows,name){
    const pts=rows.filter(p=>isFinite(Number(p.lat))&&isFinite(Number(p.lng))).sort((a,b)=>new Date(a.recorded_at)-new Date(b.recorded_at)).map(p=>`<trkpt lat="${Number(p.lat)}" lon="${Number(p.lng)}"><time>${new Date(p.recorded_at).toISOString()}</time>${p.altitude_m!=null?`<ele>${Number(p.altitude_m)}</ele>`:''}</trkpt>`).join('\n');
    return `<?xml version="1.0" encoding="UTF-8"?><gpx version="1.1" creator="SOV Field Tracking ${VERSION}" xmlns="http://www.topografix.com/GPX/1/1"><trk><name>${esc(name)}</name><trkseg>${pts}</trkseg></trk></gpx>`;
  }
  function download(name,text,type){const blob=new Blob([text],{type:type||'application/gpx+xml'}); const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download=name; a.click(); setTimeout(()=>URL.revokeObjectURL(a.href),1500);}
  function exportTrip(){ if(!points.length){alert('Nema točaka za export.');return;} download('sov-tracking-'+(selectedTeam||selectedTrip||'teren')+'.gpx',gpxFor(points,'SOV tracking '+(selectedTeam||selectedTrip))); }

  window.SOVTrackingLite={refresh,exportTrip,createTerrain,joinTerrain,loadTrips};
  document.addEventListener('DOMContentLoaded',()=>{
    initMap();
    $('tripSelect')?.addEventListener('change',async e=>{selectedTrip=e.target.value||''; selectedTeam=''; renderTeams(); await refresh().catch(err=>{console.error(err);renderEmpty(err.message||String(err));setStatus('Greška učitavanja.');});});
    $('teamSelect')?.addEventListener('change',async e=>{selectedTeam=e.target.value||''; updateSelectionLabel(); await refresh().catch(err=>{console.error(err);renderEmpty(err.message||String(err));setStatus('Greška učitavanja.');});});
    $('hoursSelect')?.addEventListener('change',async e=>{hours=Number(e.target.value)||0; await refresh().catch(err=>alert(err.message||err));});
    $('watchBtn')?.addEventListener('click',()=>refresh().catch(err=>{console.error(err);alert(err.message||err)}));
    $('refreshBtn')?.addEventListener('click',()=>refresh().catch(err=>alert(err.message||err)));
    $('exportBtn')?.addEventListener('click',exportTrip);
    $('createTerrainBtn')?.addEventListener('click',()=>createTerrain().catch(err=>{console.error(err);alert(err.message||err);setStatus('Greška otvaranja teama.')}));
    $('joinTerrainBtn')?.addEventListener('click',()=>joinTerrain().catch(err=>{console.error(err);alert(err.message||err);setStatus('Greška pridruživanja.')}));
    loadTrips().catch(err=>{console.error(err); setStatus('Greška učitavanja.'); renderEmpty(err.message||String(err));});
    timer=setInterval(()=>refresh().catch(()=>{}),30000);
  });
})();
