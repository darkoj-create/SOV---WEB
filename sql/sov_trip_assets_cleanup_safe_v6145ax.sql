-- SOV v6.1.45ax
-- Backward-compatible Android cleanup endpoint without direct storage.objects deletion.
-- Physical file deletion must be performed through the Supabase Storage API.

begin;

create or replace function public.sov_trip_assets_cleanup_expired()
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, public, auth
as $function$
declare
  v_marked integer := 0;
  v_paths jsonb := '[]'::jsonb;
begin
  -- The old implementation tried DELETE FROM storage.objects, which Supabase
  -- intentionally blocks. Keep the RPC compatible for already-installed APKs,
  -- but only close expired metadata rows here.
  with expired as (
    select id, storage_bucket, storage_path
    from public.sov_trip_assets
    where deleted_at is null
      and is_active = true
      and expires_at is not null
      and expires_at <= now()
    for update
  ), updated as (
    update public.sov_trip_assets a
    set is_active = false,
        deleted_at = now(),
        updated_at = now(),
        metadata = coalesce(a.metadata, '{}'::jsonb) || jsonb_build_object(
          'storage_cleanup_status', 'deferred_to_storage_api',
          'metadata_closed_at', now()
        )
    from expired e
    where a.id = e.id
    returning e.storage_bucket, e.storage_path
  )
  select count(*)::integer,
         coalesce(jsonb_agg(jsonb_build_object(
           'bucket', storage_bucket,
           'path', storage_path
         )), '[]'::jsonb)
  into v_marked, v_paths
  from updated;

  return jsonb_build_object(
    'ok', true,
    'expired_asset_rows_closed', v_marked,
    'deleted_storage_objects', 0,
    'storage_cleanup', 'deferred_to_storage_api',
    'paths_pending_storage_api', v_paths
  );
end;
$function$;

revoke all on function public.sov_trip_assets_cleanup_expired() from public;
revoke all on function public.sov_trip_assets_cleanup_expired() from anon;
grant execute on function public.sov_trip_assets_cleanup_expired() to authenticated, service_role;

comment on function public.sov_trip_assets_cleanup_expired() is
  'Compatibility RPC for installed APKs. Marks expired metadata inactive and never deletes storage.objects directly.';

commit;
