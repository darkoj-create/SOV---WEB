-- SOV Oružarstvo v5.40 — canonical app catalog for Android APK 1.3.8
-- Run in Supabase SQL editor with RLS enabled.
-- Purpose: one normalized read endpoint for the mobile app.

create extension if not exists pgcrypto;

-- Add only harmless metadata columns used by the app catalog view.
alter table public.equipment_items
  add column if not exists subcategory text,
  add column if not exists sku text,
  add column if not exists unit text default 'kom',
  add column if not exists member_visible boolean not null default true;

alter table public.equipment_pieces
  add column if not exists legacy_id text,
  add column if not exists serial_number text,
  add column if not exists category_name text,
  add column if not exists subcategory text,
  add column if not exists sku text,
  add column if not exists name text,
  add column if not exists location_name text,
  add column if not exists status text not null default 'U društvu',
  add column if not exists availability text not null default 'dostupno',
  add column if not exists note text;

alter table public.equipment_ropes
  add column if not exists legacy_id text,
  add column if not exists sku text,
  add column if not exists name text,
  add column if not exists length_m numeric,
  add column if not exists diameter_mm text,
  add column if not exists manufacturer text,
  add column if not exists model text,
  add column if not exists location_name text,
  add column if not exists status text not null default 'U društvu',
  add column if not exists availability text not null default 'dostupno',
  add column if not exists note text;

create or replace function public.sov_armory_norm(value text)
returns text
language sql
immutable
as $$
  select lower(translate(coalesce(value, ''), 'ČĆŠŽĐčćšžđ', 'CCSZDccszd'))
$$;

create or replace function public.sov_armory_main_category(raw_category text, search_basis text)
returns text
language sql
immutable
as $$
  select case
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(uzad|uzetna|(^| )uze($| )|rope|strik|statick|staticno|transportna vreca|transportne vrece)' then 'Užad'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(postavlj|karab|hms|matica|spojka|maillon|sidr|spit|anker|bolt|ploc|ring|fikser)' then 'Oprema za postavljanje'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(crtan|nacrt|mjeren|disto|distox|topodroid|kompas|klinomet|olov|papir|skic)' then 'Oprema za crtanje'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(osob|kacig|helmet|pojas|sjedal|croll|krol|crol|prsni|bloker|zumar|ascender|descender|spustal|(^| )stop($| )|rig|maestro|pedal|stremen)' then 'Osobna oprema'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(busil|hilti|bosch|makita|baterij|aku|punjac|svrd|gbh)' then 'Bušilice i baterije'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(elektro|foto|kamera|video|rasvjet|svjetl|lampa|ceona)' then 'Elektro i foto oprema'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(medicin|prva pomoc|sanitet)' then 'Medicinska oprema'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(logor|kamp|bivak|sator|kuhal|plin|podlog|vreca za spavanje)' then 'Oprema za logor'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(prosir|klin|cekic|macol|dlijet|stem)' then 'Oprema za proširivanje'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(ronil|ronjenje|neopren|maska|peraj|boca)' then 'Ronilačka oprema'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(alpinist|alpin|penjack|penjac)' then 'Alpinistička oprema'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(cisto podzemlje|ciscenje|otpad)' then 'Čisto podzemlje'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(alat|kljuc|odvijac|klijest|lopat)' then 'Ostali alat'
    when nullif(trim(coalesce(raw_category,'')), '') is not null then trim(raw_category)
    else 'Ostalo'
  end
$$;

create or replace function public.sov_armory_subcategory(raw_subcategory text, search_basis text)
returns text
language sql
immutable
as $$
  select case
    when nullif(trim(coalesce(raw_subcategory,'')), '') is not null then trim(raw_subcategory)
    when public.sov_armory_norm(search_basis) ~ '(karab|hms|matica|spojka|maillon)' then 'Karabineri'
    when public.sov_armory_norm(search_basis) ~ '(pojas|sjedal)' then 'Pojasevi i sjedalice'
    when public.sov_armory_norm(search_basis) ~ '(croll|krol|crol|prsni)' then 'Croll / prsni blokeri'
    when public.sov_armory_norm(search_basis) ~ '(zumar|ascender|rucni bloker)' then 'Ručni blokeri'
    when public.sov_armory_norm(search_basis) ~ '(descender|spustal|(^| )stop($| )|rig|maestro)' then 'Descenderi'
    when public.sov_armory_norm(search_basis) ~ '(kacig|helmet)' then 'Kacige'
    when public.sov_armory_norm(search_basis) ~ '(uze|uzad|rope|statick|staticno)' then 'Užad'
    when public.sov_armory_norm(search_basis) ~ '(transportna vreca|transportne vrece)' then 'Transportne vreće'
    when public.sov_armory_norm(search_basis) ~ '(spit|sidr|anker|bolt|ploc|ring)' then 'Spitovi i sidrišta'
    when public.sov_armory_norm(search_basis) ~ '(busil|hilti|bosch|makita)' then 'Bušilice'
    when public.sov_armory_norm(search_basis) ~ '(baterij|aku)' then 'Baterije'
    when public.sov_armory_norm(search_basis) ~ '(punjac)' then 'Punjači'
    when public.sov_armory_norm(search_basis) ~ '(svrd)' then 'Svrdla'
    when public.sov_armory_norm(search_basis) ~ '(disto|distox|topodroid|kompas|klinomet|mjeren)' then 'Mjerenje'
    when public.sov_armory_norm(search_basis) ~ '(crtan|nacrt|olov|papir|skic)' then 'Crtaći pribor'
    when public.sov_armory_norm(search_basis) ~ '(lampa|rasvjet|svjetl|ceona)' then 'Lampe i rasvjeta'
    else 'Ostalo'
  end
$$;

create or replace function public.sov_armory_category_priority(category text)
returns int
language sql
immutable
as $$
  select case public.sov_armory_main_category(category, category)
    when 'Užad' then 10
    when 'Oprema za postavljanje' then 20
    when 'Oprema za crtanje' then 30
    when 'Osobna oprema' then 40
    when 'Bušilice i baterije' then 50
    when 'Elektro i foto oprema' then 60
    when 'Oprema za logor' then 70
    when 'Medicinska oprema' then 80
    when 'Oprema za proširivanje' then 90
    when 'Ronilačka oprema' then 100
    when 'Alpinistička oprema' then 110
    when 'Čisto podzemlje' then 120
    when 'Ostali alat' then 130
    else 999
  end
$$;

drop view if exists public.sov_equipment_app_catalog cascade;

create view public.sov_equipment_app_catalog
with (security_invoker = true)
as
with item_rows as (
  select
    ('item:' || i.id::text) as app_id,
    'equipment_items'::text as source_table,
    i.id::text as source_id,
    coalesce(nullif(i.sku,''), nullif(i.legacy_id,''), i.id::text) as code,
    coalesce(nullif(i.name,''), nullif(i.sku,''), i.id::text) as name,
    coalesce(nullif(i.category_name,''), 'Ostalo') as raw_category,
    nullif(i.subcategory,'') as raw_subcategory,
    coalesce(i.location_name, 'Nije upisano') as location_name,
    greatest(coalesce(i.quantity,0), 0)::int as total_qty,
    least(greatest(coalesce(i.available, i.quantity, 0), 0), greatest(coalesce(i.quantity,0),0))::int as available_qty,
    coalesce(nullif(i.status,''), 'U društvu') as status,
    coalesce(nullif(i.availability,''), case when coalesce(i.available,0) > 0 then 'dostupno' else 'nedostupno' end) as availability,
    coalesce(nullif(i.unit,''), 'kom') as unit,
    coalesce(nullif(i.note,''), '') as note,
    coalesce(i.member_visible, true) as member_visible,
    concat_ws(' ', i.name, i.sku, i.legacy_id, i.category_name, i.subcategory, i.location_name, i.status, i.availability, i.note, i.manufacturer, i.model) as basis
  from public.equipment_items i
  where coalesce(i.member_visible, true) = true or public.sov_can_manage_equipment()
),
rope_rows as (
  select
    ('rope:' || r.id::text) as app_id,
    'equipment_ropes'::text as source_table,
    r.id::text as source_id,
    coalesce(nullif(r.sku,''), nullif(r.legacy_id,''), r.id::text) as code,
    trim(concat_ws(' · ', coalesce(nullif(r.name,''), nullif(r.model,''), 'Uže'), nullif(r.diameter_mm,''), case when r.length_m is not null then (r.length_m::text || ' m') else null end)) as name,
    'Užad i užetna oprema'::text as raw_category,
    'Užad'::text as raw_subcategory,
    coalesce(r.location_name, 'Nije upisano') as location_name,
    1::int as total_qty,
    case when public.sov_armory_norm(coalesce(r.status,'') || ' ' || coalesce(r.availability,'')) ~ '(posud|izdan|vani|servis|otpis|izgubl)' then 0 else 1 end as available_qty,
    coalesce(nullif(r.status,''), 'U društvu') as status,
    coalesce(nullif(r.availability,''), 'dostupno') as availability,
    'kom'::text as unit,
    coalesce(nullif(r.note,''), '') as note,
    true as member_visible,
    concat_ws(' ', r.name, r.sku, r.legacy_id, r.manufacturer, r.model, r.diameter_mm, r.length_m::text, r.location_name, r.status, r.availability, r.note, 'uže užad rope statičko') as basis
  from public.equipment_ropes r
),
piece_rows as (
  select
    ('piece:' || p.id::text) as app_id,
    'equipment_pieces'::text as source_table,
    p.id::text as source_id,
    coalesce(nullif(p.sku,''), nullif(p.legacy_id,''), nullif(p.serial_number,''), p.id::text) as code,
    coalesce(nullif(p.name,''), nullif(p.serial_number,''), 'Komad opreme') as name,
    coalesce(nullif(p.category_name,''), 'Ostalo') as raw_category,
    nullif(p.subcategory,'') as raw_subcategory,
    coalesce(p.location_name, 'Nije upisano') as location_name,
    1::int as total_qty,
    case when public.sov_armory_norm(coalesce(p.status,'') || ' ' || coalesce(p.availability,'')) ~ '(posud|izdan|vani|servis|otpis|izgubl)' then 0 else 1 end as available_qty,
    coalesce(nullif(p.status,''), 'U društvu') as status,
    coalesce(nullif(p.availability,''), 'dostupno') as availability,
    'kom'::text as unit,
    coalesce(nullif(p.note,''), '') as note,
    public.sov_can_manage_equipment() as member_visible,
    concat_ws(' ', p.name, p.sku, p.legacy_id, p.serial_number, p.category_name, p.subcategory, p.location_name, p.status, p.availability, p.note) as basis
  from public.equipment_pieces p
  where public.sov_can_manage_equipment()
),
all_rows as (
  select * from item_rows
  union all
  select * from rope_rows
  union all
  select * from piece_rows
)
select
  app_id,
  source_table,
  source_id,
  code,
  name,
  raw_category,
  raw_subcategory,
  public.sov_armory_main_category(raw_category, basis) as main_category,
  public.sov_armory_subcategory(raw_subcategory, basis) as subcategory,
  location_name,
  total_qty,
  available_qty,
  status,
  availability,
  unit,
  note,
  public.sov_armory_category_priority(public.sov_armory_main_category(raw_category, basis)) as priority,
  public.sov_armory_norm(concat_ws(' ',
    code, name, raw_category, raw_subcategory,
    public.sov_armory_main_category(raw_category, basis),
    public.sov_armory_subcategory(raw_subcategory, basis),
    location_name, status, availability, unit, note, basis,
    'uze uzad rope strik karabiner karabin karbiner hms matica spojka croll krol crol prsni bloker pojas sjedalica kaciga helmet stop descender spustalica busilica busilica baterija punjac crtaci crtaći pribor disto topodroid kompas'
  )) as search_text,
  member_visible
from all_rows;

grant select on public.sov_equipment_app_catalog to authenticated;

-- Quick sanity check after running:
-- select main_category, count(*) rows, sum(total_qty) total_qty, sum(available_qty) available_qty
-- from public.sov_equipment_app_catalog
-- group by main_category
-- order by min(priority), main_category;
