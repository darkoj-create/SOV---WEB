-- SOV Oružarstvo v4.84
-- FIX za v4.80 lokacije: equipment_items nema column location_name.
-- Pokrenuti jednom. Ne briše postojeće artikle/kategorije/posudbe.

create extension if not exists pgcrypto;

create table if not exists public.equipment_item_locations (
  id uuid primary key default gen_random_uuid(),
  equipment_legacy_id text,
  item_name text not null,
  location_type text not null default 'storage',
  location_name text not null default 'Oružarstvo',
  quantity integer not null default 0,
  note text,
  updated_at timestamptz not null default now(),
  constraint equipment_item_locations_qty_nonnegative check (quantity >= 0)
);

create unique index if not exists equipment_item_locations_unique
on public.equipment_item_locations (
  coalesce(equipment_legacy_id,''),
  lower(item_name),
  lower(location_type),
  lower(location_name)
);

create table if not exists public.equipment_request_item_returns (
  id uuid primary key default gen_random_uuid(),
  request_id uuid references public.equipment_requests(id) on delete cascade,
  request_item_id uuid,
  equipment_legacy_id text,
  item_name text not null,
  issued_quantity integer not null default 0,
  returned_quantity integer not null default 0,
  missing_quantity integer not null default 0,
  return_location_name text not null default 'Oružarstvo',
  remaining_location_name text,
  note text,
  created_at timestamptz not null default now()
);

alter table public.equipment_item_locations enable row level security;
alter table public.equipment_request_item_returns enable row level security;

drop policy if exists "SOV v4.80 open item locations" on public.equipment_item_locations;
drop policy if exists "SOV v4.84 open item locations" on public.equipment_item_locations;
create policy "SOV v4.84 open item locations"
on public.equipment_item_locations
for all
using (true)
with check (true);

drop policy if exists "SOV v4.80 open return records" on public.equipment_request_item_returns;
drop policy if exists "SOV v4.84 open return records" on public.equipment_request_item_returns;
create policy "SOV v4.84 open return records"
on public.equipment_request_item_returns
for all
using (true)
with check (true);

-- Startno napuni lokaciju Oružarstvo za količinske artikle koji još nemaju lokacijsku evidenciju.
-- Bitno: NE koristi i.location_name jer taj stupac ne postoji na equipment_items.
do $$
begin
  if to_regclass('public.equipment_items') is not null then
    insert into public.equipment_item_locations (equipment_legacy_id,item_name,location_type,location_name,quantity,note)
    select
      i.legacy_id,
      i.name,
      'storage',
      'Oružarstvo',
      greatest(0, round(coalesce(i.available, i.quantity, 0)))::integer,
      'Auto seed v4.84 iz equipment_items.available/quantity'
    from public.equipment_items i
    where coalesce(i.name,'') <> ''
      and not exists (
        select 1 from public.equipment_item_locations l
        where coalesce(l.equipment_legacy_id,'') = coalesce(i.legacy_id,'')
          and lower(l.item_name)=lower(i.name)
      );
  end if;
end $$;
