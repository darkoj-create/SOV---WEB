-- SOV Oružarstvo v4.76 — deduplikacija količinskih artikala
-- Pravilo: samo užad ostaje individualno po kodu/SKU.
-- Ostala oprema se spaja po: kategorija + podkategorija + normalizirani naziv artikla.
-- Primjeri: krol/crol/croll => Croll; stremen/pedala => Stremen.
-- Ne spaja jasne podtipove: Croll S, Croll L, Karabiner OK, Karabiner HMS, Bušilica Makita/Bosch itd.

begin;

create or replace function public.sov_armory_norm_text(v text)
returns text
language sql
immutable
as $$
  select trim(regexp_replace(
    translate(lower(coalesce(v,'')),
      'čćžšđČĆŽŠĐ',
      'cczsdcczsd'
    ),
    '\s+', ' ', 'g'
  ));
$$;

create or replace function public.sov_armory_norm_article_name(v text)
returns text
language plpgsql
immutable
as $$
declare
  x text;
begin
  x := public.sov_armory_norm_text(v);
  x := regexp_replace(x, '[()\[\]{}+_/\\,;:]+', ' ', 'g');
  x := regexp_replace(x, '\m(krol|crol|croll)\M', 'croll', 'g');
  x := regexp_replace(x, '\mpupak\M|\mpupcano\s+u?ze\M', 'pupcano uze', 'g');
  x := trim(regexp_replace(x, '\s+', ' ', 'g'));

  if x ~ '\mcroll\M' then
    if x ~ '\m(s|small)\M' or x like '%velicina s%' then return 'croll s'; end if;
    if x ~ '\m(l|large)\M' or x like '%velicina l%' then return 'croll l'; end if;
    return 'croll';
  end if;

  if x ~ '\mstremen\M' or x ~ '\mpedala\M' then return 'stremen'; end if;
  if x ~ '\mpupcano uze\M' then return 'pupcano uze'; end if;
  if x ~ '\mprusik\M' then return 'prusik'; end if;
  if x ~ '\m(bloker|ascender|jumar|zumar)\M' then return 'bloker'; end if;
  if x ~ '\mstop\M' then return 'stop descender'; end if;

  return x;
end;
$$;

create or replace function public.sov_armory_article_key(cat text, subcat text, item_name text)
returns text
language sql
immutable
as $$
  select public.sov_armory_norm_text(coalesce(cat,'Ostalo')) || '|' ||
         public.sov_armory_norm_text(coalesce(nullif(subcat,''),'Ostalo')) || '|' ||
         public.sov_armory_norm_article_name(item_name);
$$;

-- Privremena mapa svih duplikata prema keeper retku.
drop table if exists pg_temp.sov_armory_dedup_map;
create temporary table sov_armory_dedup_map on commit drop as
with normalized as (
  select
    id,
    legacy_id,
    public.sov_armory_article_key(category_name, subcategory, name) as article_key,
    row_number() over (
      partition by public.sov_armory_article_key(category_name, subcategory, name)
      order by created_at nulls first, id
    ) as rn,
    first_value(id) over (
      partition by public.sov_armory_article_key(category_name, subcategory, name)
      order by created_at nulls first, id
    ) as keep_id,
    first_value(legacy_id) over (
      partition by public.sov_armory_article_key(category_name, subcategory, name)
      order by created_at nulls first, id
    ) as keep_legacy_id
  from public.equipment_items
  where coalesce(item_kind,'quantity_article') = 'quantity_article'
), grouped as (
  select article_key, count(*) as cnt
  from normalized
  group by article_key
  having count(*) > 1
)
select n.*
from normalized n
join grouped g using(article_key);

-- Prebaci reference sa duplikata na keeper.
update public.equipment_request_items eri
set equipment_item_id = m.keep_id,
    equipment_legacy_id = coalesce(m.keep_legacy_id, eri.equipment_legacy_id)
from sov_armory_dedup_map m
where m.rn > 1
  and (eri.equipment_item_id = m.id or eri.equipment_legacy_id = m.legacy_id);

update public.equipment_loan_items eli
set equipment_item_id = m.keep_id,
    equipment_legacy_id = coalesce(m.keep_legacy_id, eli.equipment_legacy_id)
from sov_armory_dedup_map m
where m.rn > 1
  and (eli.equipment_item_id = m.id or eli.equipment_legacy_id = m.legacy_id);

update public.equipment_pieces ep
set equipment_item_id = m.keep_id,
    catalog_legacy_id = coalesce(m.keep_legacy_id, ep.catalog_legacy_id)
from sov_armory_dedup_map m
where m.rn > 1
  and (ep.equipment_item_id = m.id or ep.catalog_legacy_id = m.legacy_id);

update public.procurement_plan pp
set equipment_legacy_id = coalesce(m.keep_legacy_id, pp.equipment_legacy_id)
from sov_armory_dedup_map m
where m.rn > 1 and pp.equipment_legacy_id = m.legacy_id;

update public.equipment_disposals ed
set equipment_legacy_id = coalesce(m.keep_legacy_id, ed.equipment_legacy_id)
from sov_armory_dedup_map m
where m.rn > 1 and ed.equipment_legacy_id = m.legacy_id;

update public.equipment_field_items ef
set equipment_legacy_id = coalesce(m.keep_legacy_id, ef.equipment_legacy_id)
from sov_armory_dedup_map m
where m.rn > 1 and ef.equipment_legacy_id = m.legacy_id;

-- Zbroji količine na keeper red.
with sums as (
  select
    m.keep_id,
    sum(coalesce(e.quantity,0)) as quantity_sum,
    sum(coalesce(e.available,0)) as available_sum,
    sum(coalesce(e.loaned,0)) as loaned_sum,
    min(nullif(e.minimum,0)) filter (where e.minimum is not null and e.minimum > 0) as minimum_min,
    string_agg(distinct nullif(e.source_sheet,''), ' + ') as source_sheet_all,
    string_agg(distinct nullif(e.internal_note,''), ' | ') as notes_all
  from sov_armory_dedup_map m
  join public.equipment_items e on e.id = m.id
  group by m.keep_id
)
update public.equipment_items k
set quantity = greatest(0, round(s.quantity_sum)),
    available = greatest(0, round(s.available_sum)),
    loaned = greatest(0, round(s.loaned_sum)),
    minimum = coalesce(s.minimum_min, k.minimum),
    source_sheet = coalesce(nullif(s.source_sheet_all,''), k.source_sheet),
    internal_note = coalesce(nullif(s.notes_all,''), k.internal_note),
    tracking_type = 'po vrsti',
    item_kind = 'quantity_article',
    code_required = false,
    physical_code_note = coalesce(k.physical_code_note, 'Nema pojedinačnih kodova; vodi se količina po artiklu.'),
    updated_at = now()
from sums s
where k.id = s.keep_id;

-- Obriši samo duple količinske retke. Užad nije u ovoj tablici i nije dirana.
delete from public.equipment_items e
using sov_armory_dedup_map m
where m.rn > 1 and e.id = m.id;

-- Spriječi nove duplikate kroz import/manual add.
create unique index if not exists equipment_items_article_key_unique
on public.equipment_items (
  public.sov_armory_article_key(category_name, subcategory, name)
)
where coalesce(item_kind,'quantity_article') = 'quantity_article';

commit;
