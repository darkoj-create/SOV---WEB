-- SOV Cloud — Oružarstvo SQL backend
-- Run this after the base auth/profile SQL from SUPABASE_SETUP.md.
-- Uses existing public.profiles table with roles: admin, editor, oruzar, user.

create extension if not exists pgcrypto;

create or replace function public.sov_is_approved()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles p
    where p.id = auth.uid()
      and p.status = 'approved'
  );
$$;

create or replace function public.sov_has_role(required_roles text[])
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles p
    where p.id = auth.uid()
      and p.status = 'approved'
      and p.role::text = any(required_roles)
  );
$$;

create table if not exists public.equipment_categories (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  name text not null unique,
  description text,
  type text,
  sort_order integer default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_locations (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  name text not null unique,
  description text,
  type text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_items (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  catalog_id text,
  name text not null,
  category_id uuid references public.equipment_categories(id) on delete set null,
  category_name text,
  subcategory text,
  unit text default 'kom',
  tracking_type text default 'po vrsti',
  quantity numeric default 0,
  loaned numeric default 0,
  available numeric default 0,
  minimum numeric,
  status text default 'aktivno',
  availability text default 'dostupno',
  member_visible boolean not null default true,
  internal_note text,
  source_sheet text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_pieces (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  catalog_legacy_id text,
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  name text not null,
  sku text,
  manufacturer text,
  model text,
  purchase_date date,
  location_id uuid references public.equipment_locations(id) on delete set null,
  location_name text,
  status text default 'U društvu',
  next_service date,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_ropes (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  sku text unique,
  name text not null,
  diameter_mm numeric,
  length_m numeric,
  manufacturer text,
  model text,
  standard text,
  production_year integer,
  in_use_since date,
  color text,
  supplier text,
  location_name text,
  status text default 'U društvu',
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_requests (
  id uuid primary key default gen_random_uuid(),
  requester_id uuid references auth.users(id) on delete set null,
  requester_name text,
  requester_email text,
  trip_name text,
  date_from date,
  date_to date,
  note text,
  status text not null default 'pending',
  decided_by uuid references auth.users(id) on delete set null,
  decided_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_request_items (
  id uuid primary key default gen_random_uuid(),
  request_id uuid not null references public.equipment_requests(id) on delete cascade,
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_legacy_id text,
  item_name text not null,
  quantity numeric not null default 1,
  note text,
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_loans (
  id uuid primary key default gen_random_uuid(),
  request_id uuid references public.equipment_requests(id) on delete set null,
  borrower_id uuid references auth.users(id) on delete set null,
  borrower_name text,
  borrower_email text,
  trip_name text,
  issued_by uuid references auth.users(id) on delete set null,
  issued_at timestamptz,
  due_date date,
  returned_at timestamptz,
  return_note text,
  status text not null default 'issued',
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
  item_name text not null,
  quantity numeric not null default 1,
  condition_out text,
  condition_in text,
  note text,
  created_at timestamptz not null default now()
);

create table if not exists public.inventory_sessions (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  name text not null,
  inventory_date date not null default current_date,
  owner_name text,
  created_by uuid references auth.users(id) on delete set null,
  status text not null default 'draft',
  note text,
  locked_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.inventory_session_items (
  id uuid primary key default gen_random_uuid(),
  inventory_session_id uuid not null references public.inventory_sessions(id) on delete cascade,
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_legacy_id text,
  item_name text not null,
  expected numeric,
  counted numeric,
  difference numeric generated always as (coalesce(counted,0) - coalesce(expected,0)) stored,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_service_tasks (
  id uuid primary key default gen_random_uuid(),
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_piece_id uuid references public.equipment_pieces(id) on delete set null,
  rope_id uuid references public.equipment_ropes(id) on delete set null,
  item_name text,
  task_type text,
  status text not null default 'open',
  due_date date,
  performed_at date,
  performed_by text,
  note text,
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.procurement_plan (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_legacy_id text,
  item_name text not null,
  quantity numeric,
  unit_price numeric,
  total_price numeric,
  supplier text,
  priority text,
  status text not null default 'prijedlog',
  purchase_date date,
  requested_by text,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_disposals (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  disposal_date date,
  disposal_type text not null default 'rashod',
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_piece_id uuid references public.equipment_pieces(id) on delete set null,
  rope_id uuid references public.equipment_ropes(id) on delete set null,
  equipment_legacy_id text,
  item_name text not null,
  quantity numeric,
  reason text,
  location_name text,
  person_name text,
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_field_items (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  equipment_item_id uuid references public.equipment_items(id) on delete set null,
  equipment_piece_id uuid references public.equipment_pieces(id) on delete set null,
  rope_id uuid references public.equipment_ropes(id) on delete set null,
  equipment_legacy_id text,
  item_name text not null,
  quantity numeric,
  field_location text,
  responsible_person text,
  status text not null default 'na terenu',
  note text,
  recorded_at date,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.equipment_audit_log (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid references auth.users(id) on delete set null,
  action text not null,
  entity_type text not null,
  entity_id text,
  before_data jsonb,
  after_data jsonb,
  created_at timestamptz not null default now()
);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

do $$
declare t text;
begin
  foreach t in array array[
    'equipment_categories','equipment_locations','equipment_items','equipment_pieces','equipment_ropes',
    'equipment_requests','equipment_loans','inventory_sessions','inventory_session_items',
    'equipment_service_tasks','procurement_plan','equipment_disposals','equipment_field_items'
  ] loop
    execute format('drop trigger if exists set_updated_at on public.%I', t);
    execute format('create trigger set_updated_at before update on public.%I for each row execute function public.set_updated_at()', t);
  end loop;
end $$;

alter table public.equipment_categories enable row level security;
alter table public.equipment_locations enable row level security;
alter table public.equipment_items enable row level security;
alter table public.equipment_pieces enable row level security;
alter table public.equipment_ropes enable row level security;
alter table public.equipment_requests enable row level security;
alter table public.equipment_request_items enable row level security;
alter table public.equipment_loans enable row level security;
alter table public.equipment_loan_items enable row level security;
alter table public.inventory_sessions enable row level security;
alter table public.inventory_session_items enable row level security;
alter table public.equipment_service_tasks enable row level security;
alter table public.procurement_plan enable row level security;
alter table public.equipment_disposals enable row level security;
alter table public.equipment_field_items enable row level security;
alter table public.equipment_audit_log enable row level security;

-- Read catalog for approved users. Internal/person-sensitive tables stay armory-only.
drop policy if exists "Approved users read equipment categories" on public.equipment_categories;
create policy "Approved users read equipment categories" on public.equipment_categories for select using (public.sov_is_approved());

drop policy if exists "Approved users read equipment locations" on public.equipment_locations;
create policy "Approved users read equipment locations" on public.equipment_locations for select using (public.sov_is_approved());

drop policy if exists "Approved users read visible equipment items" on public.equipment_items;
create policy "Approved users read visible equipment items" on public.equipment_items for select using (public.sov_is_approved() and (member_visible = true or public.sov_has_role(array['admin','oruzar'])));

drop policy if exists "Approved users read ropes" on public.equipment_ropes;
create policy "Approved users read ropes" on public.equipment_ropes for select using (public.sov_is_approved());

drop policy if exists "Armory reads pieces" on public.equipment_pieces;
create policy "Armory reads pieces" on public.equipment_pieces for select using (public.sov_has_role(array['admin','oruzar']));

-- Member requests.
drop policy if exists "Users insert own equipment requests" on public.equipment_requests;
create policy "Users insert own equipment requests" on public.equipment_requests for insert with check (public.sov_is_approved() and requester_id = auth.uid());

drop policy if exists "Users read own equipment requests" on public.equipment_requests;
create policy "Users read own equipment requests" on public.equipment_requests for select using (requester_id = auth.uid() or public.sov_has_role(array['admin','oruzar']));

drop policy if exists "Armory updates equipment requests" on public.equipment_requests;
create policy "Armory updates equipment requests" on public.equipment_requests for update using (public.sov_has_role(array['admin','oruzar'])) with check (public.sov_has_role(array['admin','oruzar']));

drop policy if exists "Users insert own request items" on public.equipment_request_items;
create policy "Users insert own request items" on public.equipment_request_items for insert with check (
  exists (select 1 from public.equipment_requests r where r.id = request_id and r.requester_id = auth.uid())
  or public.sov_has_role(array['admin','oruzar'])
);

drop policy if exists "Users read own request items" on public.equipment_request_items;
create policy "Users read own request items" on public.equipment_request_items for select using (
  exists (select 1 from public.equipment_requests r where r.id = request_id and r.requester_id = auth.uid())
  or public.sov_has_role(array['admin','oruzar'])
);

drop policy if exists "Armory updates request items" on public.equipment_request_items;
create policy "Armory updates request items" on public.equipment_request_items for update using (public.sov_has_role(array['admin','oruzar'])) with check (public.sov_has_role(array['admin','oruzar']));

-- Armory-only tables.
do $$
declare t text;
begin
  foreach t in array array[
    'equipment_loans','equipment_loan_items','inventory_sessions','inventory_session_items',
    'equipment_service_tasks','procurement_plan','equipment_disposals','equipment_field_items','equipment_audit_log'
  ] loop
    execute format('drop policy if exists "Armory full access" on public.%I', t);
    execute format('create policy "Armory full access" on public.%I for all using (public.sov_has_role(array[''admin'',''oruzar''])) with check (public.sov_has_role(array[''admin'',''oruzar'']))', t);
  end loop;
end $$;

-- Import/write access for catalog tables.
do $$
declare t text;
begin
  foreach t in array array['equipment_categories','equipment_locations','equipment_items','equipment_pieces','equipment_ropes'] loop
    execute format('drop policy if exists "Armory writes catalog" on public.%I', t);
    execute format('create policy "Armory writes catalog" on public.%I for all using (public.sov_has_role(array[''admin'',''oruzar''])) with check (public.sov_has_role(array[''admin'',''oruzar'']))', t);
  end loop;
end $$;

create or replace view public.equipment_dashboard_summary as
select
  (select count(*) from public.equipment_items where member_visible = true) as equipment_count,
  (select coalesce(sum(available),0) from public.equipment_items where member_visible = true) as available_total,
  (select count(*) from public.equipment_ropes) as ropes_count,
  (select coalesce(sum(length_m),0) from public.equipment_ropes) as ropes_total_m,
  (select count(*) from public.equipment_requests where status in ('pending','approved','prepared','issued')) as open_request_count,
  (select count(*) from public.equipment_loans where status not in ('returned','closed')) as open_loan_count,
  (select count(*) from public.inventory_sessions) as inventory_count,
  (select count(*) from public.equipment_service_tasks where status not in ('closed','done')) as open_service_count;

create index if not exists equipment_items_legacy_idx on public.equipment_items(legacy_id);
create index if not exists equipment_items_category_idx on public.equipment_items(category_name);
create index if not exists equipment_requests_requester_idx on public.equipment_requests(requester_id);
create index if not exists equipment_requests_status_idx on public.equipment_requests(status);
create index if not exists equipment_request_items_request_idx on public.equipment_request_items(request_id);
create index if not exists inventory_session_items_session_idx on public.inventory_session_items(inventory_session_id);
