# SOV Admin APK 1.4.13 — Arhivar workflow

- Arhiva/Nacrti ekran više nije read-only placeholder.
- Učitava `sov_arhivar_worklist` iz Supabasea.
- Prikazuje što fali za katastar: nacrt, koordinate, zapisnik.
- Arhivar/Admin može spremiti checklist status kroz RPC `sov_archive_update_object_status`.
- Ima cache fallback ako nema signala.
- Web je i dalje glavni za dodavanje nacrta/zapisnika i edit objekta; APK je terenski status/checklist.
