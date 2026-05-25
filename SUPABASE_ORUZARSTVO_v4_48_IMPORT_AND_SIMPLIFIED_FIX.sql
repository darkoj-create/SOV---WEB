-- SOV Oružarstvo v4.48 — import/category fix + simplified model guard
-- Run this after the base setup and v4.47 migration.

begin;

-- Ensure simplified columns exist.
alter table if exists public.equipment_items
  add column if not exists item_kind text not null default 'quantity_article',
  add column if not exists code_required boolean not null default false,
  add column if not exists physical_code_note text;

alter table if exists public.equipment_ropes
  add column if not exists item_kind text not null default 'individual_rope',
  add column if not exists code_required boolean not null default true;

update public.equipment_items
set item_kind='quantity_article',
    code_required=false,
    tracking_type=coalesce(nullif(tracking_type,''),'po vrsti'),
    physical_code_note=coalesce(physical_code_note,'Nema pojedinačnih kodova; vodi se količina po artiklu.')
where true;

update public.equipment_ropes
set item_kind='individual_rope',
    code_required=true
where true;

-- Category import now upserts by unique name in JS, not by legacy_id.
-- This avoids: duplicate key value violates unique constraint equipment_categories_name_key.
comment on table public.equipment_categories is 'SOV v4.48: import upserts categories by name; legacy_id is not treated as the category identity.';

commit;
