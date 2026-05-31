-- SOV Field Tracking Lite v5.59.0
-- MVP: aktivni izlet -> Android lokalni queue -> batch sync -> web live mapa/trail.
-- Sigurnosna napomena: ovo je pomocni terenski alat, nije sluzbeni/spasilacki sustav.

create extension if not exists pgcrypto;

-- -----------------------------------------------------------------------------
-- Helperi role/trip pristupa bez rušenja ako neke stare funkcije/tablice ne postoje
-- -----------------------------------------------------------------------------
create or replace function public.sov_tracking_is_webmaster_or_admin()
returns boolean
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_email text := lower(coalesce(auth.jwt()->>'email',''));
  v_role text := lower(coalesce(auth.jwt()->'user_metadata'->>'role', auth.jwt()->'app_metadata'->>'role', ''));
begin
  if v_email in ('darko.jras@gmail.com','darko.jeras@gmail.com') then return true; end if;
  if v_role in ('webmaster','admin') then return true; end if;

  if to_regclass('public.profiles') is not null then
    begin
      execute 'select lower(coalesce(role::text,'''')) from public.profiles where id = $1 limit 1'
        into v_role using auth.uid();
      if v_role in ('webmaster','admin') then return true; end if;
    exception when others then
      null;
    end;
  end if;

  return false;
end;
$$;

create or replace function public.sov_tracking_can_view_trip(p_trip_id uuid)
returns boolean
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if auth.uid() is null then return false; end if;
  if public.sov_tracking_is_webmaster_or_admin() then return true; end if;

  if exists (
    select 1 from public.sov_tracking_trip_members m
    where m.trip_id = p_trip_id and m.user_id = auth.uid() and m.can_view = true
  ) then return true; end if;

  if to_regclass('public.sov_trip_members') is not null then
    if exists (
      select 1 from public.sov_trip_members m
      where m.trip_id = p_trip_id and m.user_id = auth.uid()
    ) then return true; end if;
  end if;

  if to_regclass('public.sov_trips') is not null then
    if exists (
      select 1 from public.sov_trips t
      where t.id = p_trip_id
        and (
          t.created_by = auth.uid()
          or lower(coalesce(t.visibility::text,''club'')) in ('club','public')
        )
    ) then return true; end if;
  end if;

  return false;
end;
$$;

create or replace function public.sov_tracking_can_track_trip(p_trip_id uuid)
returns boolean
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if auth.uid() is null then return false; end if;
  if public.sov_tracking_is_webmaster_or_admin() then return true; end if;

  if exists (
    select 1 from public.sov_tracking_trip_members m
    where m.trip_id = p_trip_id and m.user_id = auth.uid() and m.can_track = true
  ) then return true; end if;

  if to_regclass('public.sov_trip_members') is not null then
    if exists (
      select 1 from public.sov_trip_members m
      where m.trip_id = p_trip_id and m.user_id = auth.uid()
    ) then return true; end if;
  end if;

  -- MVP fallback: approved cloud user can track a visible active/planned club trip.
  if to_regclass('public.sov_trips') is not null then
    if exists (
      select 1 from public.sov_trips t
      where t.id = p_trip_id
        and lower(coalesce(t.status::text,'planned')) not in ('cancelled','deleted','archived')
        and lower(coalesce(t.visibility::text,'club')) in ('club','public')
    ) then return true; end if;
  end if;

  return false;
end;
$$;

-- -----------------------------------------------------------------------------
-- Tablice
-- -----------------------------------------------------------------------------
create table if not exists public.sov_tracking_trip_members (
  trip_id uuid not null,
  user_id uuid not null references auth.users(id) on delete cascade,
  role text not null default 'participant' check (role in ('participant','leader','viewer','admin')),
  can_track boolean not null default true,
  can_view boolean not null default true,
  created_at timestamptz not null default now(),
  primary key (trip_id, user_id)
);

create table if not exists public.sov_tracking_sessions (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid not null,
  user_id uuid references auth.users(id) on delete set null,
  device_id text not null,
  display_name text,
  role_label text,
  status text not null default 'active' check (status in ('active','paused','stopped','ended')),
  started_at timestamptz not null default now(),
  stopped_at timestamptz,
  last_seen_at timestamptz,
  last_lat double precision,
  last_lng double precision,
  last_accuracy_m double precision,
  last_battery_pct integer,
  last_status text,
  app_version text,
  device_model text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.sov_tracking_points (
  id uuid primary key default gen_random_uuid(),
  session_id uuid not null references public.sov_tracking_sessions(id) on delete cascade,
  trip_id uuid not null,
  user_id uuid references auth.users(id) on delete set null,
  device_id text not null,
  client_point_id uuid not null,
  lat double precision not null,
  lng double precision not null,
  recorded_at timestamptz not null,
  received_at timestamptz not null default now(),
  accuracy_m double precision,
  altitude_m double precision,
  speed_mps double precision,
  heading_deg double precision,
  battery_pct integer,
  network_state text,
  client_status text not null default 'synced' check (client_status in ('live','queued','synced','sos')),
  created_at timestamptz not null default now(),
  unique(device_id, client_point_id)
);

create index if not exists idx_sov_tracking_members_user on public.sov_tracking_trip_members(user_id);
create index if not exists idx_sov_tracking_sessions_trip_status on public.sov_tracking_sessions(trip_id, status, last_seen_at desc);
create index if not exists idx_sov_tracking_sessions_user on public.sov_tracking_sessions(user_id, started_at desc);
create index if not exists idx_sov_tracking_points_trip_time on public.sov_tracking_points(trip_id, recorded_at desc);
create index if not exists idx_sov_tracking_points_session_time on public.sov_tracking_points(session_id, recorded_at desc);
create index if not exists idx_sov_tracking_points_user_time on public.sov_tracking_points(user_id, recorded_at desc);

-- -----------------------------------------------------------------------------
-- Updated_at trigger
-- -----------------------------------------------------------------------------
create or replace function public.sov_tracking_touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_sov_tracking_sessions_touch on public.sov_tracking_sessions;
create trigger trg_sov_tracking_sessions_touch
before update on public.sov_tracking_sessions
for each row execute function public.sov_tracking_touch_updated_at();

-- -----------------------------------------------------------------------------
-- RLS
-- -----------------------------------------------------------------------------
alter table public.sov_tracking_trip_members enable row level security;
alter table public.sov_tracking_sessions enable row level security;
alter table public.sov_tracking_points enable row level security;

drop policy if exists sov_tracking_members_select on public.sov_tracking_trip_members;
create policy sov_tracking_members_select on public.sov_tracking_trip_members
for select using (user_id = auth.uid() or public.sov_tracking_is_webmaster_or_admin() or public.sov_tracking_can_view_trip(trip_id));

drop policy if exists sov_tracking_members_write on public.sov_tracking_trip_members;
create policy sov_tracking_members_write on public.sov_tracking_trip_members
for all using (public.sov_tracking_is_webmaster_or_admin()) with check (public.sov_tracking_is_webmaster_or_admin());

drop policy if exists sov_tracking_sessions_select on public.sov_tracking_sessions;
create policy sov_tracking_sessions_select on public.sov_tracking_sessions
for select using (user_id = auth.uid() or public.sov_tracking_can_view_trip(trip_id));

drop policy if exists sov_tracking_sessions_insert on public.sov_tracking_sessions;
create policy sov_tracking_sessions_insert on public.sov_tracking_sessions
for insert with check (auth.uid() = user_id and public.sov_tracking_can_track_trip(trip_id));

drop policy if exists sov_tracking_sessions_update on public.sov_tracking_sessions;
create policy sov_tracking_sessions_update on public.sov_tracking_sessions
for update using (auth.uid() = user_id or public.sov_tracking_is_webmaster_or_admin())
with check (auth.uid() = user_id or public.sov_tracking_is_webmaster_or_admin());

drop policy if exists sov_tracking_points_select on public.sov_tracking_points;
create policy sov_tracking_points_select on public.sov_tracking_points
for select using (user_id = auth.uid() or public.sov_tracking_can_view_trip(trip_id));

drop policy if exists sov_tracking_points_insert on public.sov_tracking_points;
create policy sov_tracking_points_insert on public.sov_tracking_points
for insert with check (auth.uid() = user_id and public.sov_tracking_can_track_trip(trip_id));

-- -----------------------------------------------------------------------------
-- Views
-- -----------------------------------------------------------------------------
create or replace view public.sov_tracking_latest_positions as
select distinct on (p.trip_id, coalesce(p.user_id::text, p.device_id))
  p.trip_id,
  p.session_id,
  p.user_id,
  p.device_id,
  s.display_name,
  s.role_label,
  p.lat,
  p.lng,
  p.accuracy_m,
  p.altitude_m,
  p.speed_mps,
  p.heading_deg,
  p.battery_pct,
  p.client_status,
  p.recorded_at,
  p.received_at,
  now() - p.recorded_at as age,
  case
    when p.client_status = 'sos' then 'sos'
    when p.recorded_at > now() - interval '2 minutes' then 'online'
    when p.recorded_at > now() - interval '15 minutes' then 'stale'
    else 'offline'
  end as live_status
from public.sov_tracking_points p
left join public.sov_tracking_sessions s on s.id = p.session_id
order by p.trip_id, coalesce(p.user_id::text, p.device_id), p.recorded_at desc;

-- -----------------------------------------------------------------------------
-- RPC: start/stop/batch/get
-- -----------------------------------------------------------------------------
create or replace function public.sov_tracking_start_session(
  p_trip_id uuid,
  p_device_id text,
  p_display_name text default null,
  p_app_version text default null,
  p_device_model text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
  v_session uuid;
begin
  if v_uid is null then
    raise exception 'Nisi prijavljen.';
  end if;
  if not public.sov_tracking_can_track_trip(p_trip_id) then
    raise exception 'Nemaš pravo pokrenuti tracking za ovaj izlet.';
  end if;

  insert into public.sov_tracking_sessions(trip_id,user_id,device_id,display_name,role_label,status,started_at,last_seen_at,app_version,device_model)
  values (p_trip_id, v_uid, nullif(p_device_id,''), nullif(p_display_name,''), null, 'active', now(), now(), nullif(p_app_version,''), nullif(p_device_model,''))
  returning id into v_session;

  insert into public.sov_tracking_trip_members(trip_id,user_id,role,can_track,can_view)
  values (p_trip_id, v_uid, 'participant', true, true)
  on conflict (trip_id,user_id) do update set can_track = true, can_view = true;

  return jsonb_build_object(
    'ok', true,
    'session_id', v_session,
    'tracking_interval_sec', 30,
    'low_battery_interval_sec', 90,
    'message', 'Tracking session pokrenut.'
  );
end;
$$;

create or replace function public.sov_tracking_stop_session(p_session_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_count int;
begin
  update public.sov_tracking_sessions
  set status = 'stopped', stopped_at = now(), updated_at = now()
  where id = p_session_id and (user_id = auth.uid() or public.sov_tracking_is_webmaster_or_admin());
  get diagnostics v_count = row_count;
  if v_count = 0 then raise exception 'Tracking session nije pronađen ili nemaš pravo.'; end if;
  return jsonb_build_object('ok', true, 'stopped', true);
end;
$$;

create or replace function public.sov_tracking_ingest_batch(
  p_session_id uuid,
  p_points jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_session public.sov_tracking_sessions%rowtype;
  v_item jsonb;
  v_inserted int := 0;
  v_duplicates int := 0;
  v_total int := 0;
  v_point_id uuid;
  v_last_recorded timestamptz;
  v_last_lat double precision;
  v_last_lng double precision;
  v_last_acc double precision;
  v_last_batt int;
begin
  if auth.uid() is null then raise exception 'Nisi prijavljen.'; end if;
  select * into v_session from public.sov_tracking_sessions where id = p_session_id;
  if not found then raise exception 'Tracking session ne postoji.'; end if;
  if v_session.user_id <> auth.uid() and not public.sov_tracking_is_webmaster_or_admin() then
    raise exception 'Nemaš pravo slati točke za ovu session.';
  end if;
  if not public.sov_tracking_can_track_trip(v_session.trip_id) then
    raise exception 'Nemaš pravo slati tracking za ovaj izlet.';
  end if;

  for v_item in select * from jsonb_array_elements(coalesce(p_points, '[]'::jsonb)) loop
    v_total := v_total + 1;
    v_point_id := coalesce(nullif(v_item->>'client_point_id','')::uuid, gen_random_uuid());
    begin
      insert into public.sov_tracking_points(
        session_id, trip_id, user_id, device_id, client_point_id,
        lat, lng, recorded_at, accuracy_m, altitude_m, speed_mps, heading_deg,
        battery_pct, network_state, client_status
      ) values (
        v_session.id, v_session.trip_id, v_session.user_id, v_session.device_id, v_point_id,
        (v_item->>'lat')::double precision,
        coalesce(v_item->>'lng', v_item->>'lon')::double precision,
        coalesce(nullif(v_item->>'recorded_at','')::timestamptz, now()),
        nullif(v_item->>'accuracy_m','')::double precision,
        nullif(v_item->>'altitude_m','')::double precision,
        nullif(v_item->>'speed_mps','')::double precision,
        nullif(v_item->>'heading_deg','')::double precision,
        nullif(v_item->>'battery_pct','')::int,
        nullif(v_item->>'network_state',''),
        coalesce(nullif(v_item->>'client_status',''), 'queued')
      );
      v_inserted := v_inserted + 1;
    exception when unique_violation then
      v_duplicates := v_duplicates + 1;
    end;
  end loop;

  select p.recorded_at, p.lat, p.lng, p.accuracy_m, p.battery_pct
  into v_last_recorded, v_last_lat, v_last_lng, v_last_acc, v_last_batt
  from public.sov_tracking_points p
  where p.session_id = v_session.id
  order by p.recorded_at desc
  limit 1;

  update public.sov_tracking_sessions
  set last_seen_at = coalesce(v_last_recorded, now()),
      last_lat = coalesce(v_last_lat, last_lat),
      last_lng = coalesce(v_last_lng, last_lng),
      last_accuracy_m = coalesce(v_last_acc, last_accuracy_m),
      last_battery_pct = coalesce(v_last_batt, last_battery_pct),
      last_status = case when v_inserted > 0 then 'synced' else last_status end,
      updated_at = now()
  where id = v_session.id;

  return jsonb_build_object(
    'ok', true,
    'received', v_total,
    'inserted', v_inserted,
    'duplicates', v_duplicates,
    'server_time', now()
  );
end;
$$;

create or replace function public.sov_tracking_get_latest_positions(p_trip_id uuid)
returns setof public.sov_tracking_latest_positions
language sql
security definer
set search_path = public, auth
as $$
  select * from public.sov_tracking_latest_positions
  where trip_id = p_trip_id and public.sov_tracking_can_view_trip(p_trip_id)
  order by recorded_at desc;
$$;

create or replace function public.sov_tracking_get_trip_points(
  p_trip_id uuid,
  p_hours integer default 6,
  p_user_id uuid default null
)
returns table(
  session_id uuid,
  user_id uuid,
  device_id text,
  display_name text,
  lat double precision,
  lng double precision,
  recorded_at timestamptz,
  accuracy_m double precision,
  altitude_m double precision,
  speed_mps double precision,
  heading_deg double precision,
  battery_pct integer,
  client_status text
)
language sql
security definer
set search_path = public, auth
as $$
  select
    p.session_id, p.user_id, p.device_id, s.display_name,
    p.lat, p.lng, p.recorded_at, p.accuracy_m, p.altitude_m, p.speed_mps, p.heading_deg, p.battery_pct, p.client_status
  from public.sov_tracking_points p
  left join public.sov_tracking_sessions s on s.id = p.session_id
  where p.trip_id = p_trip_id
    and public.sov_tracking_can_view_trip(p_trip_id)
    and (p_user_id is null or p.user_id = p_user_id)
    and (coalesce(p_hours,0) <= 0 or p.recorded_at >= now() - make_interval(hours => p_hours))
  order by p.user_id nulls last, p.device_id, p.recorded_at asc;
$$;

create or replace function public.sov_tracking_cleanup_old_points(p_keep_days integer default 90)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_deleted int;
begin
  if not public.sov_tracking_is_webmaster_or_admin() then raise exception 'Samo Webmaster/Admin.'; end if;
  delete from public.sov_tracking_points where recorded_at < now() - make_interval(days => greatest(coalesce(p_keep_days,90), 7));
  get diagnostics v_deleted = row_count;
  return jsonb_build_object('ok', true, 'deleted', v_deleted);
end;
$$;

create or replace view public.sov_tracking_sync_status as
select
  'sov_field_tracking_lite_v5_59_0'::text as module,
  (select count(*) from public.sov_tracking_sessions) as sessions_total,
  (select count(*) from public.sov_tracking_sessions where status='active') as sessions_active,
  (select count(*) from public.sov_tracking_points where recorded_at > now() - interval '24 hours') as points_24h,
  now() as checked_at;

grant execute on function public.sov_tracking_start_session(uuid,text,text,text,text) to authenticated;
grant execute on function public.sov_tracking_stop_session(uuid) to authenticated;
grant execute on function public.sov_tracking_ingest_batch(uuid,jsonb) to authenticated;
grant execute on function public.sov_tracking_get_latest_positions(uuid) to authenticated;
grant execute on function public.sov_tracking_get_trip_points(uuid,integer,uuid) to authenticated;
grant execute on function public.sov_tracking_cleanup_old_points(integer) to authenticated;
grant select on public.sov_tracking_latest_positions to authenticated;
grant select on public.sov_tracking_sync_status to authenticated;
