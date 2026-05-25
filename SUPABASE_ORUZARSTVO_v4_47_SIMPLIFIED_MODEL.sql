-- SOV Oružarstvo v4.47 — simplified armory model
-- Cilj: samo užad ima stvarni pojedinačni kod/SKU.
-- Ostala oprema se vodi kao količinski artikl po vrsti/modelu.

begin;

-- 1) Non-destructive metadata columns. Existing tables stay compatible.
alter table if exists public.equipment_items
  add column if not exists item_kind text not null default 'quantity_article',
  add column if not exists code_required boolean not null default false,
  add column if not exists physical_code_note text;

alter table if exists public.equipment_ropes
  add column if not exists item_kind text not null default 'individual_rope',
  add column if not exists code_required boolean not null default true;

-- 2) Make intent explicit for existing data.
update public.equipment_items
set item_kind = 'quantity_article',
    code_required = false,
    tracking_type = coalesce(nullif(tracking_type,''), 'po vrsti'),
    physical_code_note = coalesce(physical_code_note, 'Nema pojedinačnih kodova; vodi se količina po artiklu.')
where coalesce(item_kind,'') <> 'quantity_article'
   or code_required is distinct from false;

update public.equipment_ropes
set item_kind = 'individual_rope',
    code_required = true
where coalesce(item_kind,'') <> 'individual_rope'
   or code_required is distinct from true;

-- 3) Helpful views for UI and checks.
create or replace view public.armory_quantity_articles as
select
  id,
  legacy_id,
  catalog_id,
  name,
  category_name,
  subcategory,
  unit,
  tracking_type,
  quantity,
  loaned,
  available,
  minimum,
  status,
  availability,
  member_visible,
  internal_note,
  source_sheet,
  item_kind,
  code_required,
  physical_code_note,
  created_at,
  updated_at
from public.equipment_items
where coalesce(item_kind,'quantity_article') = 'quantity_article';

create or replace view public.armory_individual_ropes as
select
  id,
  legacy_id,
  sku,
  name,
  diameter_mm,
  length_m,
  manufacturer,
  model,
  standard,
  production_year,
  in_use_since,
  color,
  supplier,
  location_name,
  status,
  note,
  item_kind,
  code_required,
  created_at,
  updated_at
from public.equipment_ropes
where coalesce(item_kind,'individual_rope') = 'individual_rope';

-- 4) Optional safety: if a request item references non-rope equipment, quantity is the source of truth.
comment on table public.equipment_items is 'SOV v4.47: quantity-by-article equipment. No physical code per carabiner/croll/descender/etc.';
comment on table public.equipment_ropes is 'SOV v4.47: individual rope registry. Rope SKU/code stays physical and unique.';
comment on column public.equipment_items.code_required is 'false for all normal equipment; only ropes require physical codes in equipment_ropes.';

commit;
