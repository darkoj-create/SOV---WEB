# BUILD NOTES — SOV Web v6.0.1 · Oružarstvo hardboot + WOW sync

Date: 2026-06-01
Base: `sov-web-build-v6.0-wow-layer.zip`
Merged fix: `v5.59.12-oruzarstvo-hardboot-fix`

## Što je promijenjeno
- `oruzarstvo.html` sada koristi hardboot logiku: prvo renderira `data/oruzarstvo-data.json`, a Supabase live katalog i zahtjevi idu u pozadini.
- Zadržan je v6 WOW sloj: `assets/sov-wow-v6.css`, `assets/sov-wow-app-v6.css` i `assets/sov-wow-v6.js?v=6.0.1`.
- `assets/oruzarstvo-supabase.js` koristi novi cache key `sov_armory_catalog_cache_v601`, da browser ne vrati stari loader/cache.
- `sync-status.html` je usklađen na v6.0.1 i provjerava Oružarstvo static JSON + Supabase health.
- Dodan `update.json`, te su usklađeni `BUILD_VERSION.txt` i `VERSION.txt`.

## SQL
Nema novog obaveznog SQL-a za ovaj build. Ako baza već ima ranije patch-eve koje si pokrenuo, deploy je frontend-only.

## Quick smoke test
1. Otvori `oruzarstvo.html?b=601` — katalog mora prikazati kategorije bez čekanja Supabasea.
2. Otvori `sync-status.html?b=601` — Oružarstvo card mora pokazati Static JSON status.
3. Hard refresh / očisti cache ako browser vrati stari `v55911` loader.
