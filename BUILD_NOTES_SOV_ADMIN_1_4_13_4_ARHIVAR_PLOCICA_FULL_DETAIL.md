# SOV Admin APK 1.4.13.4 — Arhivar pločica + puni SQL detail

Popravci:
- Admin Arhivar više ne ostaje samo na light worklistu. Kad klikneš objekt, app zove `sov_arhivar_get_object_detail(p_object_id)` i dopunjava karticu punim SQL detaljem.
- Kartica objekta sada može prikazati puni opis/pristup/istraživanja/ekipu/status/raw SQL tekst koji vraća web SQL v5.57.5.
- `plocica` / `pločica` iz `field_tasks` se prikazuje kao stvarna falinka preko `missing_plate` i `missing_categories_text`.

Preduvjet:
- U Supabaseu pokrenuti web SQL `SUPABASE_SOV_ARHIVAR_WORKFLOW_v5_57_5_PLOCICA_FULL_SQL_DETAIL_FIX.sql`.
