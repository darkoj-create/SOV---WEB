#!/usr/bin/env python3
"""Deterministic v6.1.45ax cleanup for Gmail, Trips, status and UI copy."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
changed=[]

def read(rel): return (ROOT/rel).read_text(encoding='utf-8',errors='replace')
def write(rel,text):
    path=ROOT/rel
    old=path.read_text(encoding='utf-8',errors='replace') if path.exists() else None
    if old!=text:
        path.write_text(text,encoding='utf-8')
        changed.append(rel)

def require(text,marker,label):
    if marker not in text: raise SystemExit(f'Missing marker for {label}: {marker[:100]}')

# Professional, literal labels instead of novelty copy.
rel='zapisnici-native.html'; text=read(rel)
repls={
    '<title>Živi zapisnici. — SOV Velebit</title>':'<title>Arhiva zapisnika — SOV Velebit</title>',
    'content="Živi zapisnici. — SOV Velebit" property="og:title"':'content="Arhiva zapisnika — SOV Velebit" property="og:title"',
    'content="Živi zapisnici. — SOV Velebit" name="twitter:title"':'content="Arhiva zapisnika — SOV Velebit" name="twitter:title"',
    '<strong>Native tekst</strong>':'<strong>Arhiva zapisnika</strong>',
    '<span class="mn-eyebrow">Gmail / DOCX → native zapisnik</span>':'<span class="mn-eyebrow">Zapisnici sastanaka</span>',
    '<h1>Živi zapisnici.</h1>':'<h1>Arhiva zapisnika</h1>',
    '<p>Gmail ili ručni DOCX import ne mora spremati originalni DOCX u web. Sadržaj se sprema kao čitljiv tekst u bazi, lijepo prikazuje ovdje i po potrebi se ponovno izvozi u Word/DOCX.</p>':'<p>Zapisnici pristigli Gmailom ili ručnim unosom spremaju se kao pretraživ tekst. Iz svakog zapisa moguće je ponovno izvesti DOCX ili izdvojiti najave izleta.</p>',
    'Gmail sync i najave →':'Gmail sinkronizacija i najave →',
    '<span>Gmail import</span>':'<span>uvezeno iz Gmaila</span>',
    '<span>DOCX</span>':'<span>izvoz u DOCX</span>',
    '<section class="mn-note"><b>Napomena:</b> ova stranica koristi tablicu <code>meeting_minutes</code>. Za Gmail automatski uvoz treba pokrenuti SQL <code>SUPABASE_SOV_GMAIL_ZAPISNICI_NATIVE_v6_1_44e.sql</code> i postaviti Apps Script <code>SOV_GMAIL_ZAPISNICI_APPS_SCRIPT_v6_1_44e_NATIVE_QUEUE.gs</code>.</section>':'<section class="mn-note"><b>Sinkronizacija:</b> novi SOV zapisnici iz Gmaila provjeravaju se automatski. Gumb na stranici Najave može zatražiti dodatnu provjeru zadnja četiri tjedna.</section>',
    'assets/zapisnici-native-v6144e.js?v=6.1.44e':'assets/zapisnici-native-v6144e.js?v=6.1.45ax',
}
for a,b in repls.items(): text=text.replace(a,b)
write(rel,text)

rel='zapisnici-najave.html'; text=read(rel)
text=text.replace('Najave iz zapisnika. — SOV Velebit','Najave iz zapisnika — SOV Velebit')
text=text.replace('sprema ga u Žive zapisnike','sprema ga u Arhivu zapisnika')
text=text.replace('Automatski sync radi kroz Apps Script queue. Ručni sync traži DOCX zapisnike iz zadnja 4 tjedna, izvlači tekst i sprema ga u Žive zapisnike bez uploadanja DOCX datoteke.','Automatska provjera traži SOV zapisnike u Gmailu i sprema njihov tekst u Arhivu zapisnika. Dodatna provjera obuhvaća zadnja četiri tjedna i ne sprema originalnu DOCX datoteku u web.')
text=text.replace('Pokreni Gmail sync · zadnja 4 tjedna','Provjeri Gmail · zadnja 4 tjedna')
text=text.replace('assets/zapisnici-najave-v6143a.js?v=6.1.45ag','assets/zapisnici-najave-v6143a.js?v=6.1.45ax')
write(rel,text)

rel='dashboard.html'; text=read(rel)
text=text.replace('<title>Jednostavan ulaz u SOV sustav.</title>','<title>Članski dashboard — SOV Velebit</title>')
text=text.replace('content="Jednostavan ulaz u SOV sustav." property="og:title"','content="Članski dashboard — SOV Velebit" property="og:title"')
text=text.replace('content="Jednostavan ulaz u SOV sustav." name="twitter:title"','content="Članski dashboard — SOV Velebit" name="twitter:title"')
text=text.replace('content="Brzi ulaz u SOV Cloud module."','content="Članski alati Speleološkog odsjeka Velebit."')
write(rel,text)

# Trips: effective status + persistent cache + automatic background refresh.
rel='izleti-cloud.html'; text=read(rel)
if 'function effectiveTripStatus(' not in text:
    marker="function tripCategory(t){return clean(t.trip_category||t.category||t.raw?.trip_category||t.raw?.meta?.trip_category||t.objective||'Izlet')||'Izlet'}"
    require(text,marker,'Trips effective status insertion')
    insert="""function effectiveTripStatus(status,endDate){const s=clean(status)||'planned';const end=isoDate(endDate||'');const today=new Date().toISOString().slice(0,10);return(['planned','active'].includes(s)&&end&&end<today)?'done':s}\n"""
    text=text.replace(marker,insert+marker,1)
old="status:r.status||'planned',visibility:r.visibility||'club'"
new="status:effectiveTripStatus(r.status||'planned',end),visibility:r.visibility||'club'"
if old in text: text=text.replace(old,new,1)
text=text.replace("const rows=await window.SOVTripsCloud.listTrips();\n    setTripsFromRows(rows||[]);","const rows=await window.SOVTripsCloud.listTrips({force:true});\n    setTripsFromRows(rows||[]);",1)
if 'function startTripsAutoSync()' not in text:
    marker="clearForm(); setView('list'); loadTrips();"
    require(text,marker,'Trips auto sync insertion')
    autosync="""
let tripsAutoRefreshTimer=null;
let tripsRealtimeChannel=null;
function refreshTripsInBackground(){
  if(document.visibilityState==='hidden'||navigator.onLine===false)return;
  loadTrips({force:true,silent:true});
}
function startTripsAutoSync(){
  if(tripsAutoRefreshTimer)return;
  tripsAutoRefreshTimer=setInterval(refreshTripsInBackground,60000);
  window.addEventListener('focus',refreshTripsInBackground);
  window.addEventListener('online',refreshTripsInBackground);
  document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')refreshTripsInBackground()});
  try{
    const c=window.SOVTripsCloud&&window.SOVTripsCloud.sb&&window.SOVTripsCloud.sb();
    if(c&&c.channel){
      tripsRealtimeChannel=c.channel('sov-trips-web-autosync-v6145ax')
        .on('postgres_changes',{event:'*',schema:'public',table:'sov_trips'},refreshTripsInBackground)
        .on('postgres_changes',{event:'*',schema:'public',table:'sov_trip_members'},refreshTripsInBackground)
        .subscribe();
    }
  }catch(e){console.warn('[SOV trips] realtime fallback to timer',e)}
}
clearForm(); setView('list'); loadTrips().finally(startTripsAutoSync);
"""
    text=text.replace(marker,autosync,1)
write(rel,text)

# Native archive: stay fresh without repeated manual clicks.
rel='assets/zapisnici-native-v6144e.js'; text=read(rel)
if 'function startMinutesAutoSync()' not in text:
    marker="document.addEventListener('DOMContentLoaded',()=>{"
    require(text,marker,'minutes autosync')
    helper="""let minutesAutoTimer=null,minutesRealtime=null;
function startMinutesAutoSync(){
  if(minutesAutoTimer)return;
  const refresh=()=>{if(document.visibilityState==='visible'&&navigator.onLine!==false)load()};
  minutesAutoTimer=setInterval(refresh,120000);
  window.addEventListener('focus',refresh);window.addEventListener('online',refresh);
  document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')refresh()});
  try{if(sb&&sb.channel)minutesRealtime=sb.channel('sov-minutes-autosync-v6145ax').on('postgres_changes',{event:'*',schema:'public',table:'meeting_minutes'},refresh).subscribe()}catch(e){}
}
"""
    text=text.replace(marker,helper+marker,1)
text=text.replace("}); load(); });","}); load().then(startMinutesAutoSync); });",1)
text=text.replace("Vjerojatno treba pokrenuti SQL v6.1.44e.","Provjeri prijavu ili ovlasti za Arhivu zapisnika.")
text=text.replace("SQL/DB dio još nije postavljen. Ova stranica je frontend spremna.","Arhiva trenutačno nije dostupna. Ponovno se prijavi pa pokušaj još jednom.")
write(rel,text)

# Gmail queue UI: truthful processing window + automatic polling.
rel='assets/zapisnici-najave-v6143a.js'; text=read(rel)
old="else{status.textContent=r.status==='processing'?'Gmail sync je u tijeku…':'Gmail sync je zatražen i čeka obradu.';status.style.color='#ffbf6b'}"
new="else{const ageMin=Math.max(0,Math.round((Date.now()-new Date(r.created_at).getTime())/60000));status.textContent=r.status==='processing'?'Gmail provjera je u tijeku…':`Zahtjev čeka automatsku provjeru${ageMin?` · ${ageMin} min`:''}.`;status.style.color=ageMin>75?'#ff9f9f':'#ffbf6b'}"
if old in text:text=text.replace(old,new,1)
text=text.replace("status.textContent='Gmail sync je zatražen. Obrada će krenuti automatski.';","status.textContent='Zahtjev je spremljen. Automatska provjera pokreće se najmanje jednom na sat.';",1)
if 'let gmailStatusTimer=' not in text:
    marker="document.addEventListener('DOMContentLoaded',async()=>{"
    require(text,marker,'gmail polling')
    helper="""let gmailStatusTimer=null;
function startGmailStatusPolling(){
  if(gmailStatusTimer)return;
  const refresh=()=>{if(document.visibilityState==='visible')Promise.all([load(),showLastSyncRequest()]).catch(()=>{})};
  gmailStatusTimer=setInterval(refresh,60000);
  window.addEventListener('focus',refresh);window.addEventListener('online',refresh);
  document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')refresh()});
}
"""
    text=text.replace(marker,helper+marker,1)
text=text.replace("await Promise.all([load(),showLastSyncRequest()])}catch(e)","await Promise.all([load(),showLastSyncRequest()]);startGmailStatusPolling()}catch(e)",1)
write(rel,text)

# Bust logger cache everywhere and remove known novelty text variants.
for path in ROOT.rglob('*.html'):
    rel=str(path.relative_to(ROOT))
    text=path.read_text(encoding='utf-8',errors='replace')
    new=re.sub(r'assets/sov-client-logger\.js\?v=[^"\']+','assets/sov-client-logger.js?v=6.1.45ax',text)
    new=new.replace('Živi zapisnici','Arhiva zapisnika').replace('ŽIVI ZAPISNICI','ARHIVA ZAPISNIKA')
    new=new.replace('System status','Status sustava')
    if new!=text: write(rel,new)

# Release metadata.
write('VERSION.txt','6.1.45ax\n')
write('BUILD_VERSION.txt','sov-web-build-v6.1.45ax-gmail-trips-status-visual\n')
rel='assets/sov-version.js'; text=read(rel)
text=re.sub(r"const FALLBACK_VERSION='[^']+';","const FALLBACK_VERSION='6.1.45ax';",text,count=1)
text=re.sub(r"const FALLBACK_CACHE='[^']+';","const FALLBACK_CACHE='6145ax-gmail-trips-status-visual';",text,count=1)
text=re.sub(r"const FALLBACK_BUILD='[^']+';","const FALLBACK_BUILD='sov-web-build-v6.1.45ax-gmail-trips-status-visual';",text,count=1)
text=re.sub(r"const FALLBACK_NAME='[^']+';","const FALLBACK_NAME='v6.1.45ax-gmail-trips-status-visual';",text,count=1)
write(rel,text)

manifest_path=ROOT/'update.json'; manifest=json.loads(manifest_path.read_text(encoding='utf-8'))
manifest.update({
  'version':'6.1.45ax','versionName':'v6.1.45ax-gmail-trips-status-visual',
  'build':'sov-web-build-v6.1.45ax-gmail-trips-status-visual',
  'createdAt':'2026-07-20T23:15:00+02:00','cacheBust':'6145ax-gmail-trips-status-visual',
  'base':'sov-web-build-v6.1.45aw-trips-force-refresh','requiresSql':True,
  'sqlFiles':['sql/sov_release_v6145ax.sql'],'releaseType':'stability-and-ui-cleanup',
  'notes':'Automatic Trips refresh and effective past-trip completion, Gmail queue health, factual System Status v3, quieter client telemetry and professional zapisnici copy.'
})
write('update.json',json.dumps(manifest,ensure_ascii=False,indent=2)+'\n')

rel='README.md'; text=read(rel)
text=re.sub(r'^# SOV web build v[^\n]+','# SOV web build v6.1.45ax',text,count=1)
section="""
## v6.1.45ax — Gmail, Izleti, status i vizualno čišćenje

- prošli izleti više se ne prikazuju kao planirani ili aktivni;
- Izleti se osvježavaju u pozadini i ostaju spremljeni u lokalnom cacheu;
- Gmail red čekanja dobiva stvarni status i automatsku obradu;
- Status sustava odvaja aktualne incidente od starih, obrađenih i testnih događaja;
- Android warning nije označen kao crash;
- uklonjeni su nazivi poput „Živi zapisnici” i zamijenjeni jasnim nazivima;
- dodan je desktop/mobile vizualni layout audit ključnih stranica.

"""
if '## v6.1.45ax — Gmail, Izleti, status i vizualno čišćenje' not in text:
    pos=text.find('\n')+1;text=text[:pos]+section+text[pos:]
write(rel,text)

print(f'v6.1.45ax normalized {len(changed)} file(s).')
for x in changed:print(x)
