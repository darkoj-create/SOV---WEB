# SOV web v5.57.8 — Arhivar workflow checkovi + TK25 karta

Promjene:
- Arhivar status panel sada ima prošireni checklist: Pločica, Koordinate, Nacrt, Zapisnik, Fotka, Nacrt ne treba ponoviti.
- Dodan input za broj pločice / katastarski broj u status tabu.
- Novi SQL patch dodaje `has_plate`, `has_photo`, `needs_redraw` i `workflow_checks` u `sov_archive_object_status`.
- Novi RPC `sov_archive_update_object_status_v2` sprema prošireni checklist, a stari RPC ostaje radi kompatibilnosti s APK-om.
- Detail objekta u Arhivaru sada ima gumb "Otvori na TK25 karti" kad postoje koordinate.
- `baza.html` zna otvoriti objekt iz URL parametara `lat`, `lon`, `name`, `plate` i nacrtati marker čak i ako objekt nije u lokalnom JSON fallbacku.

Prvo pokrenuti:
`SUPABASE_SOV_ARHIVAR_WORKFLOW_v5_57_8_WORKFLOW_CHECKS_TK25.sql`

Zatim deployati web build.
