#!/usr/bin/env python3
"""Make Trips open on fresh, upcoming items for the visible month."""
from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PAGE = ROOT / 'izleti-cloud.html'

text = PAGE.read_text(encoding='utf-8')
original = text

text = re.sub(
    r'<script src="assets/sov-trips-cloud\.js\?v=[^"]+"></script>',
    '<script src="assets/sov-trips-cloud.js?v=6.1.45ay"></script>',
    text,
    count=1,
)

text = text.replace(
    '<select class="select" id="statusFilter"><option value="">Svi statusi</option>',
    '<select class="select" id="statusFilter"><option value="upcoming" selected>Nadolazeći ovaj mjesec</option><option value="">Svi statusi</option>',
    1,
)

text = text.replace(
    "let allTrips=[]; let selectedTrip=null; let selectedTripFiles=[]; let currentView='list'; let currentMonth=new Date(); let monthManuallyChanged=false;",
    "let allTrips=[]; let selectedTrip=null; let selectedTripFiles=[]; let currentView='list'; let currentMonth=new Date(new Date().getFullYear(),new Date().getMonth(),1); let monthManuallyChanged=false;",
    1,
)

old_filter = "function filteredTrips(){const q=clean($('search').value).toLowerCase(); const status=$('statusFilter').value; const cat=$('categoryFilter')?$('categoryFilter').value:''; return allTrips.filter(t=>(!status||t.status===status)&&(!cat||tripCategory(t)===cat)&&overlapsMonth(t)&&(!q||tripBlob(t).includes(q))).sort((a,b)=>String(a.start_date).localeCompare(String(b.start_date)))}"
new_filter = "function filteredTrips(){const q=clean($('search').value).toLowerCase(); const status=$('statusFilter').value; const cat=$('categoryFilter')?$('categoryFilter').value:''; const today=todayIso(); return allTrips.filter(t=>{const statusOk=status==='upcoming'?(['planned','active'].includes(t.status)&&isoDate(t.end_date||t.start_date)>=today):(!status||t.status===status); return statusOk&&(!cat||tripCategory(t)===cat)&&overlapsMonth(t)&&(!q||tripBlob(t).includes(q));}).sort((a,b)=>String(a.start_date).localeCompare(String(b.start_date)))}"
if new_filter not in text:
    if old_filter not in text:
        raise SystemExit('filteredTrips marker not found')
    text = text.replace(old_filter, new_filter, 1)

old_render = "function renderList(){const data=filteredTrips(); if($('listMonthTitle')) $('listMonthTitle').textContent=currentMonth.toLocaleDateString('hr-HR',{month:'long',year:'numeric'}); $('listWrap').innerHTML=data.length?data.map(tripCard).join(''):'<div class=\"empty\"><b>Nema izleta.</b></div>'; $('status').textContent=`${data.length} / ${allTrips.length} izleta`; bindTripClicks();}"
new_render = "function renderList(){const data=filteredTrips(); if($('listMonthTitle')) $('listMonthTitle').textContent=currentMonth.toLocaleDateString('hr-HR',{month:'long',year:'numeric'}); const upcoming=$('statusFilter').value==='upcoming'; $('listWrap').innerHTML=data.length?data.map(tripCard).join(''):`<div class=\"empty\"><b>${upcoming?'Nema nadolazećih izleta u ovom mjesecu.':'Nema izleta.'}</b></div>`; $('status').textContent=`${data.length} / ${allTrips.length} izleta`; bindTripClicks();}"
if new_render not in text:
    if old_render not in text:
        raise SystemExit('renderList marker not found')
    text = text.replace(old_render, new_render, 1)

text = text.replace(
    "clearForm(); setView('list'); loadTrips().finally(startTripsAutoSync);",
    "clearForm(); setView('list'); loadTrips({force:true}).finally(startTripsAutoSync);",
    1,
)

text = re.sub(
    r'<script defer="" src="assets/sov-version\.js\?v=[^"]+"></script>',
    '<script defer="" src="assets/sov-version.js?v=6.1.45ay"></script>',
    text,
    count=1,
)

required = [
    'value="upcoming" selected>Nadolazeći ovaj mjesec',
    "status==='upcoming'",
    "loadTrips({force:true}).finally(startTripsAutoSync)",
    'Nema nadolazećih izleta u ovom mjesecu.',
    'sov-trips-cloud.js?v=6.1.45ay',
]
for marker in required:
    if marker not in text:
        raise SystemExit(f'Missing Trips upcoming marker: {marker}')

if text != original:
    PAGE.write_text(text, encoding='utf-8')
    print('izleti-cloud.html')

(ROOT / 'VERSION.txt').write_text('6.1.45ay\n', encoding='utf-8')
(ROOT / 'BUILD_VERSION.txt').write_text('sov-web-build-v6.1.45ay-trips-auto-upcoming\n', encoding='utf-8')

version_path = ROOT / 'assets' / 'sov-version.js'
version = version_path.read_text(encoding='utf-8')
version = re.sub(r"const FALLBACK_VERSION='[^']+';", "const FALLBACK_VERSION='6.1.45ay';", version, count=1)
version = re.sub(r"const FALLBACK_CACHE='[^']+';", "const FALLBACK_CACHE='6145ay-trips-auto-upcoming';", version, count=1)
version = re.sub(r"const FALLBACK_BUILD='[^']+';", "const FALLBACK_BUILD='sov-web-build-v6.1.45ay-trips-auto-upcoming';", version, count=1)
version = re.sub(r"const FALLBACK_NAME='[^']+';", "const FALLBACK_NAME='v6.1.45ay-trips-auto-upcoming';", version, count=1)
version_path.write_text(version, encoding='utf-8')

manifest_path = ROOT / 'update.json'
manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
manifest.update({
    'version': '6.1.45ay',
    'versionName': 'v6.1.45ay-trips-auto-upcoming',
    'build': 'sov-web-build-v6.1.45ay-trips-auto-upcoming',
    'createdAt': '2026-07-21T00:10:00+02:00',
    'cacheBust': '6145ay-trips-auto-upcoming',
    'base': 'sov-web-build-v6.1.45ax-gmail-trips-status-visual',
    'requiresSql': False,
    'sqlFiles': [],
    'releaseType': 'trips-default-and-autoload',
    'changedFiles': [
        'izleti-cloud.html',
        'tools/fix_trips_default_upcoming.py',
        'tools/trips_default_upcoming_test.mjs',
        '.github/workflows/pre-release-audit.yml',
        'VERSION.txt',
        'BUILD_VERSION.txt',
        'assets/sov-version.js',
        'update.json'
    ],
    'notes': 'Trips now force-load fresh data on entry, keep cached rows visible while loading, auto-refresh in the background, and default to upcoming planned/active trips in the visible month.'
})
manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

print('Trips default upcoming fix ready.')
