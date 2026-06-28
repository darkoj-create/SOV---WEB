# SOV Web v5.47.3 — read layer stabilize

Fix nakon DB konsolidacije:

- Web više ne ostaje prazan ako Supabase catalog view vrati 0 redova.
- Oružar Master fallbacka na `data/oruzarstvo-data.json` ako live katalog nije čitljiv.
- Aktivni app/web read layer treba biti vraćen na stabilne legacy-backed viewove kroz SQL `SUPABASE_ORUZARSTVO_DB_CONSOLIDATION_v5_47_3_READ_LAYER_STABILIZE.sql`.
- `equipment_assets` ostaje kao mirror/phase-1 konsolidacijska tablica, ali se još ne koristi kao glavni app/web katalog dok ne validiramo countove i RLS.
