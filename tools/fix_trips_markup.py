#!/usr/bin/env python3
from pathlib import Path

root=Path(__file__).resolve().parents[1]
path=root/'izleti-cloud.html'
text=path.read_text(encoding='utf-8')

old_status='<span class="statusLine" id="status">Učitavam…</span>'
new_status='<div class="toolbar"><span class="statusLine" id="status">Učitavam…</span><button class="btn small" id="refreshBtn" type="button" aria-label="Osvježi izlete">↻ Osvježi</button></div>'
if old_status not in text and 'id="refreshBtn"' not in text:
    raise SystemExit('Trips status marker not found.')
text=text.replace(old_status,new_status,1)

old_bind="$('toggleFormBtn').onclick=()=>openForm(!$('tripForm').classList.contains('open')); $('topAddBtn').onclick=()=>openForm(true); $('sideAddBtn').onclick=()=>openForm(true); $('clearForm').onclick=clearForm; $('refreshBtn').onclick=()=>{toast('Osvježavam…');monthManuallyChanged=false;loadTrips({force:true);};"
# The exact production source has a correct object literal; keep a separate
# exact marker to avoid touching unrelated code.
old_bind="$('toggleFormBtn').onclick=()=>openForm(!$('tripForm').classList.contains('open')); $('topAddBtn').onclick=()=>openForm(true); $('sideAddBtn').onclick=()=>openForm(true); $('clearForm').onclick=clearForm; $('refreshBtn').onclick=()=>{toast('Osvježavam…');monthManuallyChanged=false;loadTrips({force:true});};"
new_bind="if($('toggleFormBtn')) $('toggleFormBtn').onclick=()=>openForm(!$('tripForm').classList.contains('open')); if($('topAddBtn')) $('topAddBtn').onclick=()=>openForm(true); if($('sideAddBtn')) $('sideAddBtn').onclick=()=>openForm(true); if($('clearForm')) $('clearForm').onclick=clearForm; if($('refreshBtn')) $('refreshBtn').onclick=()=>{toast('Osvježavam…');monthManuallyChanged=false;loadTrips({force:true});};"
if old_bind not in text:
    raise SystemExit('Trips event binding marker not found.')
text=text.replace(old_bind,new_bind,1)

path.write_text(text,encoding='utf-8')
print('Patched izleti-cloud.html refresh markup and event binding.')
