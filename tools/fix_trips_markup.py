#!/usr/bin/env python3
from pathlib import Path

root=Path(__file__).resolve().parents[1]
path=root/'izleti-cloud.html'
text=path.read_text(encoding='utf-8')
changed=False

clean_toolbar='<div class="toolbar"><span class="statusLine" id="status">Učitavam…</span><button class="btn small" id="refreshBtn" type="button" aria-label="Osvježi izlete">↻ Osvježi</button></div>'
duplicate_toolbar='<div class="toolbar"><div class="toolbar"><span class="statusLine" id="status">Učitavam…</span><button class="btn small" id="refreshBtn" type="button" aria-label="Osvježi izlete">↻ Osvježi</button></div><button class="btn small" id="refreshBtn" type="button" aria-label="Osvježi izlete">↻ Osvježi</button></div>'
old_status='<span class="statusLine" id="status">Učitavam…</span>'

if duplicate_toolbar in text:
    text=text.replace(duplicate_toolbar,clean_toolbar,1)
    changed=True
elif old_status in text and 'id="refreshBtn"' not in text:
    text=text.replace(old_status,clean_toolbar,1)
    changed=True
elif text.count('id="refreshBtn"') != 1:
    raise SystemExit(f'Expected exactly one Trips refresh button, found {text.count(chr(105)+chr(100)+"=\"refreshBtn\"")}.')

old_bind="$('toggleFormBtn').onclick=()=>openForm(!$('tripForm').classList.contains('open')); $('topAddBtn').onclick=()=>openForm(true); $('sideAddBtn').onclick=()=>openForm(true); $('clearForm').onclick=clearForm; $('refreshBtn').onclick=()=>{toast('Osvježavam…');monthManuallyChanged=false;loadTrips({force:true});};"
new_bind="if($('toggleFormBtn')) $('toggleFormBtn').onclick=()=>openForm(!$('tripForm').classList.contains('open')); if($('topAddBtn')) $('topAddBtn').onclick=()=>openForm(true); if($('sideAddBtn')) $('sideAddBtn').onclick=()=>openForm(true); if($('clearForm')) $('clearForm').onclick=clearForm; if($('refreshBtn')) $('refreshBtn').onclick=()=>{toast('Osvježavam…');monthManuallyChanged=false;loadTrips({force:true});};"
if old_bind in text:
    text=text.replace(old_bind,new_bind,1)
    changed=True
elif new_bind not in text:
    raise SystemExit('Trips event binding marker not found.')

if text.count('id="refreshBtn"') != 1:
    raise SystemExit(f'Trips refresh button count invalid after repair: {text.count(chr(105)+chr(100)+"=\"refreshBtn\"")}.')

if changed:
    path.write_text(text,encoding='utf-8')
    print('Patched izleti-cloud.html refresh markup and event binding.')
else:
    print('Trips markup already repaired.')
