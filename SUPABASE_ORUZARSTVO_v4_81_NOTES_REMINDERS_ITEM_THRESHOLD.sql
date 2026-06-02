-- SOV Oružarstvo v4.81
-- Notes & Reminders + prag direktno na artiklu.
-- Pokrenuti jednom. Sigurno je za postojeću bazu.

alter table if exists public.equipment_items
  add column if not exists minimum integer default 0;

alter table if exists public.equipment_items
  add column if not exists physical_code_note text;

create table if not exists public.equipment_armory_notes (
  id text primary key,
  title text not null,
  body text,
  due_date date,
  note_type text default 'todo',
  priority text default 'normal',
  status text default 'open',
  created_at timestamptz default now(),
  updated_at timestamptz default now()
);

alter table public.equipment_armory_notes enable row level security;

drop policy if exists "open preview read armory notes" on public.equipment_armory_notes;
drop policy if exists "open preview write armory notes" on public.equipment_armory_notes;

-- Privremeno otvoreno prema trenutnom preview režimu projekta.
create policy "open preview read armory notes"
  on public.equipment_armory_notes for select
  using (true);

create policy "open preview write armory notes"
  on public.equipment_armory_notes for all
  using (true)
  with check (true);

-- Ako je prije postojala zasebna tablica/postavke za threshold, više se ne koristi u UI-u.
-- Prag za crveno je sada equipment_items.minimum, po artiklu.
