-- SOV Oružarstvo v4.78 — schema compatibility + open preview stability
-- Run once after v4.77 if import says schema cache / missing column errors.
-- This does NOT add a fake `category` column; the app now correctly uses category_name.

begin;

-- Columns used by the simplified model. Safe if already present.
alter table if exists public.equipment_items
  add column if not exists item_kind text not null default 'quantity_article',
  add column if not exists code_required boolean not null default false,
  add column if not exists physical_code_note text;

alter table if exists public.equipment_ropes
  add column if not exists item_kind text not null default 'individual_rope',
  add column if not exists code_required boolean not null default true;

-- Keep category_name populated for old/imported rows.
update public.equipment_items
set category_name = coalesce(nullif(category_name,''),'Ostalo')
where category_name is null or category_name='';

-- Open-preview policies, idempotent.
do $$
declare t text;
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
    execute format('drop policy if exists "SOV v4.78 open select" on public.%I', t);
    execute format('drop policy if exists "SOV v4.78 open insert" on public.%I', t);
    execute format('drop policy if exists "SOV v4.78 open update" on public.%I', t);
    execute format('drop policy if exists "SOV v4.78 open delete" on public.%I', t);
    execute format('create policy "SOV v4.78 open select" on public.%I for select using (true)', t);
    execute format('create policy "SOV v4.78 open insert" on public.%I for insert with check (true)', t);
    execute format('create policy "SOV v4.78 open update" on public.%I for update using (true) with check (true)', t);
    execute format('create policy "SOV v4.78 open delete" on public.%I for delete using (true)', t);
  end loop;
end $$;

commit;

-- If Supabase still reports an old schema cache immediately after this, wait 10-30 seconds and refresh the app tab.
