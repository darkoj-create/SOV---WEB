-- SOV Web v5.36.3 — Oružarstvo schema-tolerant fix
-- Run in Supabase SQL editor with RLS enabled. Safe/idempotent.
-- Purpose: repair v5.36.x on existing armory schemas that do not yet have sku / canonical columns.

create extension if not exists pgcrypto;

-- -------------------------------------------------------------------
-- Role helpers: keep existing function signatures stable.
-- -------------------------------------------------------------------
create or replace function public.sov_current_profile_role()
returns text
language sql
security definer
set search_path = public
as $$
  select coalesce((select p.role::text from public.profiles p where p.id = auth.uid()), 'user')
$$;

create or replace function public.sov_is_approved()
returns boolean
language sql
security definer
set search_path = public
as $$
  select coalesce((select p.status::text = 'approved' from public.profiles p where p.id = auth.uid()), false)
$$;

create or replace function public.sov_has_role(required_roles text[])
returns boolean
language sql
security definer
set search_path = public
as $$
  select public.sov_is_approved() and public.sov_current_profile_role() = any(required_roles)
$$;

create or replace function public.sov_can_manage_equipment()
returns boolean
language sql
security definer
set search_path = public
as $$
  select public.sov_has_role(array['admin','oruzar'])
$$;

-- -------------------------------------------------------------------
-- Ensure required tables exist. These are additive only.
-- -------------------------------------------------------------------
create table if not exists public.sov_audit_log (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid default auth.uid(),
  actor_email text,
  action text not null,
  entity_type text,
  entity_id text,
  before_data jsonb,
  after_data jsonb,
  metadata jsonb default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_categories (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_locations (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_items (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_pieces (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_ropes (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_requests (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_request_items (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_loans (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_loan_items (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_service_tasks (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_disposals (id uuid primary key default gen_random_uuid());
create table if not exists public.equipment_audit_log (id uuid primary key default gen_random_uuid());
create table if not exists public.inventory_sessions (id uuid primary key default gen_random_uuid());

-- -------------------------------------------------------------------
-- Add canonical columns if the current DB was created by older armory SQL.
-- -------------------------------------------------------------------
alter table public.equipment_categories
  add column if not exists name text,
  add column if not exists sort_order int default 0,
  add column if not exists created_at timestamptz not null default now();

alter table public.equipment_locations
  add column if not exists name text,
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now();

alter table public.equipment_items
  add column if not exists legacy_id text,
  add column if not exists sku text,
  add column if not exists name text,
  add column if not exists category_id uuid,
  add column if not exists category_name text,
  add column if not exists subcategory text,
  add column if not exists manufacturer text,
  add column if not exists model text,
  add column if not exists quantity int not null default 0,
  add column if not exists available int not null default 0,
  add column if not exists loaned int not null default 0,
  add column if not exists unit text default 'kom',
  add column if not exists location_id uuid,
  add column if not exists location_name text,
  add column if not exists status text not null default 'U društvu',
  add column if not exists availability text not null default 'dostupno',
  add column if not exists tracking_type text default 'po vrsti',
  add column if not exists member_visible boolean not null default true,
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

-- Backfill sku/code-like field where possible without assuming old column names exist.
do $$
begin
  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='equipment_items' and column_name='item_code') then
    execute 'update public.equipment_items set sku = coalesce(sku, item_code::text) where sku is null';
  elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='equipment_items' and column_name='code') then
    execute 'update public.equipment_items set sku = coalesce(sku, code::text) where sku is null';
  elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='equipment_items' and column_name='inventory_id') then
    execute 'update public.equipment_items set sku = coalesce(sku, inventory_id::text) where sku is null';
  end if;
end $$;

alter table public.equipment_pieces
  add column if not exists equipment_item_id uuid,
  add column if not exists legacy_id text,
  add column if not exists sku text,
  add column if not exists serial_number text,
  add column if not exists name text,
  add column if not exists category_name text,
  add column if not exists status text not null default 'U društvu',
  add column if not exists availability text not null default 'dostupno',
  add column if not exists location_id uuid,
  add column if not exists location_name text,
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

alter table public.equipment_ropes
  add column if not exists legacy_id text,
  add column if not exists sku text,
  add column if not exists name text,
  add column if not exists length_m numeric,
  add column if not exists diameter_mm text,
  add column if not exists manufacturer text,
  add column if not exists model text,
  add column if not exists status text not null default 'U društvu',
  add column if not exists availability text not null default 'dostupno',
  add column if not exists location_name text,
  add column if not exists next_service date,
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

alter table public.equipment_requests
  add column if not exists requester_id uuid default auth.uid(),
  add column if not exists requester_email text,
  add column if not exists requester_name text,
  add column if not exists trip text,
  add column if not exists date_from date,
  add column if not exists date_to date,
  add column if not exists note text,
  add column if not exists status text not null default 'pending',
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

alter table public.equipment_request_items
  add column if not exists request_id uuid,
  add column if not exists equipment_item_id uuid,
  add column if not exists equipment_legacy_id text,
  add column if not exists name text,
  add column if not exists quantity int not null default 1,
  add column if not exists unit text default 'kom',
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now();

alter table public.equipment_loans
  add column if not exists request_id uuid,
  add column if not exists user_id uuid,
  add column if not exists user_email text,
  add column if not exists user_name text,
  add column if not exists status text not null default 'issued',
  add column if not exists issued_at timestamptz default now(),
  add column if not exists returned_at timestamptz,
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

alter table public.equipment_loan_items
  add column if not exists loan_id uuid,
  add column if not exists equipment_item_id uuid,
  add column if not exists equipment_piece_id uuid,
  add column if not exists rope_id uuid,
  add column if not exists equipment_legacy_id text,
  add column if not exists name text,
  add column if not exists quantity int not null default 1,
  add column if not exists returned_quantity int not null default 0,
  add column if not exists return_status text,
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now();

alter table public.equipment_service_tasks
  add column if not exists equipment_item_id uuid,
  add column if not exists equipment_piece_id uuid,
  add column if not exists rope_id uuid,
  add column if not exists title text,
  add column if not exists status text not null default 'open',
  add column if not exists due_date date,
  add column if not exists note text,
  add column if not exists created_by uuid default auth.uid(),
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

alter table public.equipment_disposals
  add column if not exists equipment_item_id uuid,
  add column if not exists equipment_piece_id uuid,
  add column if not exists rope_id uuid,
  add column if not exists reason text,
  add column if not exists disposed_by uuid default auth.uid(),
  add column if not exists disposed_at timestamptz not null default now(),
  add column if not exists note text;

alter table public.equipment_audit_log
  add column if not exists actor_id uuid default auth.uid(),
  add column if not exists action text,
  add column if not exists entity_type text,
  add column if not exists entity_id text,
  add column if not exists before_data jsonb,
  add column if not exists after_data jsonb,
  add column if not exists created_at timestamptz not null default now();

alter table public.inventory_sessions
  add column if not exists created_at timestamptz not null default now();

-- Reasonable non-null backfills for older partially-null rows.
update public.equipment_items set name = coalesce(name, sku, legacy_id, id::text) where name is null;
update public.equipment_ropes set name = coalesce(name, sku, legacy_id, id::text) where name is null;
update public.equipment_service_tasks set title = coalesce(title, 'Servis opreme') where title is null;

-- -------------------------------------------------------------------
-- RLS stays enabled. Policies are recreated idempotently.
-- -------------------------------------------------------------------
do $$
declare t text;
begin
  foreach t in array array[
    'sov_audit_log','equipment_categories','equipment_locations','equipment_items','equipment_pieces','equipment_ropes',
    'equipment_requests','equipment_request_items','equipment_loans','equipment_loan_items',
    'equipment_service_tasks','equipment_disposals','equipment_audit_log','inventory_sessions'
  ] loop
    execute format('alter table public.%I enable row level security', t);
  end loop;
end $$;

drop policy if exists "Admin reads sov audit" on public.sov_audit_log;
create policy "Admin reads sov audit" on public.sov_audit_log for select using (public.sov_has_role(array['admin']));
drop policy if exists "Internal inserts sov audit" on public.sov_audit_log;
create policy "Internal inserts sov audit" on public.sov_audit_log for insert with check (auth.uid() is not null);

drop policy if exists "Approved read equipment categories" on public.equipment_categories;
create policy "Approved read equipment categories" on public.equipment_categories for select using (public.sov_is_approved());
drop policy if exists "Approved read equipment locations" on public.equipment_locations;
create policy "Approved read equipment locations" on public.equipment_locations for select using (public.sov_is_approved());
drop policy if exists "Approved read visible equipment items" on public.equipment_items;
create policy "Approved read visible equipment items" on public.equipment_items for select using (public.sov_is_approved() and (member_visible = true or public.sov_can_manage_equipment()));
drop policy if exists "Approved read equipment ropes" on public.equipment_ropes;
create policy "Approved read equipment ropes" on public.equipment_ropes for select using (public.sov_is_approved());
drop policy if exists "Armory read equipment pieces" on public.equipment_pieces;
create policy "Armory read equipment pieces" on public.equipment_pieces for select using (public.sov_can_manage_equipment());

drop policy if exists "Users insert own equipment requests" on public.equipment_requests;
create policy "Users insert own equipment requests" on public.equipment_requests for insert with check (public.sov_is_approved() and (requester_id = auth.uid() or requester_id is null));
drop policy if exists "Users read own equipment requests" on public.equipment_requests;
create policy "Users read own equipment requests" on public.equipment_requests for select using (requester_id = auth.uid() or public.sov_can_manage_equipment());
drop policy if exists "Armory updates equipment requests" on public.equipment_requests;
create policy "Armory updates equipment requests" on public.equipment_requests for update using (public.sov_can_manage_equipment()) with check (public.sov_can_manage_equipment());

drop policy if exists "Users insert own request items" on public.equipment_request_items;
create policy "Users insert own request items" on public.equipment_request_items for insert with check (
  exists (select 1 from public.equipment_requests r where r.id = request_id and (r.requester_id = auth.uid() or r.requester_id is null))
  or public.sov_can_manage_equipment()
);
drop policy if exists "Users read own request items" on public.equipment_request_items;
create policy "Users read own request items" on public.equipment_request_items for select using (
  exists (select 1 from public.equipment_requests r where r.id = request_id and r.requester_id = auth.uid())
  or public.sov_can_manage_equipment()
);
drop policy if exists "Armory updates request items" on public.equipment_request_items;
create policy "Armory updates request items" on public.equipment_request_items for update using (public.sov_can_manage_equipment()) with check (public.sov_can_manage_equipment());

do $$
declare t text;
begin
  foreach t in array array[
    'equipment_categories','equipment_locations','equipment_items','equipment_pieces','equipment_ropes',
    'equipment_loans','equipment_loan_items','equipment_service_tasks','equipment_disposals','equipment_audit_log','inventory_sessions'
  ] loop
    execute format('drop policy if exists "Armory full access" on public.%I', t);
    execute format('create policy "Armory full access" on public.%I for all using (public.sov_can_manage_equipment()) with check (public.sov_can_manage_equipment())', t);
  end loop;
end $$;

-- -------------------------------------------------------------------
-- Dashboard view. Drop/recreate avoids old column-order rename errors.
-- -------------------------------------------------------------------
drop view if exists public.equipment_dashboard_summary cascade;
create view public.equipment_dashboard_summary as
select
  (select count(*) from public.equipment_items where member_visible = true) as equipment_count,
  (select coalesce(sum(available),0) from public.equipment_items where member_visible = true) as available_total,
  (select count(*) from public.equipment_ropes) as ropes_count,
  (select coalesce(sum(length_m),0) from public.equipment_ropes) as ropes_total_m,
  (select count(*) from public.equipment_requests where status in ('pending','approved','prepared','issued')) as open_request_count,
  (select count(*) from public.equipment_loans where status not in ('returned','closed')) as open_loan_count,
  (select count(*) from public.inventory_sessions) as inventory_count,
  (select count(*) from public.equipment_items where available <= 0 and member_visible = true) as unavailable_count,
  (select count(*) from public.equipment_service_tasks where status not in ('closed','done')) as open_service_count,
  (select count(*) from public.equipment_service_tasks where status not in ('closed','done')) as service_count;

-- Indexes only after missing columns are guaranteed.
create index if not exists equipment_items_legacy_idx on public.equipment_items(legacy_id);
create index if not exists equipment_items_sku_idx on public.equipment_items(sku);
create index if not exists equipment_items_category_idx on public.equipment_items(category_name);
create index if not exists equipment_requests_requester_idx on public.equipment_requests(requester_id);
create index if not exists equipment_requests_status_idx on public.equipment_requests(status);

-- -------------------------------------------------------------------
-- Audit triggers.
-- -------------------------------------------------------------------
create or replace function public.sov_log_equipment_audit()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  action_name text;
  entity text := TG_TABLE_NAME;
  row_id text;
begin
  action_name := lower(TG_OP);
  row_id := coalesce((case when TG_OP='DELETE' then old.id else new.id end)::text, null);

  insert into public.sov_audit_log(action, entity_type, entity_id, before_data, after_data, metadata)
  values (
    'equipment_' || action_name,
    entity,
    row_id,
    case when TG_OP in ('UPDATE','DELETE') then to_jsonb(old) else null end,
    case when TG_OP in ('INSERT','UPDATE') then to_jsonb(new) else null end,
    jsonb_build_object('module','oruzarstvo','source','web_v5_36_3')
  );

  insert into public.equipment_audit_log(action, entity_type, entity_id, before_data, after_data)
  values (
    action_name,
    entity,
    row_id,
    case when TG_OP in ('UPDATE','DELETE') then to_jsonb(old) else null end,
    case when TG_OP in ('INSERT','UPDATE') then to_jsonb(new) else null end
  );

  if TG_OP = 'DELETE' then return old; end if;
  return new;
end $$;

do $$
declare t text;
begin
  foreach t in array array[
    'equipment_items','equipment_pieces','equipment_ropes','equipment_requests','equipment_request_items',
    'equipment_loans','equipment_loan_items','equipment_service_tasks','equipment_disposals'
  ] loop
    execute format('drop trigger if exists sov_equipment_audit_trg on public.%I', t);
    execute format('create trigger sov_equipment_audit_trg after insert or update or delete on public.%I for each row execute function public.sov_log_equipment_audit()', t);
  end loop;
end $$;

-- Done.
