# SOV Admin APK 1.4.6 — Fast Armory Sync / Cache-first

- Oružarstvo sada odmah prikazuje zadnji lokalni katalog ako postoji.
- Cloud sync prvo čita mali `sov_equipment_catalog_manifest`.
- Ako se katalog nije promijenio, APK ne skida cijeli katalog ponovno, nego osvježi samo zahtjeve/queue.
- Ako se katalog promijenio, tek tada vuče `sov_equipment_app_catalog_grouped` i raw katalog za inventuru.
- Sync poruka više ne sugerira da se svaki put skida sve ispočetka.

SQL: `SUPABASE_ORUZARSTVO_FAST_SYNC_v5_48.sql`
