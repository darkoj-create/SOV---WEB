(function(){
  const VERSION='5.59.3';
  const $=id=>document.getElementById(id);
  let sb=null,map=null,markerLayer=null,lineLayer=null;
  let fieldEvents=[],sourceTrips=[],latest=[],points=[];
  let selectedTripFilter='all',selectedTeam='all-teams',hours=6,timer=null;
  const colors=['#d7f66f','#83e6c2','#78d7ff','#ffc46b','#c7a7ff','#ff8f8f','#8ee6ff','#b8f27a','#ffb0e8','#b6d4ff'];

  function client(){
    if(sb) return sb;
    if(window.SOVAuth&&window.SOVAuth.getClient) sb=window.SOVAuth.getClient();
    else if(window.supabase&&window.SOV_SUPABASE_URL&&window.SOV_SUPABASE_ANON_KEY) sb=window.supabase.createClient(window.SOV_SUPABASE_URL,window.SOV_SUPABASE_ANON_KEY,{auth:{persistSession:true,autoRefreshToken:true,detectSessionInUrl:true}});
    return sb;
  }
  function esc(v){return String(v==null?'':v).replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]));}
  function id(v){return String(v||'');}
  function shortId(v){v=id(v);return v?v.slice(0,8):'—'}
  function fmtTime(v){try{return new Date(v).toLocaleString('hr-HR',{day:'2-digit',month:'2-digit',hour:'2-digit',minute:'2-digit'})}catch(e){return '—'}}
  function ageText(v){const d=(Date.now()-new Date(v).getTime())/60000;if(!isFinite(d))return '—'; if(d<1)return 'upravo'; if(d<60)return 'prije '+Math.round(d)+' min'; const h=d/60; if(h<24)return 'prije '+Math.round(h)+' h'; return 'prije '+Math.round(h/24)+' d'}
  function setStatus(t){const el=$('trackingStatus'); if(el) el.textContent=t;}
  function selectedMode(){return document.querySelector('input[name="defaultMode"]:checked')?.value||'lite'}
  function sourceTripId(e){return id(e.source_trip_id||'');}
  function eventTitle(e){
    const parts=[e.title||e.location_text||'Team '+shortId(e.id), e.location_text && e.title ? e.location_text : '', e.start_at?fmtTime(e.start_at):''].filter(Boolean);
    const label=parts.join(' · ')||('Team '+shortId(e.id));
    return e.join_code ? `${label} · kod ${e.join_code}` : label;
  }
  function sourceTripTitle(t){return [t.title||t.name||t.trip_title||t.location_name||t.location||('Izlet '+shortId(t.id||t.cloudId)), t.start_date||t.date||t.start_at?fmtTime(t.start_date||t.date||t.start_at):''].filter(Boolean).join(' · ')}
  function findSourceTripTitle(tripId){
    const t=sourceTrips.find(x=>id(x.id||x.cloudId)===id(tripId));
    if(t) return sourceTripTitle(t);
    const e=fieldEvents.find(x=>sourceTripId(x)===id(tripId));
    return e ? ('Izlet '+shortId(tripId)) : ('Izlet '+shortId(tripId));
  }

  function initMap(){
    if(map) return;
    map=L.map('trackingMap',{zoomControl:true}).setView([44.8,15.2],8);
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap'}).addTo(map);
    markerLayer=L.layerGroup().addTo(map);
    lineLayer=L.layerGroup().addTo(map);
  }

  async function loadSourceTrips(c){
    sourceTrips=[];
    try{
      let q=await c.from('sov_trips_mobile_feed').select('*').order('start_date',{ascending:false}).limit(500);
      if(q.error) q=await c.from('sov_trips').select('*').order('start_date',{ascending:false}).limit(500);
      if(!q.error && Array.isArray(q.data)) sourceTrips=q.data;
    }catch(e){ console.warn('Trip feed nije dostupan, koristim field events.', e); }
    const seen=new Set(sourceTrips.map(t=>id(t.id||t.cloudId)).filter(Boolean));
    fieldEvents.forEach(e=>{
      const sid=sourceTripId(e);
      if(sid && !seen.has(sid)){
        sourceTrips.push({id:sid,title:'Izlet '+shortId(sid),start_date:e.start_at});
        seen.add(sid);
      }
    });
  }

  async function loadTrips(){
    const c=client(); if(!c) throw new Error('Supabase nije konfiguriran.');
    setStatus('Učitavam izlete i teamove…');
    const events=await c.rpc('sov_tracking_get_my_field_events');
    if(events.error) throw events.error;
    fieldEvents=(events.data||[]).map(e=>({...e,id:id(e.id),source_trip_id:e.source_trip_id? id(e.source_trip_id):null,kind:'field_event'}));
    await loadSourceTrips(c);
    renderTripSelect();
    if(!fieldEvents.length){
      renderTeamSelect(); renderEmpty('Još nema otvorenih teamova/terena. Odaberi izlet i klikni “Otvori teren”.'); setStatus('Nema otvorenih teamova.'); return;
    }
    // Default: ako postoji vezani izlet, odaberi prvi aktivni izlet; inače sve.
    const firstWithTrip=fieldEvents.find(e=>sourceTripId(e));
    selectedTripFilter = firstWithTrip ? sourceTripId(firstWithTrip) : 'all';
    if($('tripSelect')) $('tripSelect').value=selectedTripFilter;
    renderTeamSelect();
    await refresh();
  }

  function renderTripSelect(){
    const sel=$('tripSelect'); if(!sel) return;
    const linkedIds=new Set(fieldEvents.map(e=>sourceTripId(e)).filter(Boolean));
    const tripOptions=sourceTrips.filter(t=>linkedIds.has(id(t.id||t.cloudId)) || sourceTrips.length<=80);
    const opts=['<option value="all">Svi izleti / svi teamovi</option>'];
    tripOptions.forEach(t=>{const tid=id(t.id||t.cloudId); if(tid) opts.push(`<option value="${esc(tid)}">${esc(sourceTripTitle(t))}</option>`);});
    if(fieldEvents.some(e=>!sourceTripId(e))) opts.push('<option value="no-trip">Tereni bez vezanog izleta</option>');
    sel.innerHTML=opts.join('');
  }

  function currentEvents(){
    if(selectedTripFilter==='all') return fieldEvents.slice();
    if(selectedTripFilter==='no-trip') return fieldEvents.filter(e=>!sourceTripId(e));
    return fieldEvents.filter(e=>sourceTripId(e)===selectedTripFilter || id(e.id)===selectedTripFilter);
  }

  function renderTeamSelect(){
    const sel=$('teamSelect'); if(!sel) return;
    const events=currentEvents();
    if(!events.length){ sel.innerHTML='<option value="">Nema teamova za ovaj izlet</option>'; selectedTeam=''; return; }
    const opts=[];
    if(events.length>1) opts.push('<option value="all-teams">Svi teamovi u izletu</option>');
    events.forEach(e=>opts.push(`<option value="${esc(e.id)}">${esc(eventTitle(e))}</option>`));
    sel.innerHTML=opts.join('');
    const valid=new Set(['all-teams',...events.map(e=>e.id)]);
    if(!valid.has(selectedTeam)) selectedTeam=events.length>1?'all-teams':events[0].id;
    sel.value=selectedTeam;
  }

  function selectedEvents(){
    const events=currentEvents();
    if(!events.length) return [];
    if(selectedTeam==='all-teams') return events;
    return events.filter(e=>e.id===selectedTeam);
  }

  async function createTerrain(){
    const c=client(); if(!c)return;
    const src = (selectedTripFilter && !['all','no-trip'].includes(selectedTripFilter)) ? selectedTripFilter : null;
    const baseTitle = src ? findSourceTripTitle(src) : 'Novi teren';
    const title=$('createTerrainTitle').value.trim() || (baseTitle + ' — team');
    const loc=$('createTerrainLocation').value.trim();
    setStatus('Otvaram team/teren…');
    let res=await c.rpc('sov_tracking_create_field_event_v2',{p_source_trip_id:src,p_title:title,p_location:loc,p_start_at:null,p_default_tracking_mode:selectedMode()});
    if(res.error){
      // fallback za stariji SQL: radi, ali možda dozvoljava samo jedan team po izletu
      res=await c.rpc('sov_tracking_create_field_event',{p_source_trip_id:src,p_title:title,p_location:loc,p_start_at:null,p_default_tracking_mode:selectedMode()});
    }
    if(res.error) throw res.error;
    const d=res.data||{};
    $('joinCodeOut').textContent='Kod ekipe: '+(d.join_code||'—');
    await loadTrips();
    if(d.field_event_id){
      selectedTripFilter=d.source_trip_id || src || selectedTripFilter || 'all';
      if($('tripSelect')) $('tripSelect').value=selectedTripFilter;
      renderTeamSelect(); selectedTeam=id(d.field_event_id); if($('teamSelect')) $('teamSelect').value=selectedTeam;
      await refresh();
    }
  }

  async function joinTerrain(){
    const c=client(); if(!c)return;
    const code=$('joinCodeInput').value.trim(); if(!code){alert('Upiši kod ekipe.');return;}
    setStatus('Pridružujem te teamu…');
    const res=await c.rpc('sov_tracking_join_field_event',{p_join_code:code}); if(res.error) throw res.error;
    const d=res.data||{}; $('joinCodeOut').textContent='Kod ekipe: '+(d.join_code||code.toUpperCase());
    await loadTrips();
    if(d.field_event_id){ selectedTeam=id(d.field_event_id); if($('teamSelect')) $('teamSelect').value=selectedTeam; await refresh(); }
  }

  async function fetchEventData(c,e){
    const eventId=id(e.id);
    const lp=await c.rpc('sov_tracking_get_latest_positions',{p_trip_id:eventId}); if(lp.error) throw lp.error;
    let tp=await c.rpc('sov_tracking_get_trip_points_v2',{p_trip_id:eventId,p_hours:hours,p_user_id:null});
    if(tp.error){ tp=await c.rpc('sov_tracking_get_trip_points',{p_trip_id:eventId,p_hours:hours,p_user_id:null}); if(tp.error) throw tp.error; }
    const teamTitle=eventTitle(e);
    return {
      latest:(lp.data||[]).map(x=>({...x,_team_id:eventId,_team_title:teamTitle,_source_trip_id:sourceTripId(e)})),
      points:(tp.data||[]).map(x=>({...x,_team_id:eventId,_team_title:teamTitle,_source_trip_id:sourceTripId(e)}))
    };
  }

  async function refresh(){
    const c=client(); if(!c)return;
    const evs=selectedEvents();
    if(!evs.length){renderEmpty('Odaberi izlet i team ili otvori novi team za izlet.');return;}
    setStatus('Osvježavam team tracking…');
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
  function renderEmpty(msg){
    if($('peopleList')) $('peopleList').innerHTML='<div class="tracking-empty">'+esc(msg)+'</div>';
    markerLayer&&markerLayer.clearLayers(); lineLayer&&lineLayer.clearLayers();
    if($('kpiTeams')) $('kpiTeams').textContent='0'; if($('kpiPeople')) $('kpiPeople').textContent='0'; if($('kpiOnline')) $('kpiOnline').textContent='0'; if($('kpiPoints')) $('kpiPoints').textContent='0';
  }
  function render(evs){
    markerLayer.clearLayers(); lineLayer.clearLayers();
    if($('kpiTeams')) $('kpiTeams').textContent=String(evs.length);
    if($('kpiPeople')) $('kpiPeople').textContent=String(latest.length);
    if($('kpiPoints')) $('kpiPoints').textContent=String(points.length);
    if($('kpiOnline')) $('kpiOnline').textContent=String(latest.filter(x=>String(x.live_status).toLowerCase()==='online').length);
    if(!latest.length){renderEmpty('Još nema poslanih tracking točaka za odabrani izlet/team. Članovi se mogu pridružiti kodom ekipe i pokrenuti Lite ili ruta mod u appu.'); if($('kpiTeams')) $('kpiTeams').textContent=String(evs.length); return;}

    const byKey={};
    points.forEach(p=>{const k=(p._team_id||'team')+'::'+(p.user_id||p.device_id||p.session_id); (byKey[k]=byKey[k]||[]).push(p);});
    const bounds=[];
    Object.entries(byKey).forEach(([k,arr],idx)=>{
      arr.sort((a,b)=>new Date(a.recorded_at)-new Date(b.recorded_at));
      const latlngs=arr.map(p=>[Number(p.lat),Number(p.lng)]).filter(x=>isFinite(x[0])&&isFinite(x[1]));
      const mode=(arr[0]?.tracking_mode||'lite');
      if(latlngs.length>1) L.polyline(latlngs,{color:colors[idx%colors.length],weight:mode==='lite'?3:5,opacity:mode==='lite'?.55:.86,dashArray:mode==='lite'?'6 8':null}).addTo(lineLayer);
      latlngs.forEach(x=>bounds.push(x));
    });
    latest.forEach((p,idx)=>{
      if(!isFinite(Number(p.lat))||!isFinite(Number(p.lng)))return;
      const cls=statusClass(p.live_status);
      const color=cls==='online'?'#59f58d':cls==='stale'?'#ffd36c':cls==='sos'?'#ff4c4c':'#89919b';
      const name=p.display_name||p.device_id||'Član'; const mode=p.tracking_mode||'lite';
      const m=L.circleMarker([Number(p.lat),Number(p.lng)],{radius:9,color:'#061010',weight:3,fillColor:color,fillOpacity:.95});
      m.bindPopup(`<b>${esc(name)}</b><br>Team: ${esc(p._team_title||'—')}<br>Mod: ${esc(modeLabel(mode))}<br>Status: ${esc(p.live_status||'offline')}<br>Zadnje: ${esc(ageText(p.recorded_at))}<br>Baterija: ${p.battery_pct??'—'}%<br>GPS: ±${p.accuracy_m??'—'} m`);
      m.addTo(markerLayer); bounds.push([Number(p.lat),Number(p.lng)]);
    });
    if(bounds.length) map.fitBounds(bounds,{padding:[28,28],maxZoom:15});
    $('peopleList').innerHTML=latest.map(p=>{
      const cls=statusClass(p.live_status), name=p.display_name||p.device_id||'Član', mode=String(p.tracking_mode||'lite').toLowerCase();
      return `<div class="tracking-person" data-user="${esc(p.user_id||'')}" data-device="${esc(p.device_id||'')}"><strong><span class="tracking-dot ${cls}"></span>${esc(name)} <span class="tracking-badge ${mode==='route'?'route':'lite'}">${esc(modeLabel(mode))}</span></strong><small>${esc(p._team_title||'Team')} · ${esc(p.live_status||'offline')} · zadnje ${esc(ageText(p.recorded_at))} · baterija ${p.battery_pct??'—'}% · GPS ±${p.accuracy_m??'—'} m</small></div>`;
    }).join('');
  }

  function gpxFor(rows,name){
    const pts=rows.map(p=>`<trkpt lat="${Number(p.lat)}" lon="${Number(p.lng)}"><time>${new Date(p.recorded_at).toISOString()}</time>${p.altitude_m!=null?`<ele>${Number(p.altitude_m)}</ele>`:''}</trkpt>`).join('\n');
    return `<?xml version="1.0" encoding="UTF-8"?><gpx version="1.1" creator="SOV Field Tracking Lite ${VERSION}" xmlns="http://www.topografix.com/GPX/1/1"><trk><name>${esc(name)}</name><trkseg>${pts}</trkseg></trk></gpx>`;
  }
  function download(name,text,type){const blob=new Blob([text],{type:type||'application/gpx+xml'}); const a=document.createElement('a'); a.href=URL.createObjectURL(blob); a.download=name; a.click(); setTimeout(()=>URL.revokeObjectURL(a.href),1500);}
  function exportTrip(){ if(!points.length){alert('Nema točaka za export.');return;} download('sov-tracking-prikaz-'+(selectedTeam||selectedTripFilter||'teren')+'.gpx',gpxFor(points,'SOV tracking prikaz '+(selectedTeam||selectedTripFilter))); }

  window.SOVTrackingLite={refresh,exportTrip,createTerrain,joinTerrain,loadTrips};
  document.addEventListener('DOMContentLoaded',()=>{
    initMap();
    $('tripSelect')?.addEventListener('change',async e=>{selectedTripFilter=e.target.value||'all'; renderTeamSelect(); await refresh().catch(err=>{console.error(err);renderEmpty(err.message||String(err));});});
    $('teamSelect')?.addEventListener('change',async e=>{selectedTeam=e.target.value||''; await refresh().catch(err=>{console.error(err);renderEmpty(err.message||String(err));});});
    $('hoursSelect')?.addEventListener('change',async e=>{hours=Number(e.target.value)||0; await refresh().catch(err=>alert(err.message||err));});
    $('refreshBtn')?.addEventListener('click',()=>refresh().catch(err=>alert(err.message||err)));
    $('exportBtn')?.addEventListener('click',exportTrip);
    $('createTerrainBtn')?.addEventListener('click',()=>createTerrain().catch(err=>{console.error(err);alert(err.message||err);setStatus('Greška otvaranja teama.')}));
    $('joinTerrainBtn')?.addEventListener('click',()=>joinTerrain().catch(err=>{console.error(err);alert(err.message||err);setStatus('Greška pridruživanja.')}));
    loadTrips().catch(err=>{console.error(err); setStatus('Greška učitavanja.'); renderEmpty(err.message||String(err));});
    timer=setInterval(()=>refresh().catch(()=>{}),30000);
  });
})();
