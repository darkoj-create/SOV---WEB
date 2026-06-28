-- SOV Oružarstvo v5.59.9 — canonical category cleanup
-- Purpose: fix scattered categories like ordinary batteries / busole / kompasi / random tools.
-- Run after deploying sov-web-build-v5.59.9-oruzarstvo-category-cleanup.

create extension if not exists pgcrypto;

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
    public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(raw_subcategory,'') || ' ' || coalesce(search_basis,'')) ~ '(croll|krol|crol|prsni|descender|spustal|(^| )stop($| )|rig|maestro|id[''’]?s|zumar|jumar|ascender|rucni bloker|rucni|(^| )bloker|pedal|stremen|pupak|pupcano)'
    or (public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(raw_subcategory,'') || ' ' || coalesce(search_basis,'')) ~ '(pojas|sjedal)'
        and public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(raw_subcategory,'') || ' ' || coalesce(search_basis,'')) !~ '(penjack|penjac|alpinist|climbing)')
  )
$$;

create or replace function public.sov_armory_main_category(raw_category text, search_basis text)
returns text
language sql
immutable
as $$
with t as (
  select
    trim(coalesce(raw_category,'')) as raw,
    public.sov_armory_norm(coalesce(raw_category,'') || ' ' || coalesce(search_basis,'')) as x
), flags as (
  select *,
    x ~ '(^| )(aa|aaa|9v|18650|cr123|cr2032)( |$)|baterij[ae]?[[:space:]]+(aa|aaa|9v)|punjac.*(aa|aaa)|mini usb|usb kabel|solarni punjac|powerbank' as ordinary_battery,
    x ~ '(bosch|hilti|makita|gbh|sds|busil|busilic|buzil|svrd|borer|akumulatorsk)' as drill_context,
    x ~ '(dron|dji|phantom|mavic|spark|gl300|elisa|propeler|ph4c|ade019|4480mah|5350mah|kofer za dron|punjac za dron)' as drone_context
  from t
)
select case
  -- Canonical names pass through.
  when raw in ('Osobna oprema','Užad i užetna oprema','Oprema za postavljanje','Oprema za crtanje','Bušilice i svrdla','Elektro, rasvjeta i foto','Dronovi','Oprema za logor','Oprema za proširivanje','Medicinska oprema','Alat i radionica','Alpinistička oprema','Ronilačka oprema','Čisto podzemlje','Ostalo / provjeriti') then raw
  when raw = 'Užad' then 'Užad i užetna oprema'
  -- Old broad bucket split into the right buckets.
  when public.sov_armory_norm(raw) like '%busilice i baterije%' and drone_context then 'Dronovi'
  when public.sov_armory_norm(raw) like '%busilice i baterije%' and ordinary_battery and not drill_context then 'Elektro, rasvjeta i foto'
  when public.sov_armory_norm(raw) like '%busilice i baterije%' then 'Bušilice i svrdla'
  -- Old labels.
  when public.sov_armory_norm(raw) in ('elektro i foto','elektro i foto oprema','rasvjeta') then 'Elektro, rasvjeta i foto'
  when public.sov_armory_norm(raw) in ('ostali alat','alat','ostalo') then 'Alat i radionica'
  when public.sov_armory_norm(raw) ~ 'medicin' then 'Medicinska oprema'
  when public.sov_armory_norm(raw) ~ '(logor|kamp)' then 'Oprema za logor'
  when public.sov_armory_norm(raw) ~ 'prosir' then 'Oprema za proširivanje'
  when public.sov_armory_norm(raw) ~ 'alpin' then 'Alpinistička oprema'
  when public.sov_armory_norm(raw) ~ 'ronil' then 'Ronilačka oprema'
  when public.sov_armory_norm(raw) ~ 'cisto podzemlje' then 'Čisto podzemlje'
  -- Item text rules.
  when drone_context then 'Dronovi'
  when x ~ '(kompas|busol|suunto|disto|distox|topodroid|klinomet|laser|nacrt|skic|olov)' then 'Oprema za crtanje'
  when ordinary_battery and not drill_context then 'Elektro, rasvjeta i foto'
  when x ~ '(lampa|rasvjet|svjetl|ceona|kamera|foto|video|walkie|radio stanica|punjenje walkie)' then 'Elektro, rasvjeta i foto'
  when drill_context and x ~ '(bater|aku|punjac|busil|svrd|borer|sds)' then 'Bušilice i svrdla'
  when x ~ '(spit|sidr|sidrist|anker|bolt|ploc|ring|fikser|karab|hms|matica|maillon|omni|triact|trilock)' and not public.sov_armory_is_personal_kit(raw, '', search_basis) then 'Oprema za postavljanje'
  when public.sov_armory_is_personal_kit(raw, '', search_basis) then 'Osobna oprema'
  when x ~ '(osob|kacig|helmet|kombinezon|odijel|rukavic|cizm|obuc)' then 'Osobna oprema'
  when x ~ '(uzad|uzetna|(^| )uze($| )|rope|strik|statick|staticno|dinamick|transportna vreca|transportne vrece|prusik|gurt|traka)' then 'Užad i užetna oprema'
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
$$;

create or replace function public.sov_armory_subcategory(raw_subcategory text, search_basis text)
returns text
language sql
immutable
as $$
with t as (select public.sov_armory_norm(coalesce(raw_subcategory,'') || ' ' || coalesce(search_basis,'')) as x, trim(coalesce(raw_subcategory,'')) as raw)
select case
  -- Crtanje / mjerenje.
  when x ~ '(kompas|busol|suunto)' then 'Busole / kompasi / Suunto'
  when x ~ '(disto|distox|topodroid|laser|klinomet|mjeren)' then 'Mjerenje / Disto / TopoDroid'
  when x ~ '(crtan|nacrt|skic|olov|papir)' then 'Crtaći pribor'
  -- Dronovi.
  when x ~ '(dron|dji|phantom|mavic|spark|gl300|elisa|propeler|ph4c|ade019|4480mah|5350mah)' and x ~ '(bater|aku|4480mah|5350mah)' then 'Dron baterije'
  when x ~ '(dron|dji|phantom|mavic|spark|gl300|elisa|propeler|ph4c|ade019)' and x ~ '(punjac|charging|ph4c|ade019)' then 'Dron punjači'
  when x ~ '(elisa|propeler)' then 'Elise / propeleri'
  when x ~ '(kontrol|gl300|remote)' then 'Kontroleri'
  when x ~ '(kofer|torba|drzac|transport)' and x ~ '(dron|dji|phantom|mavic|spark)' then 'Transport i zaštita drona'
  when x ~ '(dron|dji|phantom|mavic|spark)' then 'Dronovi i pribor'
  -- Bušilice vs obične baterije.
  when x ~ '(^| )(busilica|busilice|busilic[ae]|boschhammer)( |$)' then 'Bušilice'
  when x ~ '(bosch|hilti|makita|gbh|sds|busil|busilic|buzil)' and x ~ '(punjac)' then 'Punjači za bušilice'
  when x ~ '(svrd|borer|sds|spica)' then 'Svrdla i špicevi'
  when x ~ '(bosch|hilti|makita|gbh|sds)' and x ~ '(bater|aku)' then 'Baterije za bušilice'
  when x ~ '(bosch|hilti|makita|gbh|sds|busil|busilic|buzil)' then 'Bušilice'
  when x ~ '(^| )(aa|aaa|9v|18650|cr123|cr2032)( |$)|baterij[ae]?[[:space:]]+(aa|aaa|9v)|mini usb|usb kabel|solarni punjac|powerbank|punjac.*(aa|aaa)' then 'Baterije, punjači i powerbankovi'
  when x ~ '(walkie|radio stanica|stanica za punjenje)' then 'Komunikacija'
  when x ~ '(lampa|rasvjet|svjetl|ceona)' then 'Lampe i rasvjeta'
  when x ~ '(kamera|foto|video|gopro)' then 'Foto / video'
  -- Osobna / SRT.
  when x ~ '(descender|spustal|(^| )stop($| )|rig|maestro|id[''’]?s)' then 'Descenderi'
  when x ~ '(croll|krol|crol|prsni)' then 'Croll / prsni blokeri'
  when x ~ '(zumar|jumar|ascender|rucni bloker|rucni|(^| )bloker)' then 'Ručni blokeri'
  when x ~ '(pupak|pupcan|pupcano)' then 'Pupčana užad'
  when x ~ '(pedal|stremen)' then 'Pedale / stremeni'
  when x ~ '(pojas|sjedal)' and x !~ '(penjack|penjac|alpinist|climbing)' then 'Pojasevi i sjedalice'
  when x ~ '(kacig|helmet)' then 'Kacige'
  when x ~ '(kombinezon|odijel|rukavic|cizm|obuc)' then 'Odjeća i obuća'
  -- Postavljanje / užad.
  when x ~ '(karab|hms|matica|maillon|omni|triact|trilock|screw|twist|oval)' then 'Karabineri'
  when x ~ '(spit|sidr|sidrist|anker|bolt|fikser)' then 'Spitovi i sidrišta'
  when x ~ '(ploc|ring)' then 'Pločice / ringovi'
  when x ~ '(transportna vreca|transportne vrece)' then 'Transportne vreće'
  when x ~ '(prusik)' then 'Prusici'
  when x ~ '(gurt|traka|sling)' then 'Gurtne i trake'
  when x ~ '(uzad|uzetna|(^| )uze($| )|rope|strik|statick|staticno|dinamick)' then 'Užad'
  -- Logor / proširivanje / medicina.
  when x ~ '(sator|podloga|vreca za spavanje)' then 'Spavanje i šatori'
  when x ~ '(kuhal|plin|posud|tanjur|lonac|kuhin)' then 'Logorska kuhinja'
  when x ~ '(kanister|voda|bidon)' then 'Voda i kanisteri'
  when x ~ '(cekic|macol|dlijet|stem|prosir)' then 'Alat za proširivanje'
  when x ~ '(prva pomoc|sanitet|medic)' then 'Prva pomoć'
  when nullif(raw,'') is not null then raw
  else 'Ostalo'
end
from t
$$;

create or replace function public.sov_armory_category_priority(category text)
returns int
language sql
immutable
as $$
  select case public.sov_armory_main_category(category, category)
    when 'Osobna oprema' then 10
    when 'Užad i užetna oprema' then 20
    when 'Oprema za postavljanje' then 30
    when 'Oprema za crtanje' then 40
    when 'Bušilice i svrdla' then 50
    when 'Elektro, rasvjeta i foto' then 60
    when 'Dronovi' then 70
    when 'Oprema za logor' then 80
    when 'Oprema za proširivanje' then 90
    when 'Medicinska oprema' then 100
    when 'Alat i radionica' then 110
    when 'Alpinistička oprema' then 120
    when 'Ronilačka oprema' then 130
    when 'Čisto podzemlje' then 140
    else 999
  end
$$;

create or replace function public.sov_armory_search_tags(main_category text, subcategory text, search_basis text)
returns text
language sql
immutable
as $$
  select concat_ws(' ',
    case public.sov_armory_main_category(main_category, search_basis)
      when 'Osobna oprema' then 'osobna oprema srt vertikala pojas croll stop bloker pupak kaciga'
      when 'Užad i užetna oprema' then 'uze uzad rope strik prusik gurtna traka transportna vreca'
      when 'Oprema za postavljanje' then 'postavljanje rigging sidriste sidrište spit plocica pločica ring karabiner'
      when 'Oprema za crtanje' then 'crtanje dokumentiranje mjerenje kompas busola suunto disto topodroid'
      when 'Bušilice i svrdla' then 'busilice bušilice svrdla sds bosch hilti makita baterija za busilicu'
      when 'Elektro, rasvjeta i foto' then 'elektro rasvjeta foto kamera lampa baterije aa aaa 9v usb powerbank punjac'
      when 'Dronovi' then 'dron dji phantom mavic spark baterija punjac elisa kontroler kofer'
      when 'Oprema za logor' then 'logor kamp bivak sator kuhinja voda spavanje'
      when 'Oprema za proširivanje' then 'prosirivanje proširivanje stemanje štemanje cekic macola dlijeto'
      when 'Medicinska oprema' then 'medicina medicinska prva pomoc prva pomoć sanitet'
      when 'Alat i radionica' then 'alat radionica servis kljuc odvijac klijesta lopata'
      when 'Alpinistička oprema' then 'alpinisticka alpinistička penjacka penjačka'
      when 'Ronilačka oprema' then 'ronjenje ronilacka ronilačka neopren maska peraje boca'
      when 'Čisto podzemlje' then 'cisto čisto podzemlje ciscenje čišćenje otpad'
      else null
    end,
    case public.sov_armory_subcategory(subcategory, search_basis)
      when 'Busole / kompasi / Suunto' then 'kompas busola busole suunto compass'
      when 'Baterije, punjači i powerbankovi' then 'baterija baterije aa aaa 9v usb mini usb powerbank solarni punjac'
      when 'Baterije za bušilice' then 'bosch hilti makita baterija aku akumulator za busilicu'
      when 'Punjači za bušilice' then 'bosch hilti makita punjac charger za busilicu'
      when 'Svrdla i špicevi' then 'svrdlo svrdla sds boreri spica špica'
      when 'Dron baterije' then 'dron baterija dji phantom mavic spark'
      when 'Dron punjači' then 'dron punjac dji phantom charger'
      when 'Elise / propeleri' then 'elisa elise propeler propeleri'
      when 'Kontroleri' then 'kontroler remote gl300'
      when 'Karabineri' then 'karabiner karabin karab hms matica spojka maillon omni triact trilock screw twist oval'
      when 'Croll / prsni blokeri' then 'croll krol crol prsni bloker chest ascender'
      when 'Pojasevi i sjedalice' then 'pojas pojasevi sjedalica sjedni pojas harness'
      when 'Descenderi' then 'stop descender spustalica rig id maestro'
      when 'Ručni blokeri' then 'zumar jumar rucni ručni bloker ascender'
      when 'Pupčana užad' then 'pupak pupcano pupčano uze uže'
      when 'Pedale / stremeni' then 'pedala pedal stremen footloop'
      when 'Kacige' then 'kaciga kacige helmet'
      when 'Užad' then 'uze uzad rope strik staticko staticno dinamicko'
      when 'Transportne vreće' then 'transportna vreca transportne vrece transport bag'
      when 'Prusici' then 'prusik prusici pomocno uze pomoćno uže'
      when 'Gurtne i trake' then 'gurtna gurtne traka trake sling'
      when 'Spitovi i sidrišta' then 'spit sidriste sidrište anker bolt fikser'
      when 'Pločice / ringovi' then 'plocica pločica ring sidriste sidrište'
      when 'Mjerenje / Disto / TopoDroid' then 'disto distox topodroid laser klinometar mjerenje'
      when 'Crtaći pribor' then 'crtaci crtaći pribor olovka papir skica nacrt'
      when 'Lampe i rasvjeta' then 'lampa lampe rasvjeta svjetlo ceona čeona'
      when 'Foto / video' then 'kamera foto video gopro'
      when 'Komunikacija' then 'walkie talkie radio stanica komunikacija'
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
    when public.sov_armory_subcategory(subcategory, search_basis) in (
      'Karabineri','Croll / prsni blokeri','Pojasevi i sjedalice','Descenderi','Ručni blokeri','Pupčana užad','Pedale / stremeni',
      'Kacige','Odjeća i obuća','Spitovi i sidrišta','Pločice / ringovi','Transportne vreće','Prusici','Gurtne i trake',
      'Bušilice','Baterije za bušilice','Punjači za bušilice','Svrdla i špicevi','Baterije, punjači i powerbankovi',
      'Busole / kompasi / Suunto','Mjerenje / Disto / TopoDroid','Crtaći pribor','Lampe i rasvjeta','Foto / video','Komunikacija',
      'Dron baterije','Dron punjači','Elise / propeleri','Kontroleri','Transport i zaštita drona'
    ) then public.sov_armory_subcategory(subcategory, search_basis)
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

-- Ensure canonical category rows exist for admin/import UIs.
insert into public.equipment_categories (name, description, type, sort_order)
values
  ('Osobna oprema','Pojasevi, crollovi, descenderi, blokeri, kacige i osobna speleo oprema.','main',10),
  ('Užad i užetna oprema','Užad, prusici, gurtne, transportne vreće i užetni pribor.','main',20),
  ('Oprema za postavljanje','Spitovi, pločice, ringovi, sidrišta, karabineri i rigging pribor.','main',30),
  ('Oprema za crtanje','Busole/kompasi, Suunto, DistoX, TopoDroid i crtaći pribor.','main',40),
  ('Bušilice i svrdla','Bušilice, svrdla, SDS pribor, Bosch/Hilti/Makita baterije i punjači.','main',50),
  ('Elektro, rasvjeta i foto','Lampe, obične baterije AA/AAA/9V, USB, punjači, komunikacija, foto i video.','main',60),
  ('Dronovi','Dronovi, dron baterije, punjači, elise, kontroleri i transportni koferi.','main',70),
  ('Oprema za logor','Logor, kuhinja, voda, spavanje, terenski boravak i higijena.','main',80),
  ('Oprema za proširivanje','Čekići, macole, dlijeta i oprema za proširivanje.','main',90),
  ('Medicinska oprema','Prva pomoć, sanitet i medicinski kompleti.','main',100),
  ('Alat i radionica','Opći alat, radionica, servis i popravci.','main',110),
  ('Alpinistička oprema','Alpinistička i penjačka oprema.','main',120),
  ('Ronilačka oprema','Ronilačka oprema, neopreni, maske, peraje i boce.','main',130),
  ('Čisto podzemlje','Vreće, rukavice i oprema za akcije čišćenja.','main',140),
  ('Ostalo / provjeriti','Stavke koje treba ručno provjeriti.','main',999)
on conflict (name) do update
set description = excluded.description,
    type = excluded.type,
    sort_order = excluded.sort_order,
    updated_at = now();

-- Nudge the fast manifest/cache so web/APK stop using old cached category buckets.
do $$
begin
  if to_regclass('public.equipment_assets') is not null then
    update public.equipment_assets
    set updated_at = now()
    where true;
  end if;

  if to_regprocedure('public.sov_rebuild_equipment_catalog_manifest()') is not null then
    perform public.sov_rebuild_equipment_catalog_manifest();
  elsif to_regprocedure('public.sov_mark_equipment_catalog_manifest_dirty()') is not null then
    perform public.sov_mark_equipment_catalog_manifest_dirty();
  end if;
end $$;

-- Smoke checks after running:
-- select main_category, subcategory, count(*) rows
-- from public.sov_equipment_app_catalog
-- group by 1,2
-- order by min(priority), 1, 2;
--
-- select name, main_category, subcategory
-- from public.sov_equipment_app_catalog
-- where public.sov_armory_norm(name || ' ' || coalesce(raw_subcategory,'') || ' ' || coalesce(raw_category,''))
--       ~ '(baterije aa|baterije aaa|baterije 9v|mini usb|solarni punjac|kompas|suunto|baterija bosch|busilica|dron)'
-- order by main_category, subcategory, name;
