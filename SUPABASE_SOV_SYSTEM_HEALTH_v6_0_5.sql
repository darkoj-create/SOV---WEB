-- SOV web v6.0.5 — central system health RPC
-- Safe/aditive patch. Creates helper functions and public.sov_system_health().

create schema if not exists private;

create or replace function private.sov_safe_count(p_regclass text, p_where text default null)
returns jsonb
language plpgsql
security definer
set search_path = public, auth, storage
as $$
declare
  n bigint := null;
  q text;
begin
  if to_regclass(p_regclass) is null then
    return jsonb_build_object('ok', false, 'exists', false, 'count', null, 'error', 'missing: ' || p_regclass);
  end if;

  q := 'select count(*)::bigint from ' || p_regclass;
  if p_where is not null and length(trim(p_where)) > 0 then
    q := q || ' where ' || p_where;
  end if;

  execute q into n;
  return jsonb_build_object('ok', true, 'exists', true, 'count', coalesce(n,0));
exception when others then
  return jsonb_build_object('ok', false, 'exists', true, 'count', null, 'error', SQLERRM);
end;
$$;

create or replace function private.sov_rpc_exists(p_schema text, p_name text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from pg_proc p
    join pg_namespace n on n.oid = p.pronamespace
    where n.nspname = p_schema
      and p.proname = p_name
  );
$$;

create or replace function private.sov_try_rpc_jsonb(p_sql text)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  r jsonb;
begin
  execute p_sql into r;
  return coalesce(r, '{}'::jsonb);
exception when others then
  return jsonb_build_object('ok', false, 'error', SQLERRM);
end;
$$;

create or replace function public.sov_system_health()
returns jsonb
language plpgsql
security definer
set search_path = public, auth, storage
as $$
declare
  armory jsonb;
  submissions jsonb;
  news_storage jsonb;
  submissions_storage jsonb;
  trip_files_storage jsonb;
  trip_assets_storage jsonb;
begin
  if private.sov_rpc_exists('public','sov_oruzarstvo_health') then
    armory := private.sov_try_rpc_jsonb('select public.sov_oruzarstvo_health()::jsonb');
  else
    armory := jsonb_build_object(
      'ok', false,
      'rpc_missing', true,
      'grouped_catalog', private.sov_safe_count('public.sov_equipment_app_catalog_grouped')->'count',
      'raw_catalog', private.sov_safe_count('public.sov_equipment_app_catalog')->'count',
      'items', private.sov_safe_count('public.equipment_items')->'count',
      'ropes', private.sov_safe_count('public.equipment_ropes')->'count',
      'pieces', private.sov_safe_count('public.equipment_pieces')->'count'
    );
  end if;

  if private.sov_rpc_exists('public','sov_speleo_submissions_health') then
    submissions := private.sov_try_rpc_jsonb('select public.sov_speleo_submissions_health()::jsonb');
  else
    submissions := jsonb_build_object(
      'ok', false,
      'rpc_missing', true,
      'submissions', private.sov_safe_count('public.speleo_object_submissions'),
      'files', private.sov_safe_count('public.speleo_object_submission_files')
    );
  end if;

  news_storage := private.sov_safe_count('storage.buckets', $$id = 'sov-news'$$);
  submissions_storage := private.sov_safe_count('storage.buckets', $$id = 'speleo-submissions'$$);
  trip_files_storage := private.sov_safe_count('storage.buckets', $$id = 'sov-trip-files'$$);
  trip_assets_storage := private.sov_safe_count('storage.buckets', $$id = 'sov-trip-assets'$$);

  return jsonb_build_object(
    'ok', true,
    'app_version', '6.0.5',
    'checked_at', now(),
    'identity', jsonb_build_object(
      'auth_users', private.sov_safe_count('auth.users'),
      'profiles', private.sov_safe_count('public.profiles'),
      'pending_profiles', private.sov_safe_count('public.profiles', $$coalesce(approval_status,'pending') = 'pending' or coalesce(role,'pending') = 'pending'$$),
      'approved_profiles', private.sov_safe_count('public.profiles', $$coalesce(is_approved,false) = true or coalesce(approval_status,'') = 'approved'$$),
      'admins', private.sov_safe_count('public.profiles', $$role in ('admin','webmaster')$$),
      'arhivari', private.sov_safe_count('public.profiles', $$role in ('arhivar','admin','webmaster')$$),
      'oruzari', private.sov_safe_count('public.profiles', $$role in ('oruzar','admin','webmaster')$$),
      'editors', private.sov_safe_count('public.profiles', $$role in ('editor','urednik','admin','webmaster')$$)
    ),
    'roles', jsonb_build_object(
      'permissions', private.sov_safe_count('public.sov_role_permissions'),
      'current_user_permissions_view', case when to_regclass('public.sov_current_user_permissions') is null then false else true end
    ),
    'armory', armory,
    'submissions', submissions,
    'archive', jsonb_build_object(
      'worklist', private.sov_safe_count('public.sov_arhivar_worklist'),
      'staging_objects', private.sov_safe_count('public.speleo_objects_staging'),
      'live_objects', private.sov_safe_count('public.speleo_objects_live_sql'),
      'drawings', private.sov_safe_count('public.speleo_object_drawings')
    ),
    'trips', jsonb_build_object(
      'trips', private.sov_safe_count('public.sov_trips'),
      'members', private.sov_safe_count('public.sov_trip_members'),
      'files', private.sov_safe_count('public.sov_trip_files'),
      'assets', private.sov_safe_count('public.sov_trip_assets')
    ),
    'tracking', jsonb_build_object(
      'field_events', private.sov_safe_count('public.sov_tracking_field_events'),
      'sessions', private.sov_safe_count('public.sov_tracking_sessions'),
      'points_24h', private.sov_safe_count('public.sov_tracking_points', $$created_at >= now() - interval '24 hours'$$),
      'latest_positions_view', case when to_regclass('public.sov_tracking_latest_positions') is null then false else true end
    ),
    'news', jsonb_build_object(
      'rows', private.sov_safe_count('public.sov_news'),
      'published', private.sov_safe_count('public.sov_news', $$coalesce(is_published,false) = true or coalesce(status,'') = 'published'$$),
      'storage_bucket', news_storage
    ),
    'storage', jsonb_build_object(
      'sov_news', news_storage,
      'speleo_submissions', submissions_storage,
      'sov_trip_files', trip_files_storage,
      'sov_trip_assets', trip_assets_storage
    ),
    'version_contract', jsonb_build_object(
      'expected_web_version', '6.0.5',
      'expected_update_json', 'update.json',
      'status', 'frontend_update_json_checked_separately'
    )
  );
end;
$$;

grant execute on function public.sov_system_health() to authenticated;
grant execute on function public.sov_system_health() to anon;

comment on function public.sov_system_health() is 'SOV v6.0.5 central read-only health endpoint for sync-status/dashboard checks.';
