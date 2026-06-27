-- SOV Security Hardening Phase 1 TEMPLATE
-- IMPORTANT: Review the snapshot output first. This file is intentionally conservative.
-- Do NOT run blindly on production if web/APK were not tested against staging.

begin;

-- 1) Remove direct anon execution from known admin/write/security-definer RPCs.
-- Keep public read/list RPCs only if they are intentionally public and internally safe.
revoke execute on function public.sov_admin_update_user_profile(uuid, text, text, text) from anon;
revoke execute on function public.sov_admin_list_users() from anon;
revoke execute on function public.sov_admin_sync_missing_profiles() from anon;
revoke execute on function public.sov_save_trip(uuid, jsonb) from anon;
revoke execute on function public.sov_delete_trip_admin(uuid) from anon;
revoke execute on function public.sov_news_save(uuid, jsonb) from anon;
revoke execute on function public.sov_news_delete(uuid) from anon;
revoke execute on function public.sov_archive_update_object_status(text, text, text, boolean, boolean, boolean, text, text, text) from anon;
revoke execute on function public.sov_archive_update_object_status_v2(text, text, text, boolean, boolean, boolean, text, text, text, boolean, boolean, boolean, jsonb) from anon;
revoke execute on function public.sov_armory_upsert_simple_item(text, text, text, text, text, text, numeric, numeric, numeric, text, text, text, text, text, boolean) from anon;
revoke execute on function public.sov_armory_save_inventory_count(text, text, numeric, text, text) from anon;
revoke execute on function public.sov_tracking_ingest_batch(uuid, jsonb) from anon;
revoke execute on function public.sov_tracking_start_session(uuid, text, text, text, text) from anon;
revoke execute on function public.sov_tracking_start_session_v2(uuid, text, text, text, text, text, integer, text) from anon;

-- 2) Stop exposing auth.users through public views where possible.
-- Review definitions first. Prefer replacing views with security_invoker + projected safe columns.
-- Example pattern only:
-- create or replace view public.sov_profiles with (security_invoker = true) as select ... from public.sov_user_profiles ...;

-- 3) Legacy open write policies should be replaced table-by-table after app smoke test.
-- Example pattern only, do not blanket drop before testing:
-- drop policy if exists "SOV v4.77 open update" on public.equipment_items;
-- create policy equipment_items_update_armory on public.equipment_items for update to authenticated using (public.sov_can_manage_equipment()) with check (public.sov_can_manage_equipment());

commit;
