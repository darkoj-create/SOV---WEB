# SOV Supabase RLS/RPC audit — 2026-07-11

Project: `SOV-web` / `ncomefzkuixyfixisrhi`

## What was checked
Android-facing `public.sov_*` RPC functions used by trips, field tracking, trip assets, Speleo submissions, armory inventory, Speleo Runner, public news, and client logging.

## Critical findings fixed
| Function | Original risk | Applied fix |
|---|---|---|
| `sov_armory_save_inventory_count` | Could update inventory through SECURITY DEFINER without explicit login/role guard. | Added `auth.uid()` guard and `sov_can_manage_equipment()` role check. |
| `sov_delete_trip_team` | Deleted a team by id without explicit login/ownership guard. | Added login guard and trip edit/team owner checks. |
| `sov_save_trip_team` | Allowed insert/update without explicit logged-in user permission. | Added login guard; create requires trip read; update requires trip edit/team owner. |
| `sov_list_trip_messages` | Listed messages for any `trip_id`. | Added `sov_trip_can_read(p_trip_id)`. |
| `sov_list_trip_teams` | Listed teams for any `trip_id`. | Added `sov_trip_can_read(p_trip_id)`. |
| `sov_trip_assets_for_trip` | Listed trip packages/assets for any `trip_id`. | Added `sov_trip_can_read(p_trip_id)`. |

## RPC exposure changes
Anonymous `EXECUTE` and implicit `PUBLIC` execute were revoked from authenticated-only RPCs:
- trip admin/create/edit/delete/signup/team/message functions
- field tracking create/start/ingest/list/update functions
- trip asset register/list functions
- speleo submission review/update functions
- armory inventory count function

`authenticated` and `service_role` were granted where the Android app still needs JWT-authenticated calls.

## Intentionally left public for now
These are public/product endpoints and were not locked in this pass:
- `sov_news_public_list`
- `sov_news_public_detail`
- `sov_list_runner_leaderboard`
- `sov_submit_runner_score`
- `sov_log_client_error`

They still show in Supabase advisors because they are SECURITY DEFINER and public. They need a separate abuse/rate-limit/public-API hardening pass, not a blind revoke, otherwise public news, Runner leaderboard/score, or pre-login error logging can break.

## Verification after migration
Spot-check query confirmed:
- `anon_execute = false`
- `authenticated_execute = true`

for these critical app RPCs:
- `sov_armory_save_inventory_count`
- `sov_delete_trip_admin`
- `sov_delete_trip_team`
- `sov_list_trip_messages`
- `sov_list_trip_teams`
- `sov_save_trip_team`
- `sov_send_trip_message`
- `sov_tracking_ingest_batch`
- `sov_tracking_join_field_event`
- `sov_tracking_start_session_v2`
- `sov_trip_assets_for_trip`
- `sov_trip_signup`

## Remaining advisor findings
Supabase Security Advisor still reports many SECURITY DEFINER warnings for helper/debug/legacy functions and public endpoints. These should be handled in a second DB-hardening pass by classifying each as:
1. public and rate-limited/guarded,
2. authenticated only,
3. service-role/admin only,
4. trigger/helper function that should not be executable through PostgREST.

Do not mass-revoke them blindly without checking app/web dependencies.
