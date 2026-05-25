-- SOV Oružarstvo v4.77 — FULL OPEN PREVIEW MODE
-- Privremeno: svatko s linkom može čitati i mijenjati oružarstvo podatke preko anon ključa.
-- Koristi samo za test/demo. Kada završi testiranje, vrati role-gated SQL/policies.

begin;

-- Osiguraj tablice za posudbe ako nisu već napravljene.
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

-- RLS ostaje uključen, ali dodajemo privremene "open preview" politike.
do $$
declare
  t text;
begin
  foreach t in array array[
    'equipment_categories',
    'equipment_items',
    'equipment_pieces',
    'equipment_ropes',
    'equipment_requests',
    'equipment_request_items'
  ] loop
    execute format('alter table public.%I enable row level security', t);
    execute format('drop policy if exists "SOV v4.77 open select" on public.%I', t);
    execute format('drop policy if exists "SOV v4.77 open insert" on public.%I', t);
    execute format('drop policy if exists "SOV v4.77 open update" on public.%I', t);
    execute format('drop policy if exists "SOV v4.77 open delete" on public.%I', t);

    execute format('create policy "SOV v4.77 open select" on public.%I for select using (true)', t);
    execute format('create policy "SOV v4.77 open insert" on public.%I for insert with check (true)', t);
    execute format('create policy "SOV v4.77 open update" on public.%I for update using (true) with check (true)', t);
    execute format('create policy "SOV v4.77 open delete" on public.%I for delete using (true)', t);
  end loop;
end $$;

create index if not exists equipment_requests_status_idx on public.equipment_requests(status);
create index if not exists equipment_requests_created_at_idx on public.equipment_requests(created_at desc);
create index if not exists equipment_request_items_request_id_idx on public.equipment_request_items(request_id);

commit;
