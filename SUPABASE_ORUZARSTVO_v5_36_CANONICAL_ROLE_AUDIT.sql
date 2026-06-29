-- SOV Web v5.36 — Oružarstvo canonical role/audit integration
-- Safe/idempotent patch. Does not touch SOV object/base tables.

create extension if not exists pgcrypto;

-- -------------------------------------------------------------------
-- 1) Role helpers, tolerant to existing public.profiles schema.
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

create or replace function public.sov_has_role(roles text[])
returns boolean
language sql
security definer
set search_path = public
as $$
  select public.sov_is_approved() and public.sov_current_profile_role() = any(roles)
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
-- 2) Audit table fallback. v5.35 already creates this, but keep safe.
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

alter table public.sov_audit_log enable row level security;
drop policy if exists "Admin reads sov audit" on public.sov_audit_log;
create policy "Admin reads sov audit" on public.sov_audit_log
for select using (public.sov_has_role(array['admin']));

drop policy if exists "Internal inserts sov audit" on public.sov_audit_log;
create policy "Internal inserts sov audit" on public.sov_audit_log
for insert with check (auth.uid() is not null);

-- -------------------------------------------------------------------
-- 3) Canonical armory core tables. These are create-if-missing only.
-- -------------------------------------------------------------------
create table if not exists public.equipment_categories (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  sort_order int default 0,
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_locations (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  note text,
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_items (
  id uuid primary key default gen_random_uuid(),
  legacy_id text,
  sku text,
  name text not null,
  category_id uuid references public.equipment_categories(id) on delete set null,
  category_name text,
  subcategory text,
  manufacturer text,
  model text,
  quantity int not null default 0,
  available int not null default 0,
  loaned int not null default 0,
  unit text default 'kom',
  location_id uuid references public.equipment_locations(id) on delete set null,
  location_name text,
  status text not null default 'U društvu',
  availability text not null default 'dostupno',
  tracking_type text default 'po vrsti',
  member_visible boolean not null default true,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_pieces (
  id uuid primary key default gen_random_uuid(),
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  legacy_id text,
  sku text,
  serial_number text,
  name text,
  category_name text,
  status text not null default 'U društvu',
  availability text not null default 'dostupno',
  location_id uuid references public.equipment_locations(id) on delete set null,
  location_name text,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_ropes (
  id uuid primary key default gen_random_uuid(),
  legacy_id text,
  sku text,
  name text not null,
  length_m numeric,
  diameter_mm text,
  manufacturer text,
  model text,
  status text not null default 'U društvu',
  availability text not null default 'dostupno',
  location_name text,
  next_service date,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_requests (
  id uuid primary key default gen_random_uuid(),
  requester_id uuid default auth.uid(),
  requester_email text,
  requester_name text,
  trip text,
  date_from date,
  date_to date,
  note text,
  status text not null default 'pending',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_request_items (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.equipment_requests(id) on delete cascade,
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_legacy_id text,
  name text,
  quantity int not null default 1,
  unit text default 'kom',
  note text,
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_loans (
  id uuid primary key default gen_random_uuid(),
  request_id uuid references public.equipment_requests(id) on delete set null,
  user_id uuid,
  user_email text,
  user_name text,
  status text not null default 'issued',
  issued_at timestamptz default now(),
  returned_at timestamptz,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_loan_items (
  id uuid primary key default gen_random_uuid(),
  loan_id uuid not null references public.equipment_loans(id) on delete cascade,
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_piece_id uuid references public.equipment_pieces(id) on delete set null,
  rope_id uuid references public.equipment_ropes(id) on delete set null,
  equipment_legacy_id text,
  name text,
  quantity int not null default 1,
  returned_quantity int not null default 0,
  return_status text,
  note text,
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_service_tasks (
  id uuid primary key default gen_random_uuid(),
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_piece_id uuid references public.equipment_pieces(id) on delete set null,
  rope_id uuid references public.equipment_ropes(id) on delete set null,
  title text not null,
  status text not null default 'open',
  due_date date,
  note text,
  created_by uuid default auth.uid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_disposals (
  id uuid primary key default gen_random_uuid(),
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_piece_id uuid references public.equipment_pieces(id) on delete set null,
  rope_id uuid references public.equipment_ropes(id) on delete set null,
  reason text,
  disposed_by uuid default auth.uid(),
  disposed_at timestamptz not null default now(),
  note text
);

create table if not exists public.equipment_audit_log (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid default auth.uid(),
  action text not null,
  entity_type text,
  entity_id text,
  before_data jsonb,
  after_data jsonb,
  created_at timestamptz not null default now()
);

-- -------------------------------------------------------------------
-- 4) RLS policies: members can see/request; Admin+Oružar can operate.
-- -------------------------------------------------------------------
do $$
declare t text;
begin
  foreach t in array array[
    'equipment_categories','equipment_locations','equipment_items','equipment_pieces','equipment_ropes',
    'equipment_requests','equipment_request_items','equipment_loans','equipment_loan_items',
    'equipment_service_tasks','equipment_disposals','equipment_audit_log'
  ] loop
    execute format('alter table public.%I enable row level security', t);
  end loop;
end $$;

-- public member catalog read
drop policy if exists "Approved read equipment categories" on public.equipment_categories;
create policy "Approved read equipment categories" on public.equipment_categories
for select using (public.sov_is_approved());

drop policy if exists "Approved read equipment locations" on public.equipment_locations;
create policy "Approved read equipment locations" on public.equipment_locations
for select using (public.sov_is_approved());

drop policy if exists "Approved read visible equipment items" on public.equipment_items;
create policy "Approved read visible equipment items" on public.equipment_items
for select using (public.sov_is_approved() and (member_visible = true or public.sov_can_manage_equipment()));

drop policy if exists "Approved read equipment ropes" on public.equipment_ropes;
create policy "Approved read equipment ropes" on public.equipment_ropes
for select using (public.sov_is_approved());

drop policy if exists "Armory read equipment pieces" on public.equipment_pieces;
create policy "Armory read equipment pieces" on public.equipment_pieces
for select using (public.sov_can_manage_equipment());

-- requests
drop policy if exists "Users insert own equipment requests" on public.equipment_requests;
create policy "Users insert own equipment requests" on public.equipment_requests
for insert with check (public.sov_is_approved() and (requester_id = auth.uid() or requester_id is null));

drop policy if exists "Users read own equipment requests" on public.equipment_requests;
create policy "Users read own equipment requests" on public.equipment_requests
for select using (requester_id = auth.uid() or public.sov_can_manage_equipment());

drop policy if exists "Armory updates equipment requests" on public.equipment_requests;
create policy "Armory updates equipment requests" on public.equipment_requests
for update using (public.sov_can_manage_equipment()) with check (public.sov_can_manage_equipment());

drop policy if exists "Users insert own request items" on public.equipment_request_items;
create policy "Users insert own request items" on public.equipment_request_items
for insert with check (
  exists (select 1 from public.equipment_requests r where r.id = request_id and (r.requester_id = auth.uid() or r.requester_id is null))
  or public.sov_can_manage_equipment()
);

drop policy if exists "Users read own request items" on public.equipment_request_items;
create policy "Users read own request items" on public.equipment_request_items
for select using (
  exists (select 1 from public.equipment_requests r where r.id = request_id and r.requester_id = auth.uid())
  or public.sov_can_manage_equipment()
);

drop policy if exists "Armory updates request items" on public.equipment_request_items;
create policy "Armory updates request items" on public.equipment_request_items
for update using (public.sov_can_manage_equipment()) with check (public.sov_can_manage_equipment());

-- armory operational full access
do $$
declare t text;
begin
  foreach t in array array[
    'equipment_categories','equipment_locations','equipment_items','equipment_pieces','equipment_ropes',
    'equipment_loans','equipment_loan_items','equipment_service_tasks','equipment_disposals','equipment_audit_log'
  ] loop
    execute format('drop policy if exists "Armory full access" on public.%I', t);
    execute format('create policy "Armory full access" on public.%I for all using (public.sov_can_manage_equipment()) with check (public.sov_can_manage_equipment())', t);
  end loop;
end $$;

-- -------------------------------------------------------------------
-- 5) Dashboard summary + indexes.
-- -------------------------------------------------------------------
create or replace view public.equipment_dashboard_summary as
select
  (select count(*) from public.equipment_items where member_visible = true) as equipment_count,
  (select coalesce(sum(available),0) from public.equipment_items where member_visible = true) as available_total,
  (select count(*) from public.equipment_ropes) as ropes_count,
  (select coalesce(sum(length_m),0) from public.equipment_ropes) as ropes_total_m,
  (select count(*) from public.equipment_requests where status in ('pending','approved','prepared','issued')) as open_request_count,
  (select count(*) from public.equipment_loans where status not in ('returned','closed')) as open_loan_count,
  (select count(*) from public.equipment_items where available <= 0 and member_visible = true) as unavailable_count,
  (select count(*) from public.equipment_service_tasks where status not in ('closed','done')) as open_service_count;

create index if not exists equipment_items_legacy_idx on public.equipment_items(legacy_id);
create index if not exists equipment_items_sku_idx on public.equipment_items(sku);
create index if not exists equipment_items_category_idx on public.equipment_items(category_name);
create index if not exists equipment_requests_requester_idx on public.equipment_requests(requester_id);
create index if not exists equipment_requests_status_idx on public.equipment_requests(status);

-- -------------------------------------------------------------------
-- 6) Audit triggers to unified sov_audit_log + local equipment audit.
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
    jsonb_build_object('module','oruzarstvo','source','web_v5_36')
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

-- -------------------------------------------------------------------
-- 7) Permission view compatibility note: v5.34 owns sov_current_user_permissions.
-- This SQL only relies on role admin/oruzar and can_manage_equipment fallback.
-- -------------------------------------------------------------------
