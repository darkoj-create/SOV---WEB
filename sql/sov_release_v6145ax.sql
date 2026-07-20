-- SOV web v6.1.45ax
-- Factual system status, Gmail queue health and effective Trip lifecycle.

begin;

-- Existing past trips should no longer remain visually active/planned.
update public.sov_trips
set status = 'done',
    updated_at = now(),
    meta = coalesce(meta, '{}'::jsonb) || jsonb_build_object(
      'auto_closed_at', now(),
      'auto_closed_reason', 'end_date_before_today_v6145ax'
    )
where coalesce(end_date, start_date) < current_date
  and coalesce(status, 'planned') in ('planned', 'active');

create or replace function public.sov_trip_effective_status(
  p_status text,
  p_end_date date
)
returns text
language sql
stable
set search_path = pg_catalog, public
as $function$
  select case
    when coalesce(p_status, 'planned') in ('planned', 'active')
         and p_end_date is not null
         and p_end_date < current_date
      then 'done'
    else coalesce(p_status, 'planned')
  end;
$function$;

comment on function public.sov_trip_effective_status(text,date) is
  'Returns done for planned/active trips whose end date is already in the past.';

create or replace function public.sov_list_trips_feed()
returns jsonb
language plpgsql
stable
security definer
set search_path = pg_catalog, public, auth
as $function$
declare
  v_rows jsonb;
begin
  if auth.uid() is null then
    raise exception 'Nisi prijavljen.' using errcode = '42501';
  end if;

  if not (
    public.sov_rls_is_active_member(auth.uid())
    or public.sov_can_manage_trips_safe()
  ) then
    raise exception 'Nemaš pristup izletima.' using errcode = '42501';
  end if;

  select coalesce(
    jsonb_agg(to_jsonb(q) order by q.start_date nulls last, q.created_at nulls last),
    '[]'::jsonb
  )
  into v_rows
  from (
    select
      t.id,
      t.start_date,
      coalesce(t.end_date, t.start_date) as end_date,
      coalesce(t.title, trim(concat_ws(' · ', nullif(t.location_name,''), nullif(t.objective,''), t.start_date::text))) as title,
      t.leader_name,
      t.leader_user_id,
      t.location_name,
      t.objective,
      t.description,
      public.sov_trip_effective_status(t.status, coalesce(t.end_date, t.start_date)) as status,
      coalesce(t.status, 'planned') as stored_status,
      coalesce(t.visibility, 'club') as visibility,
      coalesce(nullif(t.trip_category,''), t.meta->>'trip_category', 'Izlet') as trip_category,
      t.min_lat,
      t.max_lat,
      t.min_lon,
      t.max_lon,
      t.center_lat,
      t.center_lon,
      t.created_by,
      t.updated_by,
      t.created_at,
      t.updated_at,
      t.last_synced_at,
      coalesce(t.source, 'web') as source,
      t.legacy_sheet_name,
      t.legacy_sheet_row,
      t.legacy_external_id,
      coalesce(files.file_count, 0) as file_count,
      coalesce(files.gpx_count, 0) as gpx_count,
      coalesce(files.kml_count, 0) as kml_count,
      coalesce(members.member_count, 0) as member_count,
      coalesce(t.meta, '{}'::jsonb) as meta,
      public.sov_can_manage_trips_safe() or t.created_by = auth.uid() or t.leader_user_id = auth.uid() as can_edit,
      public.sov_can_manage_trips_safe() as can_manage_all,
      case
        when public.sov_trip_effective_status(t.status, coalesce(t.end_date,t.start_date)) in ('planned','active')
          and coalesce(t.end_date,t.start_date) >= current_date
        then true else false
      end as is_relevant_now
    from public.sov_trips t
    left join lateral (
      select
        count(*)::int as file_count,
        count(*) filter (where f.file_type = 'gpx')::int as gpx_count,
        count(*) filter (where f.file_type in ('kml','kmz'))::int as kml_count
      from public.sov_trip_files f
      where f.trip_id = t.id
    ) files on true
    left join lateral (
      select count(*)::int as member_count
      from public.sov_trip_members m
      where m.trip_id = t.id
    ) members on true
    where public.sov_trip_effective_status(t.status, coalesce(t.end_date,t.start_date)) <> 'archived'
  ) q;

  return v_rows;
end;
$function$;

revoke all on function public.sov_list_trips_feed() from public;
revoke all on function public.sov_list_trips_feed() from anon;
grant execute on function public.sov_list_trips_feed() to authenticated, service_role;

create or replace function public.sov_system_status_snapshot_v3(p_recent_limit integer default 25)
returns jsonb
language plpgsql
security definer
set search_path = pg_catalog, public, auth
as $function$
declare
  v_limit integer := greatest(1, least(coalesce(p_recent_limit,25),100));
  v_now timestamptz := clock_timestamp();
  v_is_admin boolean := false;
  v_manifest jsonb := '{}'::jsonb;
  v_recent_history jsonb := '[]'::jsonb;
  v_current_recent jsonb := '[]'::jsonb;
  v_current_incidents bigint := 0;
  v_android_warning_24h bigint := 0;
  v_android_fatal_24h bigint := 0;
  v_android_fatal_7d bigint := 0;
  v_web_unhandled_24h bigint := 0;
  v_last_android_at timestamptz;
  v_last_android_version text;
  v_tables_without_rls bigint := 0;
  v_public_write_policies bigint := 0;
  v_hardening_rows bigint := 0;
  v_catalog_count bigint := 0;
  v_requests_count bigint := 0;
  v_pending_requests bigint := 0;
  v_loans_count bigint := 0;
  v_profiles_count bigint := 0;
  v_pending_profiles bigint := 0;
  v_gmail_queued bigint := 0;
  v_gmail_stale bigint := 0;
  v_gmail_last_success timestamptz;
  v_gmail_latest_request jsonb := '{}'::jsonb;
  v_past_open_trips bigint := 0;
begin
  select exists(
    select 1 from public.profiles p
    where p.id=auth.uid()
      and p.status::text='approved'
      and p.role::text in ('admin','webmaster')
  ) into v_is_admin;

  if not v_is_admin then
    raise exception 'Nemaš ovlasti za Status sustava.' using errcode='42501';
  end if;

  with valid_events as (
    select l.*
    from public.sov_client_error_logs l
    where not (
      l.platform='web' and (
        coalesce(l.details->>'url','') ilike '%127.0.0.1%'
        or coalesce(l.details->>'stack','') ilike '%127.0.0.1%'
        or coalesce(l.details->>'url','') ilike '%localhost%'
        or coalesce(l.details->>'stack','') ilike '%localhost%'
      )
    )
  )
  select
    count(*) filter (
      where created_at >= v_now-interval '30 minutes'
        and (
          (severity='fatal' and handled=false)
          or (severity='error' and action not in ('fetch 401','fetch 404'))
          or (handled=false and severity<>'warning')
        )
    ),
    count(*) filter (
      where platform='android'
        and created_at >= v_now-interval '24 hours'
        and severity in ('warning','error')
        and handled=true
    ),
    count(*) filter (
      where platform='android'
        and created_at >= v_now-interval '24 hours'
        and severity='fatal'
        and handled=false
    ),
    count(*) filter (
      where platform='android'
        and created_at >= v_now-interval '7 days'
        and severity='fatal'
        and handled=false
    ),
    count(*) filter (
      where platform='web'
        and created_at >= v_now-interval '24 hours'
        and handled=false
        and severity in ('error','fatal')
    ),
    max(created_at) filter (where platform='android')
  into
    v_current_incidents,
    v_android_warning_24h,
    v_android_fatal_24h,
    v_android_fatal_7d,
    v_web_unhandled_24h,
    v_last_android_at
  from valid_events;

  select l.app_version
  into v_last_android_version
  from public.sov_client_error_logs l
  where l.platform='android'
  order by l.created_at desc
  limit 1;

  with valid_events as (
    select l.*
    from public.sov_client_error_logs l
    where l.created_at >= v_now-interval '7 days'
      and not (
        l.platform='web' and (
          coalesce(l.details->>'url','') ilike '%127.0.0.1%'
          or coalesce(l.details->>'stack','') ilike '%127.0.0.1%'
          or coalesce(l.details->>'url','') ilike '%localhost%'
          or coalesce(l.details->>'stack','') ilike '%localhost%'
        )
      )
  ), shaped as (
    select jsonb_build_object(
      'id',x.id,
      'created_at',x.created_at,
      'platform',x.platform,
      'app_version',x.app_version,
      'screen',x.screen,
      'action',x.action,
      'severity',x.severity,
      'message',left(x.message,700),
      'handled',x.handled,
      'exception_class',nullif(x.details->>'exception_class',''),
      'fingerprint',nullif(x.details->>'fingerprint',''),
      'device',jsonb_strip_nulls(jsonb_build_object(
        'manufacturer',nullif(x.device_info->>'manufacturer',''),
        'model',nullif(x.device_info->>'model',''),
        'android',nullif(x.device_info->>'android',''),
        'sdk',nullif(x.device_info->>'sdk','')
      ))
    ) as row_json,
    x.created_at,
    (
      x.created_at >= v_now-interval '30 minutes'
      and (
        (x.severity='fatal' and x.handled=false)
        or (x.severity='error' and x.action not in ('fetch 401','fetch 404'))
        or (x.handled=false and x.severity<>'warning')
      )
    ) as is_current
    from valid_events x
    order by x.created_at desc
    limit v_limit
  )
  select
    coalesce(jsonb_agg(row_json order by created_at desc),'[]'::jsonb),
    coalesce(jsonb_agg(row_json order by created_at desc) filter(where is_current),'[]'::jsonb)
  into v_recent_history,v_current_recent
  from shaped;

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

  select count(*) into v_gmail_queued
  from public.sov_gmail_sync_requests
  where status in ('queued','processing');

  select count(*) into v_gmail_stale
  from public.sov_gmail_sync_requests
  where (status='queued' and created_at < v_now-interval '75 minutes')
     or (status='processing' and coalesce(started_at,updated_at,created_at) < v_now-interval '30 minutes');

  select max(created_at) into v_gmail_last_success
  from public.sov_gmail_minutes_import_log
  where status='imported';

  select coalesce(to_jsonb(r),'{}'::jsonb) into v_gmail_latest_request
  from (
    select id,status,requested_days,found_messages,imported_count,skipped_count,failed_count,result_message,created_at,started_at,completed_at,finished_at,updated_at
    from public.sov_gmail_sync_requests
    order by created_at desc
    limit 1
  ) r;

  select count(*) into v_past_open_trips
  from public.sov_trips
  where coalesce(end_date,start_date)<current_date
    and coalesce(status,'planned') in ('planned','active');

  return jsonb_build_object(
    'schema_version','3.0',
    'server_time',v_now,
    'manifest',coalesce(v_manifest,'{}'::jsonb),
    'armory',jsonb_build_object(
      'catalog',v_catalog_count,
      'requests',v_requests_count,
      'pending_requests',v_pending_requests,
      'loans',v_loans_count
    ),
    'users',jsonb_build_object('profiles',v_profiles_count,'pending_profiles',v_pending_profiles),
    'trips',jsonb_build_object('past_open_status_rows',v_past_open_trips),
    'security',jsonb_build_object(
      'tables_without_rls',v_tables_without_rls,
      'public_write_policies',v_public_write_policies,
      'hardening_log_rows',v_hardening_rows
    ),
    'gmail_sync',jsonb_build_object(
      'status',case when v_gmail_stale>0 then 'problem' when v_gmail_queued>0 then 'waiting' else 'ok' end,
      'queued_requests',v_gmail_queued,
      'stale_requests',v_gmail_stale,
      'last_success_at',v_gmail_last_success,
      'latest_request',v_gmail_latest_request
    ),
    'client_errors',jsonb_build_object(
      'health_window_minutes',30,
      'current_incidents_30m',v_current_incidents,
      'android_warning_24h',v_android_warning_24h,
      'android_fatal_24h',v_android_fatal_24h,
      'android_unhandled_7d',v_android_fatal_7d,
      'web_unhandled_24h',v_web_unhandled_24h,
      'last_android_at',v_last_android_at,
      'last_android_version',v_last_android_version,
      'current_recent',v_current_recent,
      'recent_history',v_recent_history
    )
  );
end;
$function$;

revoke all on function public.sov_system_status_snapshot_v3(integer) from public;
revoke all on function public.sov_system_status_snapshot_v3(integer) from anon;
grant execute on function public.sov_system_status_snapshot_v3(integer) to authenticated, service_role;

comment on function public.sov_system_status_snapshot_v3(integer) is
  'Admin status snapshot: current incidents are separated from expected auth responses, local audit noise and historical handled warnings.';

-- Old abandoned queue items must no longer make every future request look stuck.
update public.sov_gmail_sync_requests
set status='failed',
    result_message=coalesce(result_message,'Stari zahtjev nije imao aktivnog radnika i zatvoren je tijekom v6.1.45ax popravka.'),
    failed_count=greatest(coalesce(failed_count,0),1),
    completed_at=coalesce(completed_at,now()),
    finished_at=coalesce(finished_at,now()),
    updated_at=now()
where (status='queued' and created_at < now()-interval '75 minutes')
   or (status='processing' and coalesce(started_at,updated_at,created_at) < now()-interval '30 minutes');

commit;
