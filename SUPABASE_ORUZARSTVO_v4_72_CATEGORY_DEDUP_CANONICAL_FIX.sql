-- SOV Oružarstvo v4.72 — category de-dup/canonical cleanup
-- Pokrenuti jednom u Supabase SQL editoru.
-- Cilj: spojiti semantičke duplikate kategorija:
-- Medicina -> Medicinska oprema
-- Elektro i foto -> Elektro i foto oprema
-- Alpinistička i ronilačka -> Alpinistička oprema / Ronilačka oprema po nazivu artikla
-- Ekspedicijska i kamp oprema -> Oprema za logor
-- Užad -> Užad i užetna oprema

create or replace function public.sov_armory_canonical_category_name(raw_name text, item_text text default '')
returns text
language plpgsql
immutable
as $$
declare
  r text := lower(unaccent(coalesce(raw_name,'')));
  t text := lower(unaccent(coalesce(raw_name,'') || ' ' || coalesce(item_text,'')));
begin
  if t ~ 'descender|(^|[^a-z])stop([^a-z]|$)|rig|maestro|id''?s|croll|krol|crol|bloker|zumar|pojas|sjedal|pedal|stremen|prsni|pupak|pupcano' then
    return 'Osobna oprema';
  elsif t ~ 'uzad|uzetna|(^|[^a-z])uze([^a-z]|$)|rope|prusik|gurt|traka|kolotur|transportna vreca' and t !~ 'busil|bater|punjac|svrd' then
    return 'Užad i užetna oprema';
  elsif t ~ 'busil|baterija bosch|bosch.*bater|punjac|svrd|gbh18|gbh180|boschhammer' then
    return 'Bušilice i baterije';
  elsif t ~ 'postavlj|spit|sidrist|ploc|ring|anker|bolt|karabiner|matica|hms' and t !~ 'descender|croll|bloker|pojas' then
    return 'Oprema za postavljanje';
  elsif t ~ 'crtan|mjeren|disto|kompas|topodroid|dokumentac|nacrt|skic' then
    return 'Oprema za crtanje';
  elsif t ~ 'elektro|foto|kamera|video|rasvjet|svjetl|lampa|ceona|ceo' then
    return 'Elektro i foto oprema';
  elsif t ~ 'medicin|medicina|prva pomoc|prva pom' then
    return 'Medicinska oprema';
  elsif t ~ 'ronil|ronjenje|neopren|maska|peraj|boca' then
    return 'Ronilačka oprema';
  elsif t ~ 'alpinist|alpin|penjack|penjac' then
    return 'Alpinistička oprema';
  elsif t ~ 'cisto podzemlje|ciscenje|cistoc|otpad' then
    return 'Čisto podzemlje';
  elsif t ~ 'prosir|prosirivanje|klin|cekic|macol|dlijet|stem' then
    return 'Oprema za proširivanje';
  elsif t ~ 'logor|kamp|ekspedic|sator|kuhal|plin|podlog|vreca za spavanje' then
    return 'Oprema za logor';
  elsif t ~ 'alat|kljuc|odvijac|klijest|toolbox' then
    return 'Ostali alat';
  elsif r ~ 'ostalo|razno' or trim(coalesce(raw_name,'')) = '' then
    return 'Ostalo';
  else
    return trim(raw_name);
  end if;
end;
$$;

-- unaccent is usually available on Supabase; create it if missing.
create extension if not exists unaccent;

-- 1) Normalize item category names.
update public.equipment_items
set category_name = public.sov_armory_canonical_category_name(
  category_name,
  concat_ws(' ', name, subcategory, internal_note)
),
updated_at = now()
where category_name is not null;

-- 2) Ensure canonical category rows exist.
insert into public.equipment_categories (name, description, type, sort_order)
select distinct category_name, 'Kanonizirana kategorija opreme', 'main', 100
from public.equipment_items
where category_name is not null and trim(category_name) <> ''
on conflict (name) do update
set updated_at = now();

-- 3) Re-link equipment_items.category_id to canonical category.
update public.equipment_items i
set category_id = c.id,
updated_at = now()
from public.equipment_categories c
where c.name = i.category_name;

-- 4) Remove category aliases that are only old duplicate labels.
delete from public.equipment_categories c
where c.name <> public.sov_armory_canonical_category_name(c.name, '')
  and not exists (
    select 1 from public.equipment_items i where i.category_id = c.id
  );

-- 5) Final safety: if any old alias survived because of FK, null it and delete alias.
update public.equipment_items i
set category_id = c2.id,
updated_at = now()
from public.equipment_categories oldc
join public.equipment_categories c2
  on c2.name = public.sov_armory_canonical_category_name(oldc.name, '')
where i.category_id = oldc.id
  and oldc.name <> c2.name;

delete from public.equipment_categories c
where c.name <> public.sov_armory_canonical_category_name(c.name, '');

-- 6) Show final category counts.
select category_name, count(*) as artikala
from public.equipment_items
group by category_name
order by category_name;
