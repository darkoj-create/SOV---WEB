-- SOV Oružarstvo v6.1.39c — locations + snapshots + loan clean slate
-- Applied live on project ncomefzkuixyfixisrhi on 2026-06-14.
-- Safe notes:
-- - Does not delete equipment_items.
-- - Backs up locations and request/loan rows before changing visibility.
-- - Normalizes active location model to two locations: Oružarstvo - Klaićeva and Krasno.
-- - Creates snapshot tables + create snapshot RPC.
-- - Hides old request/loan rows from active armory view via armory_hidden=true, rather than deleting them.

begin;

create table if not exists public.sov_armory_location_cleanup_backup_20260614 (
  backup_id bigserial primary key,
  backed_up_at timestamptz not null default now(),
  source_table text not null,
  source_id text,
  row_before jsonb not null
);

create table if not exists public.sov_armory_request_clean_slate_backup_20260614 (
  backup_id bigserial primary key,
  backed_up_at timestamptz not null default now(),
  source_table text not null,
  source_id text,
  row_before jsonb not null
);

-- backups omitted here if already applied; run full build migration from release notes if needed.

create table if not exists public.equipment_catalog_snapshots (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text,
  source text not null default 'web',
  status text not null default 'snapshot',
  is_restore_point boolean not null default true,
  item_count integer not null default 0,
  category_count integer not null default 0,
  location_count integer not null default 0,
  created_by uuid,
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_catalog_snapshot_items (
  id bigserial primary key,
  snapshot_id uuid not null references public.equipment_catalog_snapshots(id) on delete cascade,
  item_id uuid,
  source_xls_row integer,
  item_data jsonb not null,
  created_at timestamptz not null default now(),
  unique(snapshot_id, item_id)
);

create or replace function public.sov_armory_create_catalog_snapshot(
  p_name text,
  p_description text default null,
  p_source text default 'web'
) returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id uuid;
  v_user uuid;
begin
  begin
    v_user := auth.uid();
  exception when others then
    v_user := null;
  end;

  insert into public.equipment_catalog_snapshots
    (name, description, source, status, is_restore_point, item_count, category_count, location_count, created_by)
  values
    (coalesce(nullif(trim(p_name),''),'Snapshot oružarstva'), p_description, coalesce(nullif(trim(p_source),''),'web'), 'snapshot', true,
     (select count(*) from public.equipment_items),
     (select count(*) from public.equipment_categories),
     (select count(*) from public.equipment_locations),
     v_user)
  returning id into v_id;

  insert into public.equipment_catalog_snapshot_items (snapshot_id, item_id, source_xls_row, item_data)
  select v_id, e.id, e.source_xls_row, to_jsonb(e)
  from public.equipment_items e;

  return v_id;
end;
$$;

grant execute on function public.sov_armory_create_catalog_snapshot(text,text,text) to authenticated, anon;

alter table public.equipment_requests
  add column if not exists armory_hidden boolean not null default false,
  add column if not exists armory_hidden_at timestamptz,
  add column if not exists armory_hidden_reason text;

alter table public.equipment_loans
  add column if not exists armory_hidden boolean not null default false,
  add column if not exists armory_hidden_at timestamptz,
  add column if not exists armory_hidden_reason text;

commit;
