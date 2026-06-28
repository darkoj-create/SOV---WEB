-- SOV Oružarstvo v5.48.1 — FAST MANIFEST CACHE
-- Run after v5.48 if web sync feels slow.
-- RLS stays enabled. This does not delete armory data.
-- Problem fixed: v5.48 manifest was a view that counted heavy catalog views on every page open.
-- This replaces it with a tiny one-row table that web/APK can read instantly.

-- Drop only if the old object is a VIEW. If it is already a table, keep it.
do $$
begin
  if exists (
    select 1
    from pg_class c
    join pg_namespace n on n.oid = c.relnamespace
    where n.nspname='public'
      and c.relname='sov_equipment_catalog_manifest'
      and c.relkind='v'
  ) then
    execute 'drop view public.sov_equipment_catalog_manifest';
  end if;
end $$;

create table if not exists public.sov_equipment_catalog_manifest (
  id boolean primary key default true,
  catalog_version text not null default md5(now()::text),
  raw_row_count integer not null default 0,
  grouped_row_count integer not null default 0,
  equipment_items_count integer not null default 0,
  equipment_ropes_count integer not null default 0,
  equipment_pieces_count integer not null default 0,
  last_changed_at timestamptz not null default now(),
  requests_changed_at timestamptz not null default now(),
  checked_at timestamptz not null default now()
);

alter table public.sov_equipment_catalog_manifest disable row level security;
grant select on public.sov_equipment_catalog_manifest to anon, authenticated;

drop function if exists public.sov_rebuild_equipment_catalog_manifest();
create or replace function public.sov_rebuild_equipment_catalog_manifest()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_items int := 0;
  v_ropes int := 0;
  v_pieces int := 0;
  v_raw int := 0;
  v_grouped int := 0;
  v_catalog_changed timestamptz := now();
  v_requests_changed timestamptz := now();
begin
  select count(*)::int, coalesce(max(updated_at),'epoch'::timestamptz) into v_items, v_catalog_changed from public.equipment_items;
  select count(*)::int into v_ropes from public.equipment_ropes;
  select count(*)::int into v_pieces from public.equipment_pieces;
  select greatest(
    v_catalog_changed,
    coalesce((select max(updated_at) from public.equipment_ropes),'epoch'::timestamptz),
    coalesce((select max(updated_at) from public.equipment_pieces),'epoch'::timestamptz)
  ) into v_catalog_changed;
  select greatest(
    coalesce((select max(updated_at) from public.equipment_requests),'epoch'::timestamptz),
    coalesce((select max(updated_at) from public.equipment_request_items),'epoch'::timestamptz)
  ) into v_requests_changed;

  begin
    select count(*)::int into v_raw from public.sov_equipment_app_catalog;
  exception when others then
    v_raw := v_items + v_ropes + v_pieces;
  end;
  begin
    select count(*)::int into v_grouped from public.sov_equipment_app_catalog_grouped;
  exception when others then
    v_grouped := v_raw;
  end;

  insert into public.sov_equipment_catalog_manifest (
    id, catalog_version, raw_row_count, grouped_row_count,
    equipment_items_count, equipment_ropes_count, equipment_pieces_count,
    last_changed_at, requests_changed_at, checked_at
  ) values (
    true,
    md5(concat_ws('|', v_items, v_ropes, v_pieces, v_raw, v_grouped, v_catalog_changed::text)),
    v_raw, v_grouped, v_items, v_ropes, v_pieces, v_catalog_changed, v_requests_changed, now()
  )
  on conflict (id) do update set
    catalog_version = excluded.catalog_version,
    raw_row_count = excluded.raw_row_count,
    grouped_row_count = excluded.grouped_row_count,
    equipment_items_count = excluded.equipment_items_count,
    equipment_ropes_count = excluded.equipment_ropes_count,
    equipment_pieces_count = excluded.equipment_pieces_count,
    last_changed_at = excluded.last_changed_at,
    requests_changed_at = excluded.requests_changed_at,
    checked_at = excluded.checked_at;
end;
$$;

create or replace function public.sov_mark_equipment_catalog_manifest_dirty()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.sov_equipment_catalog_manifest(id, catalog_version, last_changed_at, checked_at)
  values (true, md5(clock_timestamp()::text), now(), now())
  on conflict (id) do update set
    catalog_version = md5(coalesce(public.sov_equipment_catalog_manifest.catalog_version,'') || '|' || clock_timestamp()::text),
    last_changed_at = now(),
    checked_at = now();
  return coalesce(new, old);
end;
$$;

create or replace function public.sov_mark_equipment_requests_manifest_dirty()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.sov_equipment_catalog_manifest(id, catalog_version, requests_changed_at, checked_at)
  values (true, md5(clock_timestamp()::text), now(), now())
  on conflict (id) do update set
    requests_changed_at = now(),
    checked_at = now();
  return coalesce(new, old);
end;
$$;

drop trigger if exists trg_sov_equipment_manifest_items_dirty on public.equipment_items;
create trigger trg_sov_equipment_manifest_items_dirty
after insert or update or delete on public.equipment_items
for each statement execute function public.sov_mark_equipment_catalog_manifest_dirty();

drop trigger if exists trg_sov_equipment_manifest_ropes_dirty on public.equipment_ropes;
create trigger trg_sov_equipment_manifest_ropes_dirty
after insert or update or delete on public.equipment_ropes
for each statement execute function public.sov_mark_equipment_catalog_manifest_dirty();

drop trigger if exists trg_sov_equipment_manifest_pieces_dirty on public.equipment_pieces;
create trigger trg_sov_equipment_manifest_pieces_dirty
after insert or update or delete on public.equipment_pieces
for each statement execute function public.sov_mark_equipment_catalog_manifest_dirty();

drop trigger if exists trg_sov_equipment_manifest_requests_dirty on public.equipment_requests;
create trigger trg_sov_equipment_manifest_requests_dirty
after insert or update or delete on public.equipment_requests
for each statement execute function public.sov_mark_equipment_requests_manifest_dirty();

drop trigger if exists trg_sov_equipment_manifest_request_items_dirty on public.equipment_request_items;
create trigger trg_sov_equipment_manifest_request_items_dirty
after insert or update or delete on public.equipment_request_items
for each statement execute function public.sov_mark_equipment_requests_manifest_dirty();

select public.sov_rebuild_equipment_catalog_manifest();

-- Quick check:
-- select * from public.sov_equipment_catalog_manifest;
