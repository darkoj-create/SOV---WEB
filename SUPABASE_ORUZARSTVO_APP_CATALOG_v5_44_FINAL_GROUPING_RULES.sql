-- SOV Oružarstvo v5.44 — FINAL equipment grouping rules
-- Run in Supabase SQL editor with RLS enabled.
-- Finalizes canonical armory grouping for APK/Web: one grouped catalog layer for browsing/search, raw catalog for inventory.
-- Regular users still see only Dostupno / Nije dostupno through masked total_qty / available_qty.
-- RLS stays enabled; this only recreates the app catalog view and helper functions.

create extension if not exists pgcrypto;

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

create or replace function public.sov_armory_is_personal_kit(raw_category text, raw_subcategory text, search_basis text)
returns boolean
language sql
immutable
as $$
  select (
    public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(raw_subcategory,'') || ' ' || coalesce(search_basis,'')) ~ '(croll|krol|crol|prsni|descender|spustal|(^| )stop($| )|rig|maestro|id[''’]?s|zumar|ascender|rucni bloker|rucni|(^| )bloker|pedal|stremen|pupak|pupcano)'
    or (public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(raw_subcategory,'') || ' ' || coalesce(search_basis,'')) ~ '(pojas|sjedal)'
        and public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(raw_subcategory,'') || ' ' || coalesce(search_basis,'')) !~ '(penjack|penjac|alpinist|climbing)')
  )
$$;

create or replace function public.sov_armory_main_category(raw_category text, search_basis text)
returns text
language sql
immutable
as $$
  select case
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(uzad|uzetna|(^| )uze($| )|rope|strik|statick|staticno|dinamick|transportna vreca|transportne vrece)' then 'Užad'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(postavlj|karab|hms|matica|spojka|maillon|omni|triact|trilock|screw|twist|sidr|spit|anker|bolt|ploc|ring|fikser)'
      and not public.sov_armory_is_personal_kit(raw_category, '', search_basis) then 'Oprema za postavljanje'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(crtan|nacrt|mjeren|disto|distox|topodroid|kompas|klinomet|olov|papir|skic)' then 'Oprema za crtanje'
    when public.sov_armory_is_personal_kit(raw_category, '', search_basis) then 'Osobna oprema - komplet'
    when public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) ~ '(osob|kacig|helmet|kombinezon|odijel|rukavic|cizm|obuc)' then 'Osobna oprema'
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
    when public.sov_armory_norm(search_basis) ~ '(descender|spustal|(^| )stop($| )|rig|maestro|id[''’]?s)' then 'Descenderi'
    when public.sov_armory_norm(search_basis) ~ '(croll|krol|crol|prsni)' then 'Croll / prsni blokeri'
    when public.sov_armory_norm(search_basis) ~ '(zumar|ascender|rucni bloker|rucni)' then 'Ručni blokeri'
    when public.sov_armory_norm(search_basis) ~ '(pedal|stremen)' then 'Pedale / stremeni'
    when public.sov_armory_norm(search_basis) ~ '(pojas|sjedal)' and public.sov_armory_norm(search_basis) !~ '(penjack|penjac|alpinist|climbing)' then 'Pojasevi i sjedalice'
    when public.sov_armory_norm(search_basis) ~ '(karab|hms|matica|spojka|maillon|omni|triact|trilock|screw|twist|lock|oval)' then 'Karabineri'
    when public.sov_armory_norm(search_basis) ~ '(kacig|helmet)' then 'Kacige'
    when public.sov_armory_norm(search_basis) ~ '(transportna vreca|transportne vrece)' then 'Transportne vreće'
    when public.sov_armory_norm(search_basis) ~ '(prusik)' then 'Prusici'
    when public.sov_armory_norm(search_basis) ~ '(gurt|traka|sling)' then 'Gurtne i trake'
    when public.sov_armory_norm(search_basis) ~ '(uze|uzad|rope|statick|staticno|dinamick)' then 'Užad'
    when public.sov_armory_norm(search_basis) ~ '(ploc|ploč|ring)' then 'Pločice / ringovi'
    when public.sov_armory_norm(search_basis) ~ '(spit|sidr|anker|bolt)' then 'Spitovi i sidrišta'
    when public.sov_armory_norm(search_basis) ~ '(busil|hilti|bosch|makita)' then 'Bušilice'
    when public.sov_armory_norm(search_basis) ~ '(baterij|aku)' then 'Baterije'
    when public.sov_armory_norm(search_basis) ~ '(punjac)' then 'Punjači'
    when public.sov_armory_norm(search_basis) ~ '(svrd)' then 'Svrdla'
    when public.sov_armory_norm(search_basis) ~ '(disto|distox|topodroid|kompas|klinomet|mjeren)' then 'Mjerenje'
    when public.sov_armory_norm(search_basis) ~ '(crtan|nacrt|olov|papir|skic)' then 'Crtaći pribor'
    when public.sov_armory_norm(search_basis) ~ '(lampa|rasvjet|svjetl|ceona)' then 'Lampe i rasvjeta'
    when nullif(trim(coalesce(raw_subcategory,'')), '') is not null then trim(raw_subcategory)
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
    when 'Osobna oprema - komplet' then 40
    when 'Osobna oprema' then 45
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

create or replace function public.sov_armory_search_tags(main_category text, subcategory text, search_basis text)
returns text
language sql
immutable
as $$
  -- v5.44: search tags are intentionally NOT broad per-category synonyms.
  -- Example: searching "croll" must only hit Croll/prsni bloker rows, not every personal-kit row.
  -- Category tags are generic; concrete terms are added only through subcategory rules.
  select concat_ws(' ',
    case public.sov_armory_main_category(main_category, search_basis)
      when 'Užad' then 'uze uzad rope strik'
      when 'Oprema za postavljanje' then 'postavljanje rigging sidriste sidrište'
      when 'Oprema za crtanje' then 'crtanje dokumentiranje mjerenje'
      when 'Osobna oprema - komplet' then 'osobna oprema komplet vertikala srt'
      when 'Osobna oprema' then 'osobna oprema'
      when 'Bušilice i baterije' then 'busilice bušilice baterije alat za postavljanje'
      when 'Elektro i foto oprema' then 'elektro foto rasvjeta'
      when 'Medicinska oprema' then 'medicina medicinska prva pomoc prva pomoć sanitet'
      when 'Oprema za logor' then 'logor kamp bivak'
      when 'Oprema za proširivanje' then 'prosirivanje proširivanje stemanje štemanje'
      when 'Ronilačka oprema' then 'ronjenje ronilacka ronilačka'
      when 'Alpinistička oprema' then 'alpinisticka alpinistička penjacka penjačka'
      when 'Čisto podzemlje' then 'cisto čisto podzemlje ciscenje čišćenje'
      when 'Ostali alat' then 'alat tools'
      else null
    end,
    case public.sov_armory_subcategory(subcategory, search_basis)
      when 'Karabineri' then 'karabiner karabin karbiner karab hms matica spojka maillon omni triact trilock screw twist lock oval'
      when 'Croll / prsni blokeri' then 'croll krol crol prsni bloker chest ascender'
      when 'Pojasevi i sjedalice' then 'pojas pojasevi sjedalica sjedni pojas harness prsni pojas'
      when 'Descenderi' then 'stop descender descenderi spustalica rig id maestro'
      when 'Ručni blokeri' then 'zumar jumar rucni ručni bloker ascender'
      when 'Pedale / stremeni' then 'pedala pedal stremen stremeni footloop'
      when 'Kacige' then 'kaciga kacige helmet'
      when 'Užad' then 'uze uzad rope strik staticko staticno dinamicko'
      when 'Transportne vreće' then 'transportna vreca transportne vrece transport bag'
      when 'Prusici' then 'prusik prusici pomocno uze pomocno uže'
      when 'Gurtne i trake' then 'gurtna gurtne traka trake sling'
      when 'Spitovi i sidrišta' then 'spit sidriste sidrište anker bolt plocica pločica ring fikser'
      when 'Pločice / ringovi' then 'plocica pločica ring sidriste sidrište'
      when 'Bušilice' then 'busilica bušilica hilti bosch makita gbh'
      when 'Baterije' then 'baterija baterije aku akumulator'
      when 'Punjači' then 'punjac punjač charger'
      when 'Svrdla' then 'svrdlo svrdla boreri boreri'
      when 'Mjerenje' then 'disto distox topodroid kompas klinometar mjerenje'
      when 'Crtaći pribor' then 'crtaci crtaći pribor olovka papir skica nacrt'
      when 'Lampe i rasvjeta' then 'lampa lampe rasvjeta svjetlo ceona čeona'
      when 'Odjeća i obuća' then 'kombinezon odijelo rukavice cizme čizme odjeca odjeća obuca obuća'
      else null
    end
  )
$$;

create or replace function public.sov_armory_group_display_name(main_category text, subcategory text, item_name text, search_basis text)
returns text
language sql
immutable
as $$
  select case
    -- SRT / osobna oprema komplet
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Descenderi' then 'Descenderi'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Croll / prsni blokeri' then 'Croll / prsni blokeri'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Ručni blokeri' then 'Ručni blokeri'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Pojasevi i sjedalice' then 'Pojasevi / sjedalice'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Pedale / stremeni' then 'Pedale / stremeni'
    -- postavljanje
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Karabineri' then 'Karabineri'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Spitovi i sidrišta' then 'Spitovi i sidrišta'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Pločice / ringovi' then 'Pločice / ringovi'
    -- osobna oprema
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Kacige' then 'Kacige'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Lampe i rasvjeta' then 'Lampe i rasvjeta'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Odjeća i obuća' then 'Odjeća i obuća'
    -- bušilice
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Bušilice' then 'Bušilice'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Baterije' then 'Baterije'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Punjači' then 'Punjači'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Svrdla' then 'Svrdla'
    -- crtanje
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Mjerenje' then 'Mjerenje / Disto / TopoDroid'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Crtaći pribor' then 'Crtaći pribor'
    -- užad: keep common articles grouped, real rope lengths stay as own rows through item_name
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Transportne vreće' then 'Transportne vreće'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Prusici' then 'Prusici'
    when public.sov_armory_subcategory(subcategory, search_basis) = 'Gurtne i trake' then 'Gurtne i trake'
    else trim(coalesce(nullif(item_name,''), 'Artikl'))
  end
$$;

create or replace function public.sov_armory_group_key(main_category text, subcategory text, item_name text, search_basis text)
returns text
language sql
immutable
as $$
  select public.sov_armory_norm(concat_ws('|',
    public.sov_armory_main_category(main_category, search_basis),
    public.sov_armory_subcategory(subcategory, search_basis),
    public.sov_armory_group_display_name(main_category, subcategory, item_name, search_basis)
  ))
$$;

drop view if exists public.sov_equipment_app_catalog_grouped cascade;
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
    trim(concat_ws(' · ', coalesce(nullif(r.name,''), nullif(r.model,''), 'Uže'), nullif(r.diameter_mm::text,''), case when nullif(r.length_m::text,'') is not null then (r.length_m::text || ' m') else null end)) as name,
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
    concat_ws(' ', r.name, r.sku, r.legacy_id, r.manufacturer, r.model, r.diameter_mm::text, r.length_m::text, r.location_name, r.status, r.availability, r.note, 'uže užad rope statičko') as basis
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
),
normalized_rows as (
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
    member_visible,
    basis
  from all_rows
)
select
  app_id,
  source_table,
  source_id,
  code,
  name,
  raw_category,
  raw_subcategory,
  main_category,
  subcategory,
  location_name,
  case
    when public.sov_can_manage_equipment() then total_qty
    when available_qty > 0 then 1
    else 0
  end as total_qty,
  case
    when public.sov_can_manage_equipment() then available_qty
    when available_qty > 0 then 1
    else 0
  end as available_qty,
  case
    when public.sov_can_manage_equipment() then status
    when available_qty > 0 then 'Dostupno'
    else 'Nije dostupno'
  end as status,
  case
    when public.sov_can_manage_equipment() then availability
    when available_qty > 0 then 'dostupno'
    else 'nedostupno'
  end as availability,
  unit,
  note,
  priority,
  public.sov_armory_norm(concat_ws(' ',
    code, name, raw_category, raw_subcategory, main_category, subcategory,
    public.sov_armory_group_display_name(main_category, subcategory, name, basis),
    location_name, status, availability, unit, note, basis,
    public.sov_armory_search_tags(main_category, subcategory, basis)
  )) as search_text,
  member_visible,
  case when main_category = 'Osobna oprema - komplet' then 'Osobna oprema - komplet' else null end as bundle_name,
  public.sov_armory_group_key(main_category, subcategory, name, basis) as catalog_group_key,
  public.sov_armory_group_display_name(main_category, subcategory, name, basis) as display_name,
  1::int as variant_count,
  concat_ws(' / ', nullif(code,''), nullif(name,''), nullif(location_name,''), nullif(status,'')) as detail_summary
from normalized_rows;

create view public.sov_equipment_app_catalog_grouped
with (security_invoker = true)
as
select
  ('group:' || catalog_group_key) as app_id,
  'equipment_group'::text as source_table,
  catalog_group_key as source_id,
  case when count(*) > 1 then (count(*)::text || ' stavki') else max(code) end as code,
  max(display_name) as name,
  max(raw_category) as raw_category,
  max(raw_subcategory) as raw_subcategory,
  main_category,
  subcategory,
  case when count(distinct location_name) = 1 then max(location_name) else 'više lokacija' end as location_name,
  sum(total_qty)::int as total_qty,
  sum(available_qty)::int as available_qty,
  case when sum(available_qty) > 0 then 'Dostupno' else 'Nije dostupno' end as status,
  case when sum(available_qty) > 0 then 'dostupno' else 'nedostupno' end as availability,
  max(unit) as unit,
  string_agg(detail_summary, ' · ' order by name, code) as note,
  min(priority) as priority,
  public.sov_armory_norm(string_agg(search_text, ' ') || ' ' || max(display_name)) as search_text,
  bool_or(member_visible) as member_visible,
  max(bundle_name) as bundle_name,
  catalog_group_key,
  max(display_name) as display_name,
  count(*)::int as variant_count,
  string_agg(detail_summary, ' · ' order by name, code) as detail_summary
from public.sov_equipment_app_catalog
group by catalog_group_key, main_category, subcategory;

grant select on public.sov_equipment_app_catalog_grouped to authenticated;

grant select on public.sov_equipment_app_catalog to authenticated;

-- Sanity checks after running:
-- 1) croll should not return ropes:
-- select name, main_category, subcategory from public.sov_equipment_app_catalog where search_text like '%croll%' limit 50;
-- 2) counts. As Admin/Oružar this shows exact quantities; as User only masked 0/1 availability:
-- select main_category, count(*) rows, sum(total_qty) total_qty, sum(available_qty) available_qty
-- from public.sov_equipment_app_catalog
-- group by main_category
-- order by min(priority), main_category;
