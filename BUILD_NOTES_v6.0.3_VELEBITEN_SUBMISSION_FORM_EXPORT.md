# BUILD NOTES — SOV Web v6.0.3 · Velebiten + Predaj novu jamu + XLSX export

Base: `sov-web-build-v6.0.2-procelnistvo-xls-sync.zip`

## Public web
- Added `velebiten.html` as a modern full page.
- Added Velebiten entry to the O nama dropdown and `o-drustvu.html` switchboard/route cards.
- Kept existing `novosti/novi-velebiten.html` as the current news article and linked it from the new permanent page.

## SOV Cloud
- Added standalone `predaj-novu-jamu.html` form.
- Dashboard and Karta now open the standalone form instead of only the old map modal flow.
- Form saves to:
  - `public.speleo_object_submissions`
  - `public.speleo_object_submission_files`
  - Supabase Storage bucket `speleo-submissions`
- Saved submissions appear in `arhivar-predane-jame.html`.

## Arhivar inbox
- `arhivar-predane-jame.html` now loads SheetJS and supports Excel export.
- Export options:
  - all currently filtered rows → `.xlsx`
  - selected submission → `.xlsx`
  - existing CSV / XML / ZIP package remains.
- XLSX export is flattened like Google Forms responses: timestamp, submitter, object fields, coordinates, notes, missing categories, file counts and file paths/links.

## Sync / versions
- `sync-status.html` updated to v6.0.3.
- `update.json`, `VERSION.txt`, `BUILD_VERSION.txt` updated.
- Oružarstvo cache/build metadata bumped to v6.0.3 / v603 while preserving the v6.0.2 hardboot behavior.

## SQL
- No mandatory new SQL if the previous Predane jame SQL is already active.
- Included optional safety patch: `SUPABASE_SOV_PREDANE_JAME_FORM_EXPORT_v6_0_3.sql`.
- Run it only if sync-status shows submissions table/storage errors or if this environment has not yet run the previous submissions SQL.
