# SOV Web v5.59.4 — Field Tracking sync-status view fix

Purpose: fix Supabase SQL error 42P16 from v5.59.3:

`cannot change name of view column "sessions_total" to "field_events_total"`

## Fix

- New SQL file: `SUPABASE_SOV_FIELD_TRACKING_LITE_v5_59_4_SYNC_STATUS_VIEW_FIX.sql`
- Explicitly drops `public.sov_tracking_sync_status` before recreating it.
- Keeps v5.59.3 trip/team selector logic.
- Recreates the sync status view with field-event counts and session counts.

## Deploy

Run the v5.59.4 SQL after v5.59.2. Use it instead of v5.59.3 if v5.59.3 failed.

No APK changes required.
