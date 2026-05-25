-- SOV Oružarstvo v4.70 — request/posudbe RLS hard fix
-- Pokrenuti jednom u Supabase SQL Editoru ako se klik "Zatraži" ne vidi oružaru.
-- NE pokreće se za svaku akciju; ovo samo postavlja tablice/politike.

begin;

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

alter table public.equipment_requests enable row level security;
alter table public.equipment_request_items enable row level security;

-- Users/guests can create a request. Logged-in users keep requester_id; simple user view can use NULL requester_id.
drop policy if exists "Users insert own equipment requests" on public.equipment_requests;
drop policy if exists "SOV v4.70 insert equipment requests" on public.equipment_requests;
create policy "SOV v4.70 insert equipment requests"
on public.equipment_requests
for insert
with check (requester_id is null or requester_id = auth.uid() or public.sov_has_role(array['admin','oruzar']));

-- Oružar/admin sees all. User sees own; anonymous created requests are visible to armory only in practice.
drop policy if exists "Users read own equipment requests" on public.equipment_requests;
drop policy if exists "SOV v4.70 read equipment requests" on public.equipment_requests;
create policy "SOV v4.70 read equipment requests"
on public.equipment_requests
for select
using (public.sov_has_role(array['admin','oruzar']) or requester_id = auth.uid() or requester_id is null);

-- Oružar can update status: pending -> issued -> returned.
drop policy if exists "Armory updates equipment requests" on public.equipment_requests;
drop policy if exists "SOV v4.70 armory updates equipment requests" on public.equipment_requests;
create policy "SOV v4.70 armory updates equipment requests"
on public.equipment_requests
for update
using (public.sov_has_role(array['admin','oruzar']))
with check (public.sov_has_role(array['admin','oruzar']));

-- Request item rows can be created for a request the user just created, or by armory.
drop policy if exists "Users insert own request items" on public.equipment_request_items;
drop policy if exists "SOV v4.70 insert request items" on public.equipment_request_items;
create policy "SOV v4.70 insert request items"
on public.equipment_request_items
for insert
with check (
  exists (
    select 1 from public.equipment_requests r
    where r.id = request_id
      and (r.requester_id is null or r.requester_id = auth.uid() or public.sov_has_role(array['admin','oruzar']))
  )
);

drop policy if exists "Users read own request items" on public.equipment_request_items;
drop policy if exists "SOV v4.70 read request items" on public.equipment_request_items;
create policy "SOV v4.70 read request items"
on public.equipment_request_items
for select
using (
  exists (
    select 1 from public.equipment_requests r
    where r.id = request_id
      and (public.sov_has_role(array['admin','oruzar']) or r.requester_id = auth.uid() or r.requester_id is null)
  )
);

create index if not exists equipment_requests_status_idx on public.equipment_requests(status);
create index if not exists equipment_requests_created_at_idx on public.equipment_requests(created_at desc);
create index if not exists equipment_request_items_request_id_idx on public.equipment_request_items(request_id);

commit;
