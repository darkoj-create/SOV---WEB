# SOV Web v5.34 — Unified Identity + Sync Control

Baza: `sov-web-build-v5.33-role-control-center.zip`

## Dodano
- Dashboard kartica za SOV Cloud sync control
- `sov-sync-control.js` za status/badge i budući web sync state
- `SUPABASE_SOV_UNIFIED_IDENTITY_SYNC_v5_34.sql`

## SQL
SQL je namjerno aditivan i ne dira postojeću bazu objekata. Dodaje:
- `sov_sync_queue`
- `sov_user_devices`
- RLS policyje za vlastite redove
- osvježeni `sov_current_user_permissions` view kompatibilan s APK 1.3.0

## Sljedeći korak
Nakon što SQL prođe u Supabaseu, APK modul po modul može početi slati lokalne akcije u queue.
