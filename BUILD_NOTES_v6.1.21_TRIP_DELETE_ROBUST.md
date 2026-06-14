# SOV Web v6.1.21 — Trip delete robust SQL fix

- Web code remains on the v6.1.20 RPC delete path.
- Added robust SQL replacement for `sov_delete_trip_admin`.
- Delete now removes dependent trip rows before deleting `sov_trips`, covering old FKs without cascade.
- Role detection now uses JWT metadata, `sov_current_role()`, common profile tables, and role permissions.
- Creator/leader fallback allowed for own trips.

Deploy:
1. Run `SUPABASE_SOV_TRIP_DELETE_ROBUST_v6_1_21.sql`.
2. Deploy this web ZIP or keep v6.1.20 if already deployed.
3. Hard refresh browser.
