-- SOV Oružarstvo v5.47 — LARGE DB CONSOLIDATION / UNIFIED ASSETS
-- Run in Supabase SQL editor with RLS enabled.
-- DATA-SAFE PHASE 1:
--   - creates one canonical physical table: public.equipment_assets
--   - backfills it from legacy equipment_items / equipment_ropes / equipment_pieces
--   - keeps old legacy tables intact for compatibility/import rollback
--   - APK/Web catalog views now read from equipment_assets
--   - legacy tables get sync triggers so imports/old writes still update the unified table
--   - does NOT drop or rename legacy tables.
--
-- After this, the source of truth for app/web catalog is equipment_assets.
-- Legacy tables become import/compatibility surfaces until we explicitly retire them later.

-- SOV Oružarstvo v5.45 — ARMORY CORE CLEANUP
-- Run in Supabase SQL editor with RLS enabled.
-- Keeps SQL as the single canonical armory brain for APK/Web grouping/search; raw catalog stays for inventory.
-- Regular users still see only Dostupno / Nije dostupno through masked total_qty / available_qty.
-- RLS stays enabled; this recreates catalog views/helpers and aligns request compatibility/statuses.

create extension if not exists pgcrypto;

-- v5.45 request compatibility: web historically used item_name, APK used name.
-- Keep both columns so multi-artikl requests work from both surfaces.
alter table public.equipment_request_items
  add column if not exists name text,
  add column if not exists item_name text,
  add column if not exists unit text default 'kom';

-- v5.45 status cleanup: approved/prepared/reserved were a dead virtual step.
-- They remain readable as pending/requested in clients; existing rows are normalized to pending.
update public.equipment_requests
set status = 'pending', updated_at = coalesce(updated_at, now())
where lower(coalesce(status,'')) in ('approved','prepared','reserved','odobreno');

create or replace function public.sov_armory_request_status_ui(status text)
returns text
language sql
immutable
as $$
  select case lower(coalesce(status,'pending'))
    when 'requested' then 'pending'
    when 'approved' then 'pending'
    when 'prepared' then 'pending'
    when 'reserved' then 'pending'
    when 'issued' then 'issued'
    when 'partial_return' then 'partial_return'
    when 'partial' then 'partial_return'
    when 'returned' then 'returned'
    when 'closed' then 'returned'
    when 'cancelled' then 'cancelled'
    when 'canceled' then 'cancelled'
    when 'rejected' then 'cancelled'
    else coalesce(nullif(lower(status),''),'pending')
  end
$$;


alter table public.equipment_items
  add column if not exists subcategory text,
  add column if not exists sku text,
  add column if not exists unit text default 'kom',
  add column if not exists member_visible boolean not null default true,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

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
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

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
  add column if not exists note text,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

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



-- Ensure role helper exists even if the inventory SQL was skipped.
create or replace function public.sov_can_manage_equipment()
returns boolean
language sql
security definer
set search_path = public
as $$
  select public.sov_has_role(array['admin','oruzar'])
$$;

-- Schema compatibility for legacy tables before migration/sync.
alter table public.equipment_items
  add column if not exists sku text,
  add column if not exists unit text default 'kom',
  add column if not exists subcategory text,
  add column if not exists location_name text,
  add column if not exists availability text default 'dostupno',
  add column if not exists manufacturer text,
  add column if not exists model text,
  add column if not exists internal_note text,
  add column if not exists note text,
  add column if not exists category_id uuid,
  add column if not exists category_name text,
  add column if not exists catalog_id text,
  add column if not exists tracking_type text default 'po vrsti',
  add column if not exists quantity numeric default 0,
  add column if not exists loaned numeric default 0,
  add column if not exists available numeric default 0,
  add column if not exists minimum numeric,
  add column if not exists status text default 'aktivno',
  add column if not exists source_sheet text,
  add column if not exists member_visible boolean not null default true;

alter table public.equipment_ropes
  add column if not exists legacy_id text,
  add column if not exists sku text,
  add column if not exists name text,
  add column if not exists manufacturer text,
  add column if not exists model text,
  add column if not exists diameter_mm text,
  add column if not exists length_m numeric,
  add column if not exists standard text,
  add column if not exists production_year integer,
  add column if not exists in_use_since date,
  add column if not exists color text,
  add column if not exists supplier text,
  add column if not exists location_name text,
  add column if not exists status text default 'U društvu',
  add column if not exists availability text default 'dostupno',
  add column if not exists note text;

alter table public.equipment_pieces
  add column if not exists legacy_id text,
  add column if not exists catalog_legacy_id text,
  add column if not exists equipment_item_id uuid,
  add column if not exists sku text,
  add column if not exists serial_number text,
  add column if not exists category_name text,
  add column if not exists subcategory text,
  add column if not exists manufacturer text,
  add column if not exists model text,
  add column if not exists purchase_date date,
  add column if not exists location_id uuid,
  add column if not exists location_name text,
  add column if not exists status text default 'U društvu',
  add column if not exists availability text default 'dostupno',
  add column if not exists next_service date,
  add column if not exists note text;

create table if not exists public.equipment_assets (
  id uuid primary key default gen_random_uuid(),
  asset_type text not null default 'item', -- item / rope / piece
  legacy_source_table text not null,
  legacy_source_id text not null,
  legacy_id text,
  catalog_id text,
  catalog_legacy_id text,
  sku text,
  serial_number text,
  code text,
  name text not null,
  raw_category text,
  raw_subcategory text,
  main_category text,
  subcategory text,
  display_name text,
  catalog_group_key text,
  location_name text,
  total_qty numeric not null default 0,
  available_qty numeric not null default 0,
  loaned_qty numeric not null default 0,
  minimum_qty numeric,
  unit text not null default 'kom',
  tracking_type text default 'po vrsti',
  member_visible boolean not null default true,
  status text not null default 'aktivno',
  availability text not null default 'dostupno',
  manufacturer text,
  model text,
  diameter_mm text,
  length_m numeric,
  standard text,
  production_year integer,
  in_use_since date,
  color text,
  supplier text,
  purchase_date date,
  next_service date,
  note text,
  internal_note text,
  source_sheet text,
  meta jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (legacy_source_table, legacy_source_id)
);

alter table public.equipment_assets
  add column if not exists asset_type text not null default 'item',
  add column if not exists legacy_source_table text,
  add column if not exists legacy_source_id text,
  add column if not exists legacy_id text,
  add column if not exists catalog_id text,
  add column if not exists catalog_legacy_id text,
  add column if not exists sku text,
  add column if not exists serial_number text,
  add column if not exists code text,
  add column if not exists name text,
  add column if not exists raw_category text,
  add column if not exists raw_subcategory text,
  add column if not exists main_category text,
  add column if not exists subcategory text,
  add column if not exists display_name text,
  add column if not exists catalog_group_key text,
  add column if not exists location_name text,
  add column if not exists total_qty numeric not null default 0,
  add column if not exists available_qty numeric not null default 0,
  add column if not exists loaned_qty numeric not null default 0,
  add column if not exists minimum_qty numeric,
  add column if not exists unit text not null default 'kom',
  add column if not exists tracking_type text default 'po vrsti',
  add column if not exists member_visible boolean not null default true,
  add column if not exists status text not null default 'aktivno',
  add column if not exists availability text not null default 'dostupno',
  add column if not exists manufacturer text,
  add column if not exists model text,
  add column if not exists diameter_mm text,
  add column if not exists length_m numeric,
  add column if not exists standard text,
  add column if not exists production_year integer,
  add column if not exists in_use_since date,
  add column if not exists color text,
  add column if not exists supplier text,
  add column if not exists purchase_date date,
  add column if not exists next_service date,
  add column if not exists note text,
  add column if not exists internal_note text,
  add column if not exists source_sheet text,
  add column if not exists meta jsonb not null default '{}'::jsonb,
  add column if not exists created_at timestamptz not null default now(),
  add column if not exists updated_at timestamptz not null default now();

create unique index if not exists equipment_assets_legacy_source_uidx
  on public.equipment_assets(legacy_source_table, legacy_source_id);
create index if not exists equipment_assets_type_idx on public.equipment_assets(asset_type);
create index if not exists equipment_assets_group_idx on public.equipment_assets(catalog_group_key);
create index if not exists equipment_assets_category_idx on public.equipment_assets(main_category, subcategory);
create index if not exists equipment_assets_updated_idx on public.equipment_assets(updated_at desc);

create or replace function public.sov_armory_asset_compute_fields()
returns trigger
language plpgsql
as $$
begin
  new.code := coalesce(nullif(new.code,''), nullif(new.sku,''), nullif(new.legacy_id,''), nullif(new.serial_number,''), new.legacy_source_id);
  new.raw_category := coalesce(nullif(new.raw_category,''), 'Ostalo');
  new.raw_subcategory := nullif(new.raw_subcategory,'');
  new.unit := coalesce(nullif(new.unit,''), 'kom');
  new.total_qty := greatest(coalesce(new.total_qty,0),0);
  new.available_qty := least(greatest(coalesce(new.available_qty, new.total_qty,0),0), greatest(coalesce(new.total_qty,0),0));
  new.loaned_qty := greatest(coalesce(new.loaned_qty, greatest(new.total_qty - new.available_qty,0)),0);
  new.status := coalesce(nullif(new.status,''), 'aktivno');
  new.availability := coalesce(nullif(new.availability,''), case when new.available_qty > 0 then 'dostupno' else 'nedostupno' end);
  new.main_category := public.sov_armory_main_category(new.raw_category, concat_ws(' ', new.name, new.code, new.raw_category, new.raw_subcategory, new.note, new.internal_note, new.manufacturer, new.model));
  new.subcategory := public.sov_armory_subcategory(new.raw_subcategory, concat_ws(' ', new.name, new.code, new.raw_category, new.raw_subcategory, new.note, new.internal_note, new.manufacturer, new.model));
  new.display_name := public.sov_armory_group_display_name(new.main_category, new.subcategory, new.name, concat_ws(' ', new.name, new.code, new.raw_category, new.raw_subcategory, new.note, new.internal_note, new.manufacturer, new.model));
  new.catalog_group_key := public.sov_armory_group_key(new.main_category, new.subcategory, new.name, concat_ws(' ', new.name, new.code, new.raw_category, new.raw_subcategory, new.note, new.internal_note, new.manufacturer, new.model));
  new.updated_at := now();
  return new;
end;
$$;

drop trigger if exists equipment_assets_compute_fields_trg on public.equipment_assets;
create trigger equipment_assets_compute_fields_trg
before insert or update on public.equipment_assets
for each row execute function public.sov_armory_asset_compute_fields();

create or replace function public.sov_armory_upsert_item_asset(p_id uuid)
returns void
language plpgsql
as $$
begin
  insert into public.equipment_assets (
    asset_type, legacy_source_table, legacy_source_id, legacy_id, catalog_id, sku, code, name,
    raw_category, raw_subcategory, location_name, total_qty, available_qty, loaned_qty, minimum_qty,
    unit, tracking_type, member_visible, status, availability, manufacturer, model, note, internal_note,
    source_sheet, meta, created_at, updated_at
  )
  select
    'item', 'equipment_items', i.id::text, i.legacy_id, i.catalog_id, i.sku,
    coalesce(nullif(i.sku,''), nullif(i.legacy_id,''), nullif(i.catalog_id,''), i.id::text),
    coalesce(nullif(i.name,''), nullif(i.sku,''), nullif(i.legacy_id,''), 'Artikl'),
    coalesce(nullif(i.category_name,''), 'Ostalo'), nullif(i.subcategory,''), coalesce(nullif(i.location_name,''),'Nije upisano'),
    greatest(coalesce(i.quantity,0),0),
    least(greatest(coalesce(i.available, i.quantity,0),0), greatest(coalesce(i.quantity,0),0)),
    greatest(coalesce(i.loaned, greatest(coalesce(i.quantity,0)-coalesce(i.available,0),0)),0),
    i.minimum,
    coalesce(nullif(i.unit,''),'kom'), coalesce(nullif(i.tracking_type,''),'po vrsti'), coalesce(i.member_visible,true),
    coalesce(nullif(i.status,''),'aktivno'), coalesce(nullif(i.availability,''), case when coalesce(i.available,0)>0 then 'dostupno' else 'nedostupno' end),
    nullif(i.manufacturer,''), nullif(i.model,''), nullif(i.note,''), nullif(i.internal_note,''), nullif(i.source_sheet,''),
    jsonb_strip_nulls(jsonb_build_object('category_id', i.category_id, 'source', 'equipment_items')),
    coalesce(i.created_at, now()), coalesce(i.updated_at, now())
  from public.equipment_items i
  where i.id = p_id
  on conflict (legacy_source_table, legacy_source_id) do update set
    legacy_id=excluded.legacy_id, catalog_id=excluded.catalog_id, sku=excluded.sku, code=excluded.code,
    name=excluded.name, raw_category=excluded.raw_category, raw_subcategory=excluded.raw_subcategory,
    location_name=excluded.location_name, total_qty=excluded.total_qty, available_qty=excluded.available_qty,
    loaned_qty=excluded.loaned_qty, minimum_qty=excluded.minimum_qty, unit=excluded.unit,
    tracking_type=excluded.tracking_type, member_visible=excluded.member_visible, status=excluded.status,
    availability=excluded.availability, manufacturer=excluded.manufacturer, model=excluded.model,
    note=excluded.note, internal_note=excluded.internal_note, source_sheet=excluded.source_sheet,
    meta=excluded.meta, updated_at=now();
end;
$$;

create or replace function public.sov_armory_upsert_rope_asset(p_id uuid)
returns void
language plpgsql
as $$
begin
  insert into public.equipment_assets (
    asset_type, legacy_source_table, legacy_source_id, legacy_id, sku, code, name,
    raw_category, raw_subcategory, location_name, total_qty, available_qty, loaned_qty,
    unit, tracking_type, member_visible, status, availability, manufacturer, model,
    diameter_mm, length_m, standard, production_year, in_use_since, color, supplier, note,
    meta, created_at, updated_at
  )
  select
    'rope', 'equipment_ropes', r.id::text, r.legacy_id, r.sku,
    coalesce(nullif(r.sku,''), nullif(r.legacy_id,''), r.id::text),
    trim(concat_ws(' · ', coalesce(nullif(r.name,''), nullif(r.model,''), 'Uže'), nullif(r.diameter_mm::text,''), case when nullif(r.length_m::text,'') is not null then (r.length_m::text || ' m') else null end)),
    'Užad i užetna oprema', 'Užad', coalesce(nullif(r.location_name,''),'Nije upisano'),
    1,
    case when public.sov_armory_norm(coalesce(r.status,'') || ' ' || coalesce(r.availability,'')) ~ '(posud|izdan|vani|servis|otpis|izgubl)' then 0 else 1 end,
    case when public.sov_armory_norm(coalesce(r.status,'') || ' ' || coalesce(r.availability,'')) ~ '(posud|izdan|vani|servis|otpis|izgubl)' then 1 else 0 end,
    'kom', 'po komadu', true, coalesce(nullif(r.status,''),'U društvu'), coalesce(nullif(r.availability,''),'dostupno'),
    nullif(r.manufacturer,''), nullif(r.model,''), nullif(r.diameter_mm::text,''), nullif(r.length_m::text,'')::numeric,
    nullif(r.standard,''), r.production_year, r.in_use_since, nullif(r.color,''), nullif(r.supplier,''), nullif(r.note,''),
    jsonb_strip_nulls(jsonb_build_object('source', 'equipment_ropes')),
    coalesce(r.created_at, now()), coalesce(r.updated_at, now())
  from public.equipment_ropes r
  where r.id = p_id
  on conflict (legacy_source_table, legacy_source_id) do update set
    legacy_id=excluded.legacy_id, sku=excluded.sku, code=excluded.code, name=excluded.name,
    raw_category=excluded.raw_category, raw_subcategory=excluded.raw_subcategory, location_name=excluded.location_name,
    total_qty=excluded.total_qty, available_qty=excluded.available_qty, loaned_qty=excluded.loaned_qty,
    unit=excluded.unit, tracking_type=excluded.tracking_type, member_visible=excluded.member_visible,
    status=excluded.status, availability=excluded.availability, manufacturer=excluded.manufacturer,
    model=excluded.model, diameter_mm=excluded.diameter_mm, length_m=excluded.length_m, standard=excluded.standard,
    production_year=excluded.production_year, in_use_since=excluded.in_use_since, color=excluded.color,
    supplier=excluded.supplier, note=excluded.note, meta=excluded.meta, updated_at=now();
end;
$$;

create or replace function public.sov_armory_upsert_piece_asset(p_id uuid)
returns void
language plpgsql
as $$
begin
  insert into public.equipment_assets (
    asset_type, legacy_source_table, legacy_source_id, legacy_id, catalog_legacy_id, sku, serial_number, code, name,
    raw_category, raw_subcategory, location_name, total_qty, available_qty, loaned_qty,
    unit, tracking_type, member_visible, status, availability, manufacturer, model, purchase_date, next_service, note,
    meta, created_at, updated_at
  )
  select
    'piece', 'equipment_pieces', p.id::text, p.legacy_id, p.catalog_legacy_id, p.sku, p.serial_number,
    coalesce(nullif(p.sku,''), nullif(p.legacy_id,''), nullif(p.serial_number,''), p.id::text),
    coalesce(nullif(p.name,''), nullif(p.model,''), nullif(p.serial_number,''), 'Komad opreme'),
    coalesce(nullif(p.category_name,''), 'Ostalo'), nullif(p.subcategory,''), coalesce(nullif(p.location_name,''),'Nije upisano'),
    1,
    case when public.sov_armory_norm(coalesce(p.status,'') || ' ' || coalesce(p.availability,'')) ~ '(posud|izdan|vani|servis|otpis|izgubl)' then 0 else 1 end,
    case when public.sov_armory_norm(coalesce(p.status,'') || ' ' || coalesce(p.availability,'')) ~ '(posud|izdan|vani|servis|otpis|izgubl)' then 1 else 0 end,
    'kom', 'po komadu', public.sov_can_manage_equipment(), coalesce(nullif(p.status,''),'U društvu'), coalesce(nullif(p.availability,''),'dostupno'),
    nullif(p.manufacturer,''), nullif(p.model,''), p.purchase_date, p.next_service, nullif(p.note,''),
    jsonb_strip_nulls(jsonb_build_object('equipment_item_id', p.equipment_item_id, 'location_id', p.location_id, 'source', 'equipment_pieces')),
    coalesce(p.created_at, now()), coalesce(p.updated_at, now())
  from public.equipment_pieces p
  where p.id = p_id
  on conflict (legacy_source_table, legacy_source_id) do update set
    legacy_id=excluded.legacy_id, catalog_legacy_id=excluded.catalog_legacy_id, sku=excluded.sku,
    serial_number=excluded.serial_number, code=excluded.code, name=excluded.name, raw_category=excluded.raw_category,
    raw_subcategory=excluded.raw_subcategory, location_name=excluded.location_name, total_qty=excluded.total_qty,
    available_qty=excluded.available_qty, loaned_qty=excluded.loaned_qty, unit=excluded.unit,
    tracking_type=excluded.tracking_type, member_visible=excluded.member_visible, status=excluded.status,
    availability=excluded.availability, manufacturer=excluded.manufacturer, model=excluded.model,
    purchase_date=excluded.purchase_date, next_service=excluded.next_service, note=excluded.note,
    meta=excluded.meta, updated_at=now();
end;
$$;

create or replace function public.sov_armory_refresh_assets_from_legacy()
returns table(source_table text, rows_synced bigint)
language plpgsql
security definer
set search_path = public
as $$
declare
  c_items bigint := 0;
  c_ropes bigint := 0;
  c_pieces bigint := 0;
  r record;
begin
  for r in select id from public.equipment_items loop
    perform public.sov_armory_upsert_item_asset(r.id);
    c_items := c_items + 1;
  end loop;
  for r in select id from public.equipment_ropes loop
    perform public.sov_armory_upsert_rope_asset(r.id);
    c_ropes := c_ropes + 1;
  end loop;
  for r in select id from public.equipment_pieces loop
    perform public.sov_armory_upsert_piece_asset(r.id);
    c_pieces := c_pieces + 1;
  end loop;

  -- Legacy deletions: remove unified assets whose original row is gone.
  delete from public.equipment_assets a
  where a.legacy_source_table = 'equipment_items'
    and not exists (select 1 from public.equipment_items i where i.id::text = a.legacy_source_id);
  delete from public.equipment_assets a
  where a.legacy_source_table = 'equipment_ropes'
    and not exists (select 1 from public.equipment_ropes r where r.id::text = a.legacy_source_id);
  delete from public.equipment_assets a
  where a.legacy_source_table = 'equipment_pieces'
    and not exists (select 1 from public.equipment_pieces p where p.id::text = a.legacy_source_id);

  return query select 'equipment_items'::text, c_items;
  return query select 'equipment_ropes'::text, c_ropes;
  return query select 'equipment_pieces'::text, c_pieces;
end;
$$;

create or replace function public.sov_armory_legacy_item_asset_trg()
returns trigger
language plpgsql
as $$
begin
  if TG_OP = 'DELETE' then
    delete from public.equipment_assets where legacy_source_table='equipment_items' and legacy_source_id=old.id::text;
    return old;
  end if;
  perform public.sov_armory_upsert_item_asset(new.id);
  return new;
end;
$$;

create or replace function public.sov_armory_legacy_rope_asset_trg()
returns trigger
language plpgsql
as $$
begin
  if TG_OP = 'DELETE' then
    delete from public.equipment_assets where legacy_source_table='equipment_ropes' and legacy_source_id=old.id::text;
    return old;
  end if;
  perform public.sov_armory_upsert_rope_asset(new.id);
  return new;
end;
$$;

create or replace function public.sov_armory_legacy_piece_asset_trg()
returns trigger
language plpgsql
as $$
begin
  if TG_OP = 'DELETE' then
    delete from public.equipment_assets where legacy_source_table='equipment_pieces' and legacy_source_id=old.id::text;
    return old;
  end if;
  perform public.sov_armory_upsert_piece_asset(new.id);
  return new;
end;
$$;

drop trigger if exists equipment_items_to_assets_trg on public.equipment_items;
create trigger equipment_items_to_assets_trg
after insert or update or delete on public.equipment_items
for each row execute function public.sov_armory_legacy_item_asset_trg();

drop trigger if exists equipment_ropes_to_assets_trg on public.equipment_ropes;
create trigger equipment_ropes_to_assets_trg
after insert or update or delete on public.equipment_ropes
for each row execute function public.sov_armory_legacy_rope_asset_trg();

drop trigger if exists equipment_pieces_to_assets_trg on public.equipment_pieces;
create trigger equipment_pieces_to_assets_trg
after insert or update or delete on public.equipment_pieces
for each row execute function public.sov_armory_legacy_piece_asset_trg();

-- Initial backfill / resync.
select * from public.sov_armory_refresh_assets_from_legacy();

-- Optional forward-looking links for requests/inventory history.
alter table public.equipment_request_items
  add column if not exists equipment_asset_id uuid references public.equipment_assets(id) on delete set null,
  add column if not exists catalog_group_key text,
  add column if not exists display_name text;

create table if not exists public.equipment_inventory_sessions (
  id uuid primary key default gen_random_uuid(),
  location_name text not null default 'Sve lokacije',
  category_name text not null default 'Sve kategorije',
  status text not null default 'closed',
  started_by uuid default auth.uid(),
  started_by_email text,
  started_by_name text,
  item_count integer not null default 0,
  mismatch_count integer not null default 0,
  shortage_count integer not null default 0,
  surplus_count integer not null default 0,
  note text,
  synced_from text not null default 'android',
  started_at timestamptz not null default now(),
  closed_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create table if not exists public.equipment_inventory_counts (
  id uuid primary key default gen_random_uuid(),
  session_id uuid references public.equipment_inventory_sessions(id) on delete cascade,
  app_id text,
  source_table text,
  source_id text,
  item_code text,
  item_name text not null default 'Oprema',
  category_name text,
  subcategory text,
  location_name text,
  expected_qty integer not null default 0,
  counted_qty integer not null default 0,
  unit text not null default 'kom',
  note text,
  created_at timestamptz not null default now()
);

alter table public.equipment_inventory_counts
  add column if not exists equipment_asset_id uuid references public.equipment_assets(id) on delete set null,
  add column if not exists catalog_group_key text;

update public.equipment_request_items ri
set equipment_asset_id = a.id,
    catalog_group_key = coalesce(ri.catalog_group_key, a.catalog_group_key),
    display_name = coalesce(ri.display_name, a.display_name)
from public.equipment_assets a
where ri.equipment_asset_id is null
  and (
    lower(coalesce(ri.equipment_legacy_id,'')) = lower(coalesce(a.code,''))
    or lower(coalesce(ri.equipment_legacy_id,'')) = lower(coalesce(a.legacy_id,''))
    or lower(coalesce(ri.name, ri.item_name,'')) = lower(coalesce(a.display_name, a.name,''))
  );

alter table public.equipment_assets enable row level security;

drop policy if exists equipment_assets_read_approved on public.equipment_assets;
drop policy if exists equipment_assets_manage_armory on public.equipment_assets;

create policy equipment_assets_read_approved
on public.equipment_assets
for select
using (public.sov_is_approved() and (member_visible = true or public.sov_can_manage_equipment()));

create policy equipment_assets_manage_armory
on public.equipment_assets
for all
using (public.sov_can_manage_equipment())
with check (public.sov_can_manage_equipment());

grant select on public.equipment_assets to authenticated;
grant insert, update, delete on public.equipment_assets to authenticated;

-- Canonical app catalog now reads the unified table only.
drop view if exists public.sov_equipment_app_catalog_grouped cascade;
drop view if exists public.sov_equipment_app_catalog cascade;

create view public.sov_equipment_app_catalog
with (security_invoker = true)
as
with visible_assets as (
  select *
  from public.equipment_assets a
  where coalesce(a.member_visible, true) = true or public.sov_can_manage_equipment()
), normalized as (
  select
    a.id,
    a.asset_type,
    a.legacy_source_table,
    a.legacy_source_id,
    a.legacy_id,
    a.sku,
    a.serial_number,
    coalesce(nullif(a.code,''), nullif(a.sku,''), nullif(a.legacy_id,''), nullif(a.serial_number,''), a.id::text) as code,
    coalesce(nullif(a.name,''), 'Artikl') as name,
    coalesce(nullif(a.raw_category,''), 'Ostalo') as raw_category,
    nullif(a.raw_subcategory,'') as raw_subcategory,
    public.sov_armory_main_category(coalesce(nullif(a.raw_category,''), 'Ostalo'), concat_ws(' ', a.name, a.code, a.raw_category, a.raw_subcategory, a.note, a.internal_note, a.manufacturer, a.model, a.meta::text)) as main_category,
    public.sov_armory_subcategory(a.raw_subcategory, concat_ws(' ', a.name, a.code, a.raw_category, a.raw_subcategory, a.note, a.internal_note, a.manufacturer, a.model, a.meta::text)) as subcategory,
    coalesce(nullif(a.location_name,''),'Nije upisano') as location_name,
    greatest(coalesce(a.total_qty,0),0)::int as total_qty,
    least(greatest(coalesce(a.available_qty, a.total_qty,0),0), greatest(coalesce(a.total_qty,0),0))::int as available_qty,
    coalesce(nullif(a.status,''), 'aktivno') as status,
    coalesce(nullif(a.availability,''), case when coalesce(a.available_qty,0) > 0 then 'dostupno' else 'nedostupno' end) as availability,
    coalesce(nullif(a.unit,''),'kom') as unit,
    concat_ws(' · ', nullif(a.note,''), nullif(a.internal_note,'')) as note,
    a.member_visible,
    concat_ws(' ', a.name, a.code, a.sku, a.legacy_id, a.serial_number, a.raw_category, a.raw_subcategory, a.location_name, a.status, a.availability, a.note, a.internal_note, a.manufacturer, a.model, a.meta::text) as basis
  from visible_assets a
)
select
  case
    when legacy_source_table = 'equipment_items' then ('item:' || legacy_source_id)
    when legacy_source_table = 'equipment_ropes' then ('rope:' || legacy_source_id)
    when legacy_source_table = 'equipment_pieces' then ('piece:' || legacy_source_id)
    else ('asset:' || id::text)
  end as app_id,
  legacy_source_table as source_table,
  legacy_source_id as source_id,
  code,
  name,
  raw_category,
  raw_subcategory,
  main_category,
  subcategory,
  location_name,
  case when public.sov_can_manage_equipment() then total_qty when available_qty > 0 then 1 else 0 end as total_qty,
  case when public.sov_can_manage_equipment() then available_qty when available_qty > 0 then 1 else 0 end as available_qty,
  case when public.sov_can_manage_equipment() then status when available_qty > 0 then 'Dostupno' else 'Nije dostupno' end as status,
  case when public.sov_can_manage_equipment() then availability when available_qty > 0 then 'dostupno' else 'nedostupno' end as availability,
  unit,
  note,
  public.sov_armory_category_priority(main_category) as priority,
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
  concat_ws(' / ', nullif(code,''), nullif(name,''), nullif(location_name,''), nullif(status,'')) as detail_summary,
  id as equipment_asset_id,
  asset_type
from normalized;

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
  string_agg(detail_summary, ' · ' order by name, code) as detail_summary,
  null::uuid as equipment_asset_id,
  'group'::text as asset_type
from public.sov_equipment_app_catalog
where member_visible = true or public.sov_can_manage_equipment()
group by catalog_group_key, main_category, subcategory;

grant select on public.sov_equipment_app_catalog to authenticated;
grant select on public.sov_equipment_app_catalog_grouped to authenticated;

create or replace view public.sov_equipment_db_consolidation_health
with (security_invoker = true)
as
select 'equipment_assets'::text as surface, count(*)::bigint as rows from public.equipment_assets
union all select 'legacy equipment_items', count(*) from public.equipment_items
union all select 'legacy equipment_ropes', count(*) from public.equipment_ropes
union all select 'legacy equipment_pieces', count(*) from public.equipment_pieces
union all select 'app catalog raw view', count(*) from public.sov_equipment_app_catalog
union all select 'app catalog grouped view', count(*) from public.sov_equipment_app_catalog_grouped;

grant select on public.sov_equipment_db_consolidation_health to authenticated;

-- Smoke checks after running:
-- select * from public.sov_equipment_db_consolidation_health;
-- select main_category, count(*) rows, sum(total_qty) total_qty, sum(available_qty) available_qty
-- from public.sov_equipment_app_catalog group by main_category order by min(priority), main_category;
-- select name, main_category, subcategory from public.sov_equipment_app_catalog_grouped where search_text like '%croll%' limit 20;
