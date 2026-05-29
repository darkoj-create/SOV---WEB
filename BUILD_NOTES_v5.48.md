# SOV Web v5.48 — Fast Armory Sync / Cache-first

- Dodan cache-first katalog za Oružarstvo/Oružar Master.
- Web prvo provjerava `sov_equipment_catalog_manifest`; ako se katalog nije promijenio, koristi lokalni cache umjesto ponovnog skidanja cijelog kataloga.
- Ako manifest ili cloud pukne, ne gazi postojeći lokalni katalog prazninom.
- Nema promjene source-of-truth logike: read layer ostaje stabilizirani legacy view iz v5.47.3.

SQL: `SUPABASE_ORUZARSTVO_FAST_SYNC_v5_48.sql`
