# SOV Web v6.1.39c — Oružarstvo locations + inventory snapshots + loan clean slate

Baseline: v6.1.39b icon fit + user category display names.

## Supabase changes applied live

- Backed up current `equipment_locations` and item location fields to `sov_armory_location_cleanup_backup_20260614`.
- Backed up request/loan tables to `sov_armory_request_clean_slate_backup_20260614`.
- Normalized live locations to exactly:
  - `Oružarstvo - Klaićeva` (default)
  - `Krasno`
- Mapped existing items:
  - any old location containing `Krasno` -> `Krasno`
  - all other locations -> `Oružarstvo - Klaićeva`
- Added `equipment_catalog_snapshots` and `equipment_catalog_snapshot_items`.
- Added SECURITY DEFINER RPC `sov_armory_create_catalog_snapshot(name, description, source)`.
- Created initial snapshot: `Stara baza / stabilno stanje prije nove inventure`.
- Added `armory_hidden`, `armory_hidden_at`, `armory_hidden_reason` to `equipment_requests` and `equipment_loans`.
- Existing old requests were hidden from active armory view instead of deleted.

## Web changes

- Inventar and Inventura now show a visible **Baza inventara** selector:
  - live active database
  - saved snapshots / old base
  - create new snapshot button
- Location selector in item modal now only exposes:
  - `Oružarstvo - Klaićeva`
  - `Krasno`
- Return modal location choices aligned to the same canonical locations.
- Posudbe loads only non-hidden requests.
- Oružar can click **Makni iz viewa** on a request/loan; it is archived/hidden, not physically deleted.

## Not changed

- No equipment item deletion.
- No quantity recalculation.
- No category/import reset.
- No RLS cleanup in this build.
