# BUILD NOTES — SOV Web v6.0.2 · Pročelništvo XLS sync

Base: `sov-web-build-v6.0.1-oruzarstvo-hardboot-wow-sync.zip`

Source data:
- `SOV pročelništvo.xlsx`
- Sheet: `SOV pročelništvo`
- Range used: `A1:F72`

## Changes

- `procelnistvo.html` regenerated from the XLS data.
- Current 2026 leadership corrected to:
  - Pročelnik: Vedran Ferenčak
  - Tajnik: Ivona Klišanin
  - Oružar: Teo Barić
  - Arhivar: Gorana Perić
  - Bibliotekar: Mia Šepčević
- Historical timeline now renders each year from 1956 to 2026 separately.
- Empty XLS rows 1957–1959 are kept as explicit “nema upisanih podataka” entries.
- `o-drustvu.html` Pročelništvo card is aligned with the same 2026 data.
- Added `data/procelnistvo.json` as a normalized static data source for future reuse.
- Cleaned stale wrong name placeholders/sample meeting records in `novi-zapisnik.html` and `pregled-zapisnika.html`.
- Bumped Oružarstvo page/build/cache metadata to `v6.0.2` / `sov_armory_catalog_cache_v602` without changing the hardboot static-first behavior.
- Updated build metadata:
  - `BUILD_VERSION.txt`
  - `VERSION.txt`
  - `update.json`
  - `sync-status.html`
  - dashboard build badge
  - WOW cache query `assets/sov-wow-v6.js?v=6.0.2`

## SQL

No SQL required.
