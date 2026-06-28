# SOV Web v5.44 — Final Equipment Grouping Rules

- Web oružarstvo sada preferira `sov_equipment_app_catalog_grouped` ako postoji.
- Katalog je grupiran za browse/search, dok raw `sov_equipment_app_catalog` ostaje za inventuru i povijest.
- Search tagovi više nisu preširoki po kategoriji: npr. `croll` ne smije vraćati užad/postavljanje.
- Grupe su centralne: descenderi, croll/prsni blokeri, pojasevi, karabineri, bušilice, baterije, crtaći pribor itd.
- SQL za ovaj build: `SUPABASE_ORUZARSTVO_APP_CATALOG_v5_44_FINAL_GROUPING_RULES.sql`.
