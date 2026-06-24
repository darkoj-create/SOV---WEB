# SOV web v6.0.5 — system health center

Base: `sov-web-build-v6.0.4-version-cache-discipline.zip`

## Purpose
Second safe stabilization build from the audit plan. It adds one central, read-only Supabase health endpoint so `sync-status.html` becomes a real health center instead of only scattered per-page checks.

## Changed
- Added SQL patch: `SUPABASE_SOV_SYSTEM_HEALTH_v6_0_5.sql`.
- New RPC: `public.sov_system_health()`.
- New helper functions in `private` schema for safe table counts and RPC detection.
- `sync-status.html` now has a **SOV system health** card.
- `Provjeri sve` now checks version contract, then central system health, then old detailed module checks.
- Existing detailed checks remain as fallback diagnostics.
- `update.json`, `VERSION.txt`, `BUILD_VERSION.txt`, visible page labels and armory cache markers bumped to `v6.0.5` / `v605`.

## SQL
Run this in Supabase SQL editor:

```sql
SUPABASE_SOV_SYSTEM_HEALTH_v6_0_5.sql
```

The patch is additive and safe: it creates/replaces health functions only. It does not drop tables, change data, or change existing RLS policies.

## Smoke test
1. Deploy ZIP.
2. Run `SUPABASE_SOV_SYSTEM_HEALTH_v6_0_5.sql`.
3. Open `sync-status.html?b=605`.
4. Click `System health`; the new card should show `sov_system_health() · OK`.
5. Click `Provjeri sve`; if central health is green but a detailed card is yellow, use the detailed card as diagnostics.

## Rollback
Frontend rollback to v6.0.4 is safe. The SQL functions can remain in Supabase; they are read-only diagnostics.
