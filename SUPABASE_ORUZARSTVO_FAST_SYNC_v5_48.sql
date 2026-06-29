-- SOV Oružarstvo v5.48 — FAST SYNC / CATALOG MANIFEST
-- Run after v5.47.3 read-layer stabilize. RLS stays enabled.
-- Purpose: app/web can check a tiny manifest before downloading the whole armory catalog.
-- It does NOT delete data and does NOT switch source of truth again.

create extension if not exists pgcrypto;

-- Ensure legacy rows have an updated_at clock so the manifest can change when catalog data changes.
alter table public.equipment_items add column if not exists updated_at timestamptz not null default now();
alter table public.equipment_ropes add column if not exists updated_at timestamptz not null default now();
alter table public.equipment_pieces add column if not exists updated_at timestamptz not null default now();
alter table public.equipment_requests add column if not exists updated_at timestamptz not null default now();
alter table public.equipment_request_items add column if not exists updated_at timestamptz not null default now();

create or replace function public.sov_touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_sov_touch_equipment_items_updated_at on public.equipment_items;
create trigger trg_sov_touch_equipment_items_updated_at
before update on public.equipment_items
for each row execute function public.sov_touch_updated_at();

drop trigger if exists trg_sov_touch_equipment_ropes_updated_at on public.equipment_ropes;
create trigger trg_sov_touch_equipment_ropes_updated_at
before update on public.equipment_ropes
for each row execute function public.sov_touch_updated_at();

drop trigger if exists trg_sov_touch_equipment_pieces_updated_at on public.equipment_pieces;
create trigger trg_sov_touch_equipment_pieces_updated_at
before update on public.equipment_pieces
for each row execute function public.sov_touch_updated_at();

drop trigger if exists trg_sov_touch_equipment_requests_updated_at on public.equipment_requests;
create trigger trg_sov_touch_equipment_requests_updated_at
before update on public.equipment_requests
for each row execute function public.sov_touch_updated_at();

drop trigger if exists trg_sov_touch_equipment_request_items_updated_at on public.equipment_request_items;
create trigger trg_sov_touch_equipment_request_items_updated_at
before update on public.equipment_request_items
for each row execute function public.sov_touch_updated_at();

-- Tiny catalog manifest. APK/web fetch this first. If catalog_version is unchanged,
-- they keep local cached catalog and refresh only requests/queue.
create or replace view public.sov_equipment_catalog_manifest as
with legacy as (
  select
    (select count(*)::int from public.equipment_items) as equipment_items_count,
    (select count(*)::int from public.equipment_ropes) as equipment_ropes_count,
    (select count(*)::int from public.equipment_pieces) as equipment_pieces_count,
    greatest(
      coalesce((select max(updated_at) from public.equipment_items), 'epoch'::timestamptz),
      coalesce((select max(updated_at) from public.equipment_ropes), 'epoch'::timestamptz),
      coalesce((select max(updated_at) from public.equipment_pieces), 'epoch'::timestamptz)
    ) as catalog_changed_at,
    greatest(
      coalesce((select max(updated_at) from public.equipment_requests), 'epoch'::timestamptz),
      coalesce((select max(updated_at) from public.equipment_request_items), 'epoch'::timestamptz)
    ) as requests_changed_at
), views as (
  select
    (select count(*)::int from public.sov_equipment_app_catalog) as raw_row_count,
    (select count(*)::int from public.sov_equipment_app_catalog_grouped) as grouped_row_count
)
select
  md5(concat_ws('|',
    legacy.equipment_items_count,
    legacy.equipment_ropes_count,
    legacy.equipment_pieces_count,
    views.raw_row_count,
    views.grouped_row_count,
    legacy.catalog_changed_at::text
  )) as catalog_version,
  views.raw_row_count,
  views.grouped_row_count,
  legacy.equipment_items_count,
  legacy.equipment_ropes_count,
  legacy.equipment_pieces_count,
  legacy.catalog_changed_at as last_changed_at,
  legacy.requests_changed_at,
  now() as checked_at
from legacy, views;

grant select on public.sov_equipment_catalog_manifest to authenticated;

-- Quick check:
-- select * from public.sov_equipment_catalog_manifest;
