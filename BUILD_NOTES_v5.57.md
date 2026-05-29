# SOV Web v5.57 — Arhivar workflow

- Novi `arhivar.html` kao radna ploča za Admin/Arhivar.
- Worklist iz speleo baze: objekt, pločica, tip, mjesto, koordinate, nacrti, zapisnici.
- Checklist za katastar: fali nacrt / fali koordinate / fali zapisnik.
- Dodavanje nacrta u `speleo_object_drawings`.
- Dodavanje zapisnika u `speleo_activity_reports`.
- Sigurni edit postojećeg objekta kroz `speleo_object_overrides` + `speleo_object_edits`.
- Novi objekt ide u `speleo_objects_staging`, bez razbijanja raw importa.
- SQL: `SUPABASE_SOV_ARHIVAR_WORKFLOW_v5_57_ALL_IN_ONE.sql`.
