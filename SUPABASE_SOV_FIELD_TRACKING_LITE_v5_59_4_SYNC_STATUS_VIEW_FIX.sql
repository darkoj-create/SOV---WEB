-- SOV Field Tracking Lite v5.59.4
-- Trip + team selector support + sync-status view drop/recreate fix.
-- Run after v5.59.2. This replaces v5.59.3 if that failed with ERROR 42P16 view column rename.

create or replace function public.sov_tracking_create_field_event_v2(
  p_source_trip_id uuid default null,
  p_title text default null,
  p_location text default null,
  p_start_at timestamptz default null,
  p_default_tracking_mode text default 'lite'
)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
  v_id uuid := gen_random_uuid();
  v_join text := public.sov_tracking_make_join_code();
  v_mode text := case when lower(coalesce(p_default_tracking_mode,'lite')) in ('route','tracking','gpx') then 'route' else 'lite' end;
  v_title text := nullif(trim(coalesce(p_title,'')), '');
begin
  if v_uid is null then
    raise exception 'Nisi prijavljen.';
  end if;

  if p_source_trip_id is not null and not public.sov_tracking_can_view_trip(p_source_trip_id) then
    raise exception 'Nemaš pravo otvoriti team za ovaj izlet.';
  end if;

  if v_title is null then
    v_title := case
      when p_source_trip_id is not null then 'Team ' || to_char(coalesce(p_start_at, now()), 'DD.MM.YYYY. HH24:MI')
      else 'Teren ' || to_char(coalesce(p_start_at, now()), 'DD.MM.YYYY. HH24:MI')
    end;
  end if;

  insert into public.sov_tracking_field_events(
    id, source_trip_id, title, location_text, start_at, status, join_code, default_tracking_mode, created_by
  ) values (
    v_id, p_source_trip_id, v_title, nullif(trim(coalesce(p_location,'')), ''), coalesce(p_start_at, now()), 'active', v_join, v_mode, v_uid
  );

  insert into public.sov_tracking_trip_members(trip_id, user_id, role, can_track, can_view)
  values (v_id, v_uid, 'leader', true, true)
  on conflict (trip_id,user_id) do update set role='leader', can_track=true, can_view=true;

  return jsonb_build_object(
    'ok', true,
    'field_event_id', v_id,
    'trip_id', v_id,
    'source_trip_id', p_source_trip_id,
    'join_code', v_join,
    'tracking_mode', v_mode,
    'title', v_title
  );
end;
$$;

grant execute on function public.sov_tracking_create_field_event_v2(uuid,text,text,timestamptz,text) to authenticated;

-- Keep sync-status compatible, but add field event count for v5.59.4.
-- PostgreSQL cannot rename view columns through CREATE OR REPLACE VIEW.
-- Older builds created this view with sessions_total as the first count column,
-- so we explicitly drop and recreate it to avoid ERROR 42P16.
drop view if exists public.sov_tracking_sync_status;

create view public.sov_tracking_sync_status as
select
  'sov_field_tracking_lite_v5_59_4'::text as module,
  (select count(*) from public.sov_tracking_field_events) as field_events_total,
  (select count(*) from public.sov_tracking_field_events where status='active') as field_events_active,
  (select count(*) from public.sov_tracking_sessions) as sessions_total,
  (select count(*) from public.sov_tracking_sessions where status='active') as sessions_active,
  (select count(*) from public.sov_tracking_points where recorded_at > now() - interval '24 hours') as points_24h,
  now() as checked_at;

grant select on public.sov_tracking_sync_status to authenticated;
