-- SOV Oružarstvo v6.1.5 — taxonomy cleanup / anti-chaos patch
-- Safe/aditive: creates canonical taxonomy helpers and applies categories only where known columns exist.
-- No destructive drops, no data deletion.

create extension if not exists pgcrypto;

create schema if not exists private;

create or replace function public.sov_armory_norm(value text)
returns text
language sql
immutable
as $fn$
  select lower(translate(coalesce(value, ''), 'ČĆŠŽĐčćšžđ', 'CCSZDccszd'))
$fn$;

create table if not exists public.sov_armory_taxonomy (
  id uuid primary key default gen_random_uuid(),
  name text not null unique,
  description text,
  icon text,
  sort_order int not null default 999,
  active boolean not null default true,
  taxonomy_version text not null default '6.1.5',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

insert into public.sov_armory_taxonomy (name, description, icon, sort_order, taxonomy_version)
values
  ('Osobna oprema','Pojasevi, crollovi, descenderi, blokeri, kacige i osobna speleo oprema.','🧑‍🚒',10,'6.1.5'),
  ('Užad i užetna oprema','Užad, prusici, gurtne, transportne vreće i užetni pribor.','🪢',20,'6.1.5'),
  ('Oprema za postavljanje','Spitovi, pločice, ringovi, sidrišta, karabineri i rigging pribor.','⚓',30,'6.1.5'),
  ('Oprema za crtanje','Busole/kompasi, Suunto, DistoX, TopoDroid i crtaći pribor.','📐',40,'6.1.5'),
  ('Bušilice i svrdla','Bušilice, svrdla, SDS pribor, Bosch/Hilti/Makita baterije i punjači.','🔩',50,'6.1.5'),
  ('Elektro, rasvjeta i foto','Lampe, obične baterije AA/AAA/9V, USB, punjači, komunikacija, foto i video.','🔦',60,'6.1.5'),
  ('Dronovi','Dronovi, dron baterije, punjači, elise, kontroleri i transportni koferi.','🚁',70,'6.1.5'),
  ('Oprema za logor','Logor, kuhinja, voda, spavanje, terenski boravak i higijena.','⛺',80,'6.1.5'),
  ('Oprema za proširivanje','Čekići, macole, dlijeta i oprema za proširivanje.','🔨',90,'6.1.5'),
  ('Medicinska oprema','Prva pomoć, sanitet i medicinski kompleti.','🧰',100,'6.1.5'),
  ('Alat i radionica','Opći alat, radionica, servis i popravci.','🧰',110,'6.1.5'),
  ('Alpinistička oprema','Alpinistička i penjačka oprema koja nije standardna speleo osobna oprema.','⛰️',120,'6.1.5'),
  ('Ronilačka oprema','Ronilačka oprema, neopreni, maske, peraje i boce.','🤿',130,'6.1.5'),
  ('Čisto podzemlje','Vreće, rukavice i oprema za akcije čišćenja.','🧹',140,'6.1.5'),
  ('Ostalo / provjeriti','Stavke koje još treba ručno provjeriti.','📦',999,'6.1.5')
on conflict (name) do update set
  description = excluded.description,
  icon = excluded.icon,
  sort_order = excluded.sort_order,
  active = true,
  taxonomy_version = '6.1.5',
  updated_at = now();

create or replace function public.sov_armory_main_category_v615(raw_category text, search_basis text)
returns text
language sql
immutable
as $fn$
with t as (
  select trim(coalesce(raw_category,'')) raw,
         public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) x
), flags as (
  select *,
    x ~ '(^| )(aa|aaa|9v|18650|cr123|cr2032)( |$)|baterij[ae]?[[:space:]]+(aa|aaa|9v)|punjac.*(aa|aaa)|mini usb|usb kabel|solarni punjac|powerbank' ordinary_battery,
    x ~ '(bosch|hilti|makita|gbh|sds|busil|busilic|buzil|svrd|borer|akumulatorsk)' drill_context,
    x ~ '(dron|dji|phantom|mavic|spark|gl300|elisa|propeler|ph4c|ade019|4480mah|5350mah)' drone_context
  from t
)
select case
  when raw in ('Osobna oprema','Užad i užetna oprema','Oprema za postavljanje','Oprema za crtanje','Bušilice i svrdla','Elektro, rasvjeta i foto','Dronovi','Oprema za logor','Oprema za proširivanje','Medicinska oprema','Alat i radionica','Alpinistička oprema','Ronilačka oprema','Čisto podzemlje','Ostalo / provjeriti') then raw
  when raw = 'Užad' then 'Užad i užetna oprema'
  when public.sov_armory_norm(raw) like '%busilice i baterije%' and drone_context then 'Dronovi'
  when public.sov_armory_norm(raw) like '%busilice i baterije%' and ordinary_battery and not drill_context then 'Elektro, rasvjeta i foto'
  when public.sov_armory_norm(raw) like '%busilice i baterije%' then 'Bušilice i svrdla'
  when public.sov_armory_norm(raw) in ('elektro i foto','elektro i foto oprema','rasvjeta') then 'Elektro, rasvjeta i foto'
  when public.sov_armory_norm(raw) in ('ostali alat','alat','ostalo','razno') then 'Alat i radionica'
  when public.sov_armory_norm(raw) ~ 'medicin' then 'Medicinska oprema'
  when public.sov_armory_norm(raw) ~ '(logor|kamp)' then 'Oprema za logor'
  when public.sov_armory_norm(raw) ~ 'prosir' then 'Oprema za proširivanje'
  when public.sov_armory_norm(raw) ~ 'alpin' then 'Alpinistička oprema'
  when public.sov_armory_norm(raw) ~ 'ronil' then 'Ronilačka oprema'
  when public.sov_armory_norm(raw) ~ 'cisto podzemlje' then 'Čisto podzemlje'
  when drone_context then 'Dronovi'
  when x ~ '(kompas|busol|suunto|disto|distox|topodroid|klinomet|laser|nacrt|skic|olov)' then 'Oprema za crtanje'
  when ordinary_battery and not drill_context then 'Elektro, rasvjeta i foto'
  when x ~ '(lampa|rasvjet|svjetl|ceona|kamera|foto|video|walkie|radio stanica|punjenje walkie)' then 'Elektro, rasvjeta i foto'
  when drill_context and x ~ '(bater|aku|punjac|busil|svrd|borer|sds)' then 'Bušilice i svrdla'
  when x ~ '(spit|sidr|sidrist|anker|bolt|ploc|ring|fikser|karab|hms|matica|maillon|omni|triact|trilock)' then 'Oprema za postavljanje'
  when x ~ '(croll|krol|crol|prsni|descender|spustal|(^| )stop($| )|rig|maestro|(^| )id($| )|zumar|jumar|ascender|rucni bloker|(^| )bloker|pedal|stremen|pupak|pupcan|pojas|sjedal|kacig|kombinezon|odijel|rukavic|cizm|obuc)' and x !~ '(penjac|penjack|alpin)' then 'Osobna oprema'
  when x ~ '(uzad|(^| )uze($| )|rope|strik|statick|staticno|dinamick|transportna vreca|transportne vrece|prusik|gurt|traka)' then 'Užad i užetna oprema'
  when x ~ '(sator|podloga|vreca za spavanje|kuhal|plin|kanister|posud|tanjur|lonac|cerada|agregat|stol|stolica|logor|kamp)' then 'Oprema za logor'
  when x ~ '(prosir|klin|cekic|macol|dlijet|stem)' then 'Oprema za proširivanje'
  when x ~ '(ronil|ronjenje|neopren|maska|peraj|boca)' then 'Ronilačka oprema'
  when x ~ '(alpinist|alpin|penjack|penjac)' then 'Alpinistička oprema'
  when x ~ '(cisto podzemlje|ciscenje|otpad)' then 'Čisto podzemlje'
  when x ~ '(alat|kljuc|odvijac|klijest|lopat|skare|pila|metar)' then 'Alat i radionica'
  when nullif(raw,'') is not null then raw
  else 'Ostalo / provjeriti'
end
from flags
$fn$;

create or replace function public.sov_armory_subcategory_v615(raw_subcategory text, search_basis text)
returns text
language sql
immutable
as $fn$
with t as (select public.sov_armory_norm(coalesce(raw_subcategory,'') || ' ' || coalesce(search_basis,'')) x, trim(coalesce(raw_subcategory,'')) raw)
select case
  when raw = 'Karabineri i spojnice' then 'Karabineri'
  when raw = 'Centralni karabineri / spojnice' then 'Karabineri'
  when raw = 'Sidrišta i fiksevi' then 'Spitovi i sidrišta'
  when raw = 'Pločice i ringovi' then 'Pločice / ringovi'
  when x ~ '(kompas|busol|suunto)' then 'Busole / kompasi / Suunto'
  when x ~ '(disto|distox|topodroid|laser|klinomet|mjeren)' then 'Mjerenje / Disto / TopoDroid'
  when x ~ '(crtan|nacrt|skic|olov|papir)' then 'Crtaći pribor'
  when x ~ '(dron|dji|phantom|mavic|spark|gl300|elisa|propeler|ph4c|ade019|4480mah|5350mah)' and x ~ '(bater|aku|4480mah|5350mah)' then 'Dron baterije'
  when x ~ '(dron|dji|phantom|mavic|spark|gl300|elisa|propeler|ph4c|ade019)' and x ~ '(punjac|charging|ph4c|ade019)' then 'Dron punjači'
  when x ~ '(elisa|propeler)' then 'Elise / propeleri'
  when x ~ '(kontrol|gl300|remote)' then 'Kontroleri'
  when x ~ '(kofer|torba|drzac|transport)' and x ~ '(dron|dji|phantom|mavic|spark)' then 'Transport i zaštita drona'
  when x ~ '(dron|dji|phantom|mavic|spark)' then 'Dronovi i pribor'
  when x ~ '(^| )(busilica|busilice|busilic[ae]|boschhammer)( |$)' then 'Bušilice'
  when x ~ '(bosch|hilti|makita|gbh|sds|busil|busilic|buzil)' and x ~ 'punjac' then 'Punjači za bušilice'
  when x ~ '(svrd|borer|sds|spica)' then 'Svrdla i špicevi'
  when x ~ '(bosch|hilti|makita|gbh|sds)' and x ~ '(bater|aku)' then 'Baterije za bušilice'
  when x ~ '(bosch|hilti|makita|gbh|sds|busil|busilic|buzil)' then 'Bušilice'
  when x ~ '(^| )(aa|aaa|9v|18650|cr123|cr2032)( |$)|baterij[ae]?[[:space:]]+(aa|aaa|9v)|mini usb|usb kabel|solarni punjac|powerbank|punjac.*(aa|aaa)' then 'Baterije, punjači i powerbankovi'
  when x ~ '(walkie|radio stanica|stanica za punjenje)' then 'Komunikacija'
  when x ~ '(lampa|rasvjet|svjetl|ceona)' then 'Lampe i rasvjeta'
  when x ~ '(kamera|foto|video|gopro)' then 'Foto / video'
  when x ~ '(descender|spustal|(^| )stop($| )|rig|maestro|(^| )id($| ))' then 'Descenderi'
  when x ~ '(croll|krol|crol|prsni)' then 'Croll / prsni blokeri'
  when x ~ '(zumar|jumar|ascender|rucni|bloker)' then 'Ručni blokeri'
  when x ~ '(pupak|pupcan)' then 'Pupčana užad'
  when x ~ '(pedal|stremen|pantin)' then 'Pedale / stremeni'
  when x ~ '(pojas|sjedal)' and x !~ '(penjac|penjack|alpin)' then 'Pojasevi i sjedalice'
  when x ~ '(kacig|helmet)' then 'Kacige'
  when x ~ '(kombinezon|odijel|rukavic|cizm|obuc)' then 'Odjeća i obuća'
  when x ~ '(karab|hms|matica|maillon|omni|triact|trilock|screw|twist|oval)' then 'Karabineri'
  when x ~ '(spit|sidr|sidrist|anker|bolt|fikser|spiter)' then 'Spitovi i sidrišta'
  when x ~ '(ploc|ring)' then 'Pločice / ringovi'
  when x ~ '(transportna vreca|transportne vrece)' then 'Transportne vreće'
  when x ~ 'prusik' then 'Prusici'
  when x ~ '(gurt|traka|sling)' then 'Gurtne i trake'
  when x ~ '(uzad|(^| )uze($| )|rope|strik)' then 'Užad'
  when x ~ '(sator|podloga|vreca za spavanje)' then 'Spavanje i šatori'
  when x ~ '(kuhal|plin|posud|tanjur|lonac|kuhin)' then 'Logorska kuhinja'
  when x ~ '(kanister|voda|bidon)' then 'Voda i kanisteri'
  when x ~ '(cekic|macol|dlijet|stem|prosir)' then 'Alat za proširivanje'
  when x ~ '(prva pomoc|sanitet|medic)' then 'Prva pomoć'
  when nullif(raw,'') is not null then raw
  else 'Ostalo'
end
from t
$fn$;

create or replace function private.sov_oruzarstvo_apply_taxonomy_to_table(p_table regclass)
returns jsonb
language plpgsql
security definer
set search_path = public, private
as $fn$
declare
  rel_schema text;
  rel_name text;
  full_name text := p_table::text;
  has_category boolean;
  has_category_name boolean;
  has_main_category boolean;
  has_subcategory boolean;
  has_subcategory_name boolean;
  has_taxonomy_version boolean;
  has_updated_at boolean;
  category_expr text;
  sub_expr text;
  search_expr text;
  cols text;
  updated integer := 0;
begin
  select n.nspname, c.relname into rel_schema, rel_name
  from pg_class c join pg_namespace n on n.oid = c.relnamespace
  where c.oid = p_table;

  select exists(select 1 from information_schema.columns where table_schema=rel_schema and table_name=rel_name and column_name='category') into has_category;
  select exists(select 1 from information_schema.columns where table_schema=rel_schema and table_name=rel_name and column_name='category_name') into has_category_name;
  select exists(select 1 from information_schema.columns where table_schema=rel_schema and table_name=rel_name and column_name='main_category') into has_main_category;
  select exists(select 1 from information_schema.columns where table_schema=rel_schema and table_name=rel_name and column_name='subcategory') into has_subcategory;
  select exists(select 1 from information_schema.columns where table_schema=rel_schema and table_name=rel_name and column_name='subcategory_name') into has_subcategory_name;
  select exists(select 1 from information_schema.columns where table_schema=rel_schema and table_name=rel_name and column_name='taxonomy_version') into has_taxonomy_version;
  select exists(select 1 from information_schema.columns where table_schema=rel_schema and table_name=rel_name and column_name='updated_at') into has_updated_at;

  if not (has_category or has_category_name or has_main_category) then
    return jsonb_build_object('table', full_name, 'updated', 0, 'skipped', 'no category columns');
  end if;

  select string_agg(format('%I', column_name), ', ' order by ordinal_position) into cols
  from information_schema.columns
  where table_schema=rel_schema and table_name=rel_name
    and column_name in ('name','item_name','model','manufacturer','subcategory','subcategory_name','category','category_name','main_category','internal_note','note','description','sku');
  if cols is null or cols = '' then cols := quote_literal(''); end if;
  search_expr := 'concat_ws('' '', ' || cols || ')';

  category_expr := 'public.sov_armory_main_category_v615(coalesce(' ||
    concat_ws(', ',
      case when has_main_category then 'main_category' end,
      case when has_category_name then 'category_name' end,
      case when has_category then 'category' end,
      ''''''
    ) || '), ' || search_expr || ')';

  sub_expr := 'public.sov_armory_subcategory_v615(coalesce(' ||
    concat_ws(', ',
      case when has_subcategory_name then 'subcategory_name' end,
      case when has_subcategory then 'subcategory' end,
      ''''''
    ) || '), ' || search_expr || ')';

  execute 'update ' || p_table::text || ' set ' ||
    concat_ws(', ',
      case when has_main_category then 'main_category = ' || category_expr end,
      case when has_category_name then 'category_name = ' || category_expr end,
      case when has_category then 'category = ' || category_expr end,
      case when has_subcategory_name then 'subcategory_name = ' || sub_expr end,
      case when has_subcategory then 'subcategory = ' || sub_expr end,
      case when has_taxonomy_version then 'taxonomy_version = ''6.1.5''' end,
      case when has_updated_at then 'updated_at = now()' end
    );
  get diagnostics updated = row_count;
  return jsonb_build_object('table', full_name, 'updated', updated);
exception when others then
  return jsonb_build_object('table', full_name, 'updated', updated, 'error', sqlerrm);
end
$fn$;

create or replace function public.sov_oruzarstvo_apply_taxonomy_v615()
returns jsonb
language plpgsql
security definer
set search_path = public, private
as $fn$
declare
  result jsonb := '[]'::jsonb;
  tbl regclass;
begin
  foreach tbl in array array[
    to_regclass('public.equipment_items'),
    to_regclass('public.equipment_assets'),
    to_regclass('public.equipment_pieces'),
    to_regclass('public.equipment_ropes'),
    to_regclass('public.armory_items'),
    to_regclass('public.sov_equipment_items')
  ] loop
    if tbl is not null then
      result := result || jsonb_build_array(private.sov_oruzarstvo_apply_taxonomy_to_table(tbl));
    end if;
  end loop;
  return jsonb_build_object('version','6.1.5','results',result);
end
$fn$;

create or replace function public.sov_oruzarstvo_taxonomy_health()
returns jsonb
language plpgsql
security definer
set search_path = public, private
as $fn$
declare
  taxonomy_count int := 0;
  weird jsonb := '[]'::jsonb;
begin
  select count(*) into taxonomy_count from public.sov_armory_taxonomy where active;
  if to_regclass('public.sov_equipment_app_catalog') is not null then
    execute 'select coalesce(jsonb_agg(jsonb_build_object(''category'', main_category, ''count'', c)), ''[]''::jsonb) from (select main_category, count(*) c from public.sov_equipment_app_catalog where main_category not in (select name from public.sov_armory_taxonomy where active) group by 1) x' into weird;
  end if;
  return jsonb_build_object('ok', taxonomy_count >= 15, 'version','6.1.5', 'taxonomy_count', taxonomy_count, 'unknown_categories', weird);
end
$fn$;

select public.sov_oruzarstvo_apply_taxonomy_v615();
