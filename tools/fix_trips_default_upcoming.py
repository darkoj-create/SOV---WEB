#!/usr/bin/env python3
"""Ensure Trips opens on fresh, upcoming items without changing release metadata."""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PAGE = ROOT / 'izleti-cloud.html'

text = PAGE.read_text(encoding='utf-8')
original = text

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

required = [
    'value="upcoming" selected>Nadolazeći ovaj mjesec',
    "status==='upcoming'",
    "loadTrips({force:true}).finally(startTripsAutoSync)",
    'Nema nadolazećih izleta u ovom mjesecu.',
]
for marker in required:
    if marker not in text:
        raise SystemExit(f'Missing Trips upcoming marker: {marker}')

if text != original:
    PAGE.write_text(text, encoding='utf-8')
    print('izleti-cloud.html')

print('Trips default upcoming behavior is ready; release metadata was not changed.')
