# SOV Web v5.59.11 — Oružarstvo health + fastload

Popravci:
- `/oruzarstvo.html` više ne smije ostati u beskonačnom loadanju ako Supabase view/manifest zapne.
- Oružarstvo prvo prikaže lokalni `data/oruzarstvo-data.json` katalog, a Supabase live katalog osvježava se naknadno.
- `assets/oruzarstvo-supabase.js` sada ima timeout za manifest i katalog view/table pozive.
- Cache key dignut je na `sov_armory_catalog_cache_v55911`; stari `v548` cache se ignorira/briše.
- `sync-status.html` dignut je na v5.59.11 i ima poseban Oružarstvo health check.
- Supabase status sada posebno prikazuje broj Oružara, umjesto da su sakriveni u “Editor / Arhivar / Oružar”.

SQL:
- Dodan `SUPABASE_ORUZARSTVO_HEALTH_v5_59_11.sql`.
- Kreira RPC `public.sov_oruzarstvo_health()` za read-only provjeru tablica/viewova oružarstva.

Redoslijed deploya:
1. Deploy web ZIP.
2. Pokreni `SUPABASE_ORUZARSTVO_HEALTH_v5_59_11.sql` u Supabase SQL Editoru.
3. Otvori `sync-status.html` i klikni “Provjeri sve” ili “Oružarstvo”.
4. Hard refresh `/oruzarstvo.html`.
