# SOV Web v5.59.2 — Field Tracking SQL syntax fix

Baseline: v5.59.1 Field Tracking terrain/modes.

Fix:
- SQL parser error at `''club''` in `sov_tracking_can_view_trip()` fixed to `'club'`.
- Added corrected SQL file: `SUPABASE_SOV_FIELD_TRACKING_LITE_v5_59_2_SQL_SYNTAX_FIX.sql`.
- No frontend behavior changes.
- No APK source changes.

Install order:
1. Run `SUPABASE_SOV_FIELD_TRACKING_LITE_v5_59_2_SQL_SYNTAX_FIX.sql`.
2. Web redeploy optional; this ZIP only carries corrected bundled SQL and version metadata.
