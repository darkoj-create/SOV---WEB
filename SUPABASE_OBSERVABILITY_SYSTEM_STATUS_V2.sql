-- SOV Observability / System status v2
-- Production migration already applied to project ncomefzkuixyfixisrhi on 2026-07-20.
-- Additive only: no existing tables, rows, RLS policies or RPC signatures are removed.

create index if not exists idx_sov_client_error_logs_platform_severity_created
  on public.sov_client_error_logs (platform, severity, created_at desc);

create index if not exists idx_sov_client_error_logs_unhandled_created
  on public.sov_client_error_logs (created_at desc)
  where handled = false;

create or replace function public.sov_system_status_snapshot(
  p_recent_limit integer default 25
)
returns jsonb
language plpgsql
security definer
set search_path = public, pg_temp
as $function$
declare
  v_limit integer := greatest(1, least(coalesce(p_recent_limit, 25), 100));
  v_is_admin boolean := false;
  v_now timestamptz := clock_timestamp();
  v_total_24h bigint := 0;
  v_total_7d bigint := 0;
  v_android_24h bigint := 0;
  v_android_error_24h bigint := 0;
  v_android_fatal_24h bigint := 0;
  v_android_unhandled_7d bigint := 0;
  v_web_24h bigint := 0;
  v_web_fatal_24h bigint := 0;
  v_last_android_at timestamptz;
  v_last_android_version text;
  v_recent jsonb := '[]'::jsonb;
  v_tables_without_rls bigint := 0;
  v_public_write_policies bigint := 0;
  v_hardening_rows bigint := 0;
  v_catalog_count bigint := 0;
  v_requests_count bigint := 0;
  v_pending_requests bigint := 0;
  v_loans_count bigint := 0;
  v_profiles_count bigint := 0;
  v_pending_profiles bigint := 0;
  v_manifest jsonb := '{}'::jsonb;
begin
  select exists (
    select 1 from public.profiles p
    where p.id = auth.uid()
      and p.status::text = 'approved'
      and p.role::text in ('admin','webmaster')
  ) into v_is_admin;

  if not v_is_admin then
    raise exception 'Nemaš ovlasti za System status.' using errcode = '42501';
  end if;

  select
    count(*) filter (where created_at >= v_now - interval '24 hours'),
    count(*) filter (where created_at >= v_now - interval '7 days'),
    count(*) filter (where platform='android' and created_at >= v_now - interval '24 hours'),
    count(*) filter (where platform='android' and severity in ('error','fatal') and created_at >= v_now - interval '24 hours'),
    count(*) filter (where platform='android' and severity='fatal' and created_at >= v_now - interval '24 hours'),
    count(*) filter (where platform='android' and handled=false and created_at >= v_now - interval '7 days'),
    count(*) filter (where platform='web' and created_at >= v_now - interval '24 hours'),
    count(*) filter (where platform='web' and severity='fatal' and created_at >= v_now - interval '24 hours'),
    max(created_at) filter (where platform='android')
  into v_total_24h,v_total_7d,v_android_24h,v_android_error_24h,v_android_fatal_24h,
       v_android_unhandled_7d,v_web_24h,v_web_fatal_24h,v_last_android_at
  from public.sov_client_error_logs;

  select l.app_version into v_last_android_version
  from public.sov_client_error_logs l
  where l.platform='android'
  order by l.created_at desc limit 1;

  select coalesce(jsonb_agg(jsonb_build_object(
    'id',x.id,'created_at',x.created_at,'platform',x.platform,'app_version',x.app_version,
    'screen',x.screen,'action',x.action,'severity',x.severity,'message',left(x.message,700),
    'handled',x.handled,'exception_class',nullif(x.details->>'exception_class',''),
    'fingerprint',nullif(x.details->>'fingerprint',''),'reported_at',nullif(x.details->>'reported_at',''),
    'device',jsonb_strip_nulls(jsonb_build_object(
      'manufacturer',nullif(x.device_info->>'manufacturer',''),
      'model',nullif(x.device_info->>'model',''),
      'android',nullif(x.device_info->>'android',''),
      'sdk',nullif(x.device_info->>'sdk','')
    ))
  ) order by x.created_at desc),'[]'::jsonb)
  into v_recent
  from (
    select * from public.sov_client_error_logs
    where created_at >= v_now - interval '7 days'
    order by created_at desc limit v_limit
  ) x;

  select count(*) into v_tables_without_rls
  from pg_catalog.pg_class c
  join pg_catalog.pg_namespace n on n.oid=c.relnamespace
  where n.nspname='public' and c.relkind='r' and not c.relrowsecurity and c.relname<>'spatial_ref_sys';

  select count(*) into v_public_write_policies
  from pg_catalog.pg_policies p
  where p.schemaname='public'
    and p.cmd in ('ALL','INSERT','UPDATE','DELETE')
    and p.roles && array['public','anon']::name[];

  select count(*) into v_hardening_rows from public.sov_security_hardening_log;
  select count(*) into v_catalog_count from public.sov_equipment_app_catalog_grouped;
  select count(*) into v_requests_count from public.equipment_requests;
  select count(*) into v_pending_requests from public.equipment_requests where status='pending';
  select count(*) into v_loans_count from public.equipment_loans;
  select count(*) into v_profiles_count from public.profiles;
  select count(*) into v_pending_profiles from public.profiles where status::text='pending';
  select to_jsonb(m) into v_manifest from public.sov_ecosystem_manifest_current m limit 1;

  return jsonb_build_object(
    'schema_version','2.0','server_time',v_now,'manifest',coalesce(v_manifest,'{}'::jsonb),
    'armory',jsonb_build_object('catalog',v_catalog_count,'requests',v_requests_count,'pending_requests',v_pending_requests,'loans',v_loans_count),
    'users',jsonb_build_object('profiles',v_profiles_count,'pending_profiles',v_pending_profiles),
    'security',jsonb_build_object('tables_without_rls',v_tables_without_rls,'public_write_policies',v_public_write_policies,'hardening_log_rows',v_hardening_rows),
    'client_errors',jsonb_build_object(
      'total_24h',v_total_24h,'total_7d',v_total_7d,'android_24h',v_android_24h,
      'android_error_24h',v_android_error_24h,'android_fatal_24h',v_android_fatal_24h,
      'android_unhandled_7d',v_android_unhandled_7d,'web_24h',v_web_24h,
      'web_fatal_24h',v_web_fatal_24h,'last_android_at',v_last_android_at,
      'last_android_version',v_last_android_version,'recent',v_recent
    )
  );
end;
$function$;

revoke all on function public.sov_system_status_snapshot(integer) from public;
revoke all on function public.sov_system_status_snapshot(integer) from anon;
grant execute on function public.sov_system_status_snapshot(integer) to authenticated;
grant execute on function public.sov_system_status_snapshot(integer) to service_role;

comment on function public.sov_system_status_snapshot(integer)
  is 'Admin/Webmaster-only System status snapshot with release, security and client crash/error summaries.';

update public.sov_ecosystem_manifest
set backend_contract='2026.07.20-observability-v2',
    web_version='6.1.45au-system-status-crash',
    apk_target_version='1.4.56b-crash-bridge',
    release_channel='active',
    notes=jsonb_build_array(
      'Admin-only System status snapshot RPC v2',
      'Android fatal crashes are buffered locally and uploaded on next start from APK 1.4.56b',
      'Existing handled-error and Firebase Crashlytics flows remain enabled',
      'No trip, map, archive, armory or tracking data model changes'
    ),
    updated_at=now()
where id='current';
