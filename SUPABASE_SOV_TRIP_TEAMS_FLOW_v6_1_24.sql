-- SOV v6.1.24 / APK 1.4.16v — Trip teams flow
-- Purpose: simple shared web/APK team layout for each trip.
-- This patch only adds team tables/RPCs. It does not replace trips list/save/delete logic.

create extension if not exists pgcrypto;

create table if not exists public.sov_trip_teams (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid not null references public.sov_trips(id) on delete cascade,
  name text not null default 'Ekipa',
  leader_user_id uuid null,
  leader_name text not null default '',
  members_text text not null default '',
  note text not null default '',
  sort_order integer not null default 0,
  created_by uuid null default auth.uid(),
  updated_by uuid null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  meta jsonb not null default '{}'::jsonb
);

create index if not exists sov_trip_teams_trip_idx on public.sov_trip_teams(trip_id, sort_order, created_at);

alter table public.sov_trip_teams enable row level security;

drop policy if exists sov_trip_teams_select_auth on public.sov_trip_teams;
create policy sov_trip_teams_select_auth on public.sov_trip_teams
  for select to authenticated using (true);

drop policy if exists sov_trip_teams_insert_auth on public.sov_trip_teams;
create policy sov_trip_teams_insert_auth on public.sov_trip_teams
  for insert to authenticated with check (true);

drop policy if exists sov_trip_teams_update_auth on public.sov_trip_teams;
create policy sov_trip_teams_update_auth on public.sov_trip_teams
  for update to authenticated using (true) with check (true);

drop policy if exists sov_trip_teams_delete_auth on public.sov_trip_teams;
create policy sov_trip_teams_delete_auth on public.sov_trip_teams
  for delete to authenticated using (true);

create or replace function public.sov_trip_teams_touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  new.updated_by := auth.uid();
  return new;
end;
$$;

drop trigger if exists sov_trip_teams_touch_updated_at on public.sov_trip_teams;
create trigger sov_trip_teams_touch_updated_at
before update on public.sov_trip_teams
for each row execute function public.sov_trip_teams_touch_updated_at();

create or replace function public.sov_list_trip_teams(p_trip_id uuid)
returns table (
  id uuid,
  trip_id uuid,
  name text,
  leader_user_id uuid,
  leader_name text,
  members_text text,
  note text,
  sort_order integer,
  created_at timestamptz,
  updated_at timestamptz,
  meta jsonb
)
language sql
security definer
set search_path = public
as $$
  select
    t.id, t.trip_id, t.name, t.leader_user_id, t.leader_name,
    t.members_text, t.note, t.sort_order, t.created_at, t.updated_at, t.meta
  from public.sov_trip_teams t
  where t.trip_id = p_trip_id
  order by t.sort_order asc, t.created_at asc;
$$;

create or replace function public.sov_save_trip_team(
  p_trip_id uuid,
  p_team_id uuid default null,
  p_payload jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id uuid;
  v_name text := coalesce(nullif(trim(p_payload->>'name'), ''), 'Ekipa');
  v_leader_name text := coalesce(trim(p_payload->>'leader_name'), '');
  v_members_text text := coalesce(p_payload->>'members_text', '');
  v_note text := coalesce(p_payload->>'note', '');
  v_sort integer := coalesce((p_payload->>'sort_order')::integer, 0);
begin
  if p_trip_id is null then
    raise exception 'trip_id is required';
  end if;

  if not exists (select 1 from public.sov_trips s where s.id = p_trip_id) then
    raise exception 'Trip does not exist';
  end if;

  if p_team_id is null then
    insert into public.sov_trip_teams(trip_id, name, leader_name, members_text, note, sort_order, created_by, meta)
    values (p_trip_id, v_name, v_leader_name, v_members_text, v_note, v_sort, auth.uid(), coalesce(p_payload->'meta','{}'::jsonb))
    returning id into v_id;
  else
    update public.sov_trip_teams
    set name = v_name,
        leader_name = v_leader_name,
        members_text = v_members_text,
        note = v_note,
        sort_order = v_sort,
        meta = coalesce(p_payload->'meta', meta)
    where id = p_team_id and trip_id = p_trip_id
    returning id into v_id;

    if v_id is null then
      raise exception 'Team not found';
    end if;
  end if;

  return (
    select to_jsonb(x) from (
      select id, trip_id, name, leader_name, members_text, note, sort_order, created_at, updated_at
      from public.sov_trip_teams where id = v_id
    ) x
  );
end;
$$;

create or replace function public.sov_delete_trip_team(p_team_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_deleted integer := 0;
begin
  delete from public.sov_trip_teams where id = p_team_id;
  get diagnostics v_deleted = row_count;
  return jsonb_build_object('deleted', v_deleted > 0, 'id', p_team_id, 'count', v_deleted);
end;
$$;

grant usage on schema public to authenticated;
grant select, insert, update, delete on public.sov_trip_teams to authenticated;
grant execute on function public.sov_list_trip_teams(uuid) to authenticated;
grant execute on function public.sov_save_trip_team(uuid, uuid, jsonb) to authenticated;
grant execute on function public.sov_delete_trip_team(uuid) to authenticated;
