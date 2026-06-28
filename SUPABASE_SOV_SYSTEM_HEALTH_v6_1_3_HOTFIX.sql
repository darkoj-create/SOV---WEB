-- SOV web v6.1.3 SYSTEM HEALTH HOTFIX
-- Fixes projects where public.profiles does not have approval_status.
-- Safe/idempotent: only replaces health helper functions/RPC. No destructive changes.

create schema if not exists private;

create or replace function private.sov_column_exists(p_schema text, p_table text, p_column text)
returns boolean
language sql
security definer
set search_path = public, private
as $sov$
  select exists(
    select 1
    from information_schema.columns
    where table_schema = p_schema
      and table_name = p_table
      and column_name = p_column
  );
$sov$;

create or replace function private.sov_table_exists(p_regclass text)
returns boolean
language plpgsql
security definer
set search_path = public, storage, private
as $sov$
begin
  return to_regclass(p_regclass) is not null;
exception when others then
  return false;
end;
$sov$;

create or replace function private.sov_safe_count(p_regclass text, p_where text default null)
returns jsonb
language plpgsql
security definer
set search_path = public, storage, private, auth
as $sov$
declare
  v_count bigint;
  v_sql text;
begin
  if to_regclass(p_regclass) is null then
    return jsonb_build_object('exists', false, 'count', null, 'ok', false);
  end if;

  v_sql := 'select count(*) from ' || p_regclass;
  if p_where is not null and length(trim(p_where)) > 0 then
    v_sql := v_sql || ' where ' || p_where;
  end if;

  execute v_sql into v_count;
  return jsonb_build_object('exists', true, 'count', v_count, 'ok', true);
exception when others then
  return jsonb_build_object('exists', to_regclass(p_regclass) is not null, 'count', null, 'ok', false, 'error', sqlerrm);
end;
$sov$;

create or replace function private.sov_profile_pending_count()
returns jsonb
language plpgsql
security definer
set search_path = public, private
as $sov$
declare
  v_count bigint;
  v_sql text;
  has_status boolean;
  has_approval_status boolean;
  has_is_approved boolean;
begin
  if to_regclass('public.profiles') is null then
    return jsonb_build_object('exists', false, 'count', null, 'ok', false);
  end if;

  has_status := private.sov_column_exists('public','profiles','status');
  has_approval_status := private.sov_column_exists('public','profiles','approval_status');
  has_is_approved := private.sov_column_exists('public','profiles','is_approved');

  if has_status and has_approval_status then
    v_sql := 'select count(*) from public.profiles where coalesce(status::text, approval_status::text, '''') in ('''',''pending'',''pending_approval'',''new'')';
  elsif has_status then
    v_sql := 'select count(*) from public.profiles where coalesce(status::text, '''') in ('''',''pending'',''pending_approval'',''new'')';
  elsif has_approval_status then
    v_sql := 'select count(*) from public.profiles where coalesce(approval_status::text, '''') in ('''',''pending'',''pending_approval'',''new'')';
  elsif has_is_approved then
    v_sql := 'select count(*) from public.profiles where coalesce(is_approved,false) = false';
  else
    v_sql := 'select 0::bigint';
  end if;

  execute v_sql into v_count;
  return jsonb_build_object('exists', true, 'count', v_count, 'ok', true, 'mode',
    case
      when has_status and has_approval_status then 'status+approval_status'
      when has_status then 'status'
      when has_approval_status then 'approval_status'
      when has_is_approved then 'is_approved'
      else 'no approval columns'
    end
  );
exception when others then
  return jsonb_build_object('exists', to_regclass('public.profiles') is not null, 'count', null, 'ok', false, 'error', sqlerrm);
end;
$sov$;

create or replace function private.sov_profile_role_count(p_roles text[])
returns jsonb
language plpgsql
security definer
set search_path = public, private
as $sov$
declare
  v_count bigint;
begin
  if to_regclass('public.profiles') is null then
    return jsonb_build_object('exists', false, 'count', null, 'ok', false);
  end if;
  if not private.sov_column_exists('public','profiles','role') then
    return jsonb_build_object('exists', true, 'count', 0, 'ok', false, 'error', 'profiles.role column does not exist');
  end if;
  execute 'select count(*) from public.profiles where role::text = any($1)' into v_count using p_roles;
  return jsonb_build_object('exists', true, 'count', v_count, 'ok', true);
exception when others then
  return jsonb_build_object('exists', to_regclass('public.profiles') is not null, 'count', null, 'ok', false, 'error', sqlerrm);
end;
$sov$;


create or replace function private.sov_submission_pending_count()
returns jsonb
language plpgsql
security definer
set search_path = public, private
as $sov$
declare
  v_count bigint;
  v_sql text;
  has_review_status boolean;
  has_status boolean;
begin
  if to_regclass('public.speleo_object_submissions') is null then
    return jsonb_build_object('exists', false, 'count', null, 'ok', false);
  end if;
  has_review_status := private.sov_column_exists('public','speleo_object_submissions','review_status');
  has_status := private.sov_column_exists('public','speleo_object_submissions','status');
  if has_review_status and has_status then
    v_sql := $q$select count(*) from public.speleo_object_submissions where coalesce(review_status::text, status::text, '') in ('','new','submitted','pending','pending_review','needs_review')$q$;
  elsif has_review_status then
    v_sql := $q$select count(*) from public.speleo_object_submissions where coalesce(review_status::text, '') in ('','new','submitted','pending','pending_review','needs_review')$q$;
  elsif has_status then
    v_sql := $q$select count(*) from public.speleo_object_submissions where coalesce(status::text, '') in ('','new','submitted','pending','pending_review','needs_review')$q$;
  else
    v_sql := 'select 0::bigint';
  end if;
  execute v_sql into v_count;
  return jsonb_build_object('exists', true, 'count', v_count, 'ok', true);
exception when others then
  return jsonb_build_object('exists', to_regclass('public.speleo_object_submissions') is not null, 'count', null, 'ok', false, 'error', sqlerrm);
end;
$sov$;

create or replace function private.sov_bucket_count(p_bucket_id text)
returns jsonb
language plpgsql
security definer
set search_path = public, storage, private
as $sov$
declare
  v_count bigint;
begin
  if to_regclass('storage.buckets') is null then
    return jsonb_build_object('exists', false, 'count', null, 'ok', false);
  end if;

  select count(*) into v_count from storage.buckets where id = p_bucket_id;
  return jsonb_build_object('exists', v_count > 0, 'count', v_count, 'ok', v_count > 0);
exception when others then
  return jsonb_build_object('exists', null, 'count', null, 'ok', false, 'error', sqlerrm);
end;
$sov$;

create or replace function private.sov_routine_exists(p_name text)
returns boolean
language sql
security definer
set search_path = public, private
as $sov$
  select exists(
    select 1
    from information_schema.routines
    where routine_schema = 'public'
      and routine_name = p_name
  );
$sov$;

create or replace function public.sov_system_health()
returns jsonb
language plpgsql
security definer
set search_path = public, storage, private, auth
as $sovhealth$
declare
  armory jsonb;
  submissions jsonb;
  news_storage jsonb;
  submissions_storage jsonb;
  trip_files_storage jsonb;
  trip_assets_storage jsonb;
begin
  news_storage := private.sov_bucket_count('sov-news');
  submissions_storage := private.sov_bucket_count('speleo-submissions');
  trip_files_storage := private.sov_bucket_count('sov-trip-files');
  trip_assets_storage := private.sov_bucket_count('sov-trip-assets');

  armory := jsonb_build_object(
    'grouped_catalog', private.sov_safe_count('public.sov_oruzarstvo_grouped_catalog'),
    'items', private.sov_safe_count('public.sov_oruzarstvo_items'),
    'inventory', private.sov_safe_count('public.sov_oruzarstvo_inventory'),
    'loans', private.sov_safe_count('public.sov_oruzarstvo_loans'),
    'health_rpc', private.sov_routine_exists('sov_oruzarstvo_health')
  );

  submissions := jsonb_build_object(
    'submissions', private.sov_safe_count('public.speleo_object_submissions'),
    'files', private.sov_safe_count('public.speleo_object_submission_files'),
    'pending_review', private.sov_submission_pending_count(),
    'storage_bucket', submissions_storage,
    'workflow_health_rpc', private.sov_routine_exists('sov_predane_jame_workflow_health'),
    'review_update_rpc', private.sov_routine_exists('sov_update_speleo_submission_review')
  );

  return jsonb_build_object(
    'ok', true,
    'checked_at', now(),
    'build', '6.1.3-hotfix',
    'identity', jsonb_build_object(
      'auth_users', private.sov_safe_count('auth.users'),
      'profiles', private.sov_safe_count('public.profiles'),
      'pending_profiles', private.sov_profile_pending_count(),
      'admins', private.sov_profile_role_count(array['admin','webmaster']),
      'oruzari', private.sov_profile_role_count(array['oruzar','admin','webmaster']),
      'editors', private.sov_profile_role_count(array['editor','urednik','admin','webmaster'])
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
      'points_24h', private.sov_safe_count('public.sov_tracking_points', 'created_at >= now() - interval ''24 hours'''),
      'latest_positions_view', case when to_regclass('public.sov_tracking_latest_positions') is null then false else true end
    ),
    'news', jsonb_build_object(
      'rows', private.sov_safe_count('public.sov_news'),
      'published', private.sov_safe_count('public.sov_news', 'coalesce(is_published,false) = true or coalesce(status::text,'''') = ''published'''),
      'storage_bucket', news_storage
    ),
    'storage', jsonb_build_object(
      'sov_news', news_storage,
      'speleo_submissions', submissions_storage,
      'sov_trip_files', trip_files_storage,
      'sov_trip_assets', trip_assets_storage
    ),
    'version_contract', jsonb_build_object(
      'expected_web_version', '6.1.3',
      'expected_update_json', 'update.json',
      'status', 'frontend_update_json_checked_separately'
    )
  );
exception when others then
  return jsonb_build_object('ok', false, 'checked_at', now(), 'build', '6.1.3-hotfix', 'error', sqlerrm);
end;
$sovhealth$;

grant execute on function public.sov_system_health() to authenticated;
grant execute on function public.sov_system_health() to anon;

comment on function public.sov_system_health() is 'SOV v6.1.3 central read-only health endpoint. Safe profiles approval_status fallback.';
