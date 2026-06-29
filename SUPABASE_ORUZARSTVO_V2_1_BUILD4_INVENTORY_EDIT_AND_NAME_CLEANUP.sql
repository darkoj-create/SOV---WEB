-- SOV Oružarstvo v2.1 Build 4
-- Fixes:
-- 1) manual inventory edit now goes through SECURITY DEFINER RPC instead of browser direct upsert
-- 2) legacy/item id handling strips app prefix "item:"
-- 3) obvious OCR/import name cleanup for Stop descender rows
-- Safe to run multiple times. Does not delete data.

create extension if not exists pgcrypto;

-- Make sure the fast catalog manifest exists.
create table if not exists public.sov_equipment_catalog_manifest (
  id boolean primary key default true,
  catalog_version text not null default md5(now()::text),
  raw_row_count integer not null default 0,
  grouped_row_count integer not null default 0,
  equipment_items_count integer not null default 0,
  equipment_ropes_count integer not null default 0,
  equipment_pieces_count integer not null default 0,
  last_changed_at timestamptz not null default now(),
  requests_changed_at timestamptz not null default now(),
  checked_at timestamptz not null default now()
);

alter table public.sov_equipment_catalog_manifest disable row level security;
grant select on public.sov_equipment_catalog_manifest to anon, authenticated;

create or replace function public.sov_rebuild_equipment_catalog_manifest()
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_raw int := 0;
  v_grouped int := 0;
  v_items int := 0;
  v_ropes int := 0;
  v_pieces int := 0;
begin
  if to_regclass('public.sov_equipment_app_catalog') is not null then
    execute 'select count(*)::int from public.sov_equipment_app_catalog' into v_raw;
  end if;
  if to_regclass('public.sov_equipment_app_catalog_grouped') is not null then
    execute 'select count(*)::int from public.sov_equipment_app_catalog_grouped' into v_grouped;
  end if;
  if to_regclass('public.equipment_items') is not null then
    execute 'select count(*)::int from public.equipment_items' into v_items;
  end if;
  if to_regclass('public.equipment_ropes') is not null then
    execute 'select count(*)::int from public.equipment_ropes' into v_ropes;
  end if;
  if to_regclass('public.equipment_pieces') is not null then
    execute 'select count(*)::int from public.equipment_pieces' into v_pieces;
  end if;

  insert into public.sov_equipment_catalog_manifest (
    id, catalog_version, raw_row_count, grouped_row_count,
    equipment_items_count, equipment_ropes_count, equipment_pieces_count,
    last_changed_at, checked_at
  ) values (
    true,
    md5('sov-armory-build4|' || clock_timestamp()::text),
    v_raw, v_grouped, v_items, v_ropes, v_pieces,
    now(), now()
  )
  on conflict (id) do update set
    catalog_version = excluded.catalog_version,
    raw_row_count = excluded.raw_row_count,
    grouped_row_count = excluded.grouped_row_count,
    equipment_items_count = excluded.equipment_items_count,
    equipment_ropes_count = excluded.equipment_ropes_count,
    equipment_pieces_count = excluded.equipment_pieces_count,
    last_changed_at = excluded.last_changed_at,
    checked_at = excluded.checked_at;
end;
$$;

create or replace function public.sov_armory_clean_legacy_id(p_value text)
returns text
language sql
immutable
as $$
  select nullif(trim(regexp_replace(coalesce(p_value,''), '^item:', '', 'i')), '')
$$;

-- RPC used by web "Uredi" / inventory count edit.
-- It updates the actual counters read by sov_equipment_app_catalog(_grouped).
create or replace function public.sov_armory_upsert_simple_item(
  p_legacy_id text,
  p_catalog_id text default null,
  p_name text default null,
  p_category_name text default 'Ostalo',
  p_subcategory text default 'Ostalo',
  p_unit text default 'kom',
  p_quantity numeric default 0,
  p_available numeric default 0,
  p_minimum numeric default 0,
  p_location_name text default 'Oružarstvo Klaićeva',
  p_status text default 'aktivno',
  p_availability text default null,
  p_internal_note text default null,
  p_physical_code_note text default null,
  p_member_visible boolean default true
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_legacy_id text := public.sov_armory_clean_legacy_id(p_legacy_id);
  v_catalog_id text := public.sov_armory_clean_legacy_id(coalesce(p_catalog_id, p_legacy_id));
  v_name text := nullif(trim(coalesce(p_name,'')), '');
  v_category_name text := coalesce(nullif(trim(p_category_name),''), 'Ostalo');
  v_subcategory text := coalesce(nullif(trim(p_subcategory),''), 'Ostalo');
  v_unit text := coalesce(nullif(trim(p_unit),''), 'kom');
  v_qty integer := greatest(0, round(coalesce(p_quantity,0)))::integer;
  v_av integer := greatest(0, round(coalesce(p_available,0)))::integer;
  v_min integer := greatest(0, round(coalesce(p_minimum,0)))::integer;
  v_status text := coalesce(nullif(trim(p_status),''), 'aktivno');
  v_availability text := coalesce(nullif(trim(p_availability),''), case when v_av > 0 then 'dostupno' else 'nedostupno' end);
  v_location_name text := coalesce(nullif(trim(p_location_name),''), 'Oružarstvo Klaićeva');
  v_category_id uuid;
  v_item public.equipment_items%rowtype;
  v_existing public.equipment_items%rowtype;
begin
  if v_legacy_id is null then
    v_legacy_id := 'ART-' || gen_random_uuid()::text;
  end if;
  if v_name is null then
    raise exception 'Naziv opreme je obavezan' using errcode = '22023';
  end if;

  insert into public.equipment_categories (legacy_id, name, description, type, sort_order)
  values ('CAT-' || lower(regexp_replace(v_category_name,'[^[:alnum:]]+','-','g')), v_category_name, 'Auto category from armory edit', 'inventory', 999)
  on conflict (name) do update set name = excluded.name
  returning id into v_category_id;

  select * into v_existing
  from public.equipment_items e
  where e.legacy_id = v_legacy_id
     or (v_catalog_id is not null and e.catalog_id = v_catalog_id)
  order by e.updated_at desc nulls last
  limit 1;

  if found then
    update public.equipment_items
    set legacy_id = coalesce(v_existing.legacy_id, v_legacy_id),
        catalog_id = coalesce(v_catalog_id, v_existing.catalog_id, v_legacy_id),
        name = v_name,
        category_id = v_category_id,
        category_name = v_category_name,
        subcategory = v_subcategory,
        unit = v_unit,
        tracking_type = coalesce(nullif(tracking_type,''), 'bulk'),
        quantity = v_qty,
        available = v_av,
        loaned = greatest(0, v_qty - v_av),
        minimum = v_min,
        status = v_status,
        availability = v_availability,
        member_visible = coalesce(p_member_visible, true),
        internal_note = p_internal_note,
        physical_code_note = p_physical_code_note,
        item_kind = coalesce(nullif(item_kind,''), 'quantity_article'),
        code_required = false,
        location_name = v_location_name,
        quantity_label = v_qty::text,
        available_label = v_av::text,
        original_quantity_text = v_qty::text,
        updated_at = now()
    where id = v_existing.id
    returning * into v_item;
  else
    insert into public.equipment_items (
      legacy_id, catalog_id, name, category_id, category_name, subcategory, unit,
      tracking_type, quantity, loaned, available, minimum, status, availability,
      member_visible, internal_note, source_sheet, item_kind, code_required,
      physical_code_note, quantity_label, available_label, original_quantity_text,
      location_name, updated_at
    ) values (
      v_legacy_id, coalesce(v_catalog_id, v_legacy_id), v_name, v_category_id, v_category_name, v_subcategory, v_unit,
      'bulk', v_qty, greatest(0, v_qty - v_av), v_av, v_min, v_status, v_availability,
      coalesce(p_member_visible, true), p_internal_note, 'manual-web-rpc', 'quantity_article', false,
      p_physical_code_note, v_qty::text, v_av::text, v_qty::text,
      v_location_name, now()
    )
    returning * into v_item;
  end if;

  -- Keep simple location stock aligned with the available count, because older screens read this too.
  if to_regclass('public.equipment_item_locations') is not null then
    update public.equipment_item_locations
    set item_name = v_item.name,
        quantity = v_av,
        updated_at = now(),
        note = concat_ws(E'\n', nullif(note,''), 'manual inventory edit from web')
    where coalesce(equipment_legacy_id,'') = coalesce(v_item.legacy_id,'')
      and lower(location_type) = 'storage'
      and lower(location_name) = lower(v_location_name);

    if not found then
      begin
        insert into public.equipment_item_locations (
          equipment_legacy_id,item_name,location_type,location_name,quantity,note,updated_at
        ) values (
          v_item.legacy_id, v_item.name, 'storage', v_location_name, v_av, 'manual inventory edit from web', now()
        );
      exception when unique_violation then
        update public.equipment_item_locations
        set item_name = v_item.name, quantity = v_av, updated_at = now()
        where coalesce(equipment_legacy_id,'') = coalesce(v_item.legacy_id,'')
          and lower(location_type) = 'storage'
          and lower(location_name) = lower(v_location_name);
      end;
    end if;
  end if;

  perform public.sov_rebuild_equipment_catalog_manifest();

  return jsonb_build_object(
    'ok', true,
    'item_id', v_item.id,
    'legacy_id', v_item.legacy_id,
    'catalog_id', v_item.catalog_id,
    'name', v_item.name,
    'quantity', v_item.quantity,
    'available', v_item.available,
    'quantity_label', v_item.quantity_label,
    'available_label', v_item.available_label,
    'location_name', v_item.location_name
  );
end;
$$;

grant execute on function public.sov_armory_upsert_simple_item(text,text,text,text,text,text,numeric,numeric,numeric,text,text,text,text,text,boolean) to authenticated;

-- Replace legacy_return RPC again with stronger id resolution and explicit catalog counter update.
create or replace function public.sov_armory_record_legacy_return(
  p_item_id uuid default null,
  p_equipment_legacy_id text default null,
  p_item_name text default null,
  p_quantity numeric default 1,
  p_to_location_name text default 'Oružarstvo Klaićeva',
  p_condition_status text default 'ok',
  p_source_name text default null,
  p_note text default null,
  p_client_event_id text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_item public.equipment_items%rowtype;
  v_after public.equipment_items%rowtype;
  v_qty integer := greatest(1, round(coalesce(p_quantity,1)))::integer;
  v_available_delta integer;
  v_event_key text;
  v_existing public.sov_armory_stock_movements%rowtype;
  v_lookup_legacy text := public.sov_armory_clean_legacy_id(p_equipment_legacy_id);
  v_to_location text := coalesce(nullif(trim(p_to_location_name),''),'Oružarstvo Klaićeva');
  v_condition text := lower(coalesce(nullif(trim(p_condition_status),''),'ok'));
begin
  if p_quantity is null or p_quantity <= 0 then
    raise exception 'Quantity must be greater than zero' using errcode = '22023';
  end if;

  if p_client_event_id is not null then
    select * into v_existing
    from public.sov_armory_stock_movements
    where client_event_id = p_client_event_id
    limit 1;
    if found then
      return jsonb_build_object('ok', true, 'duplicate', true, 'movement_id', v_existing.id, 'event_key', v_existing.event_key);
    end if;
  end if;

  select e.* into v_item
  from public.equipment_items e
  where (p_item_id is not null and e.id = p_item_id)
     or (v_lookup_legacy is not null and (e.legacy_id = v_lookup_legacy or e.catalog_id = v_lookup_legacy))
     or (p_item_name is not null and lower(trim(e.name)) = lower(trim(p_item_name)))
  order by
    case when p_item_id is not null and e.id = p_item_id then 0 else 1 end,
    case when v_lookup_legacy is not null and e.legacy_id = v_lookup_legacy then 0 else 1 end,
    case when v_lookup_legacy is not null and e.catalog_id = v_lookup_legacy then 0 else 1 end,
    e.updated_at desc nulls last
  limit 1;

  if not found then
    raise exception 'Equipment item not found for legacy_id %, name %. Add the article first, then record legacy_return.', v_lookup_legacy, p_item_name using errcode = 'P0002';
  end if;

  v_available_delta := case
    when v_condition in ('ok','ispravno','available','dostupno','raspolozivo','raspoloživo')
     and lower(coalesce(v_item.status,'aktivno')) not in ('za_provjeru','quarantine','oštećeno','osteceno','damaged','superseded','rashod','otpis')
    then v_qty else 0 end;

  update public.equipment_items
  set quantity = coalesce(quantity,0) + v_qty,
      available = greatest(0, coalesce(available,0) + v_available_delta),
      loaned = greatest(0, coalesce(loaned,0)),
      quantity_label = (coalesce(quantity,0) + v_qty)::text,
      available_label = (greatest(0, coalesce(available,0) + v_available_delta))::text,
      original_quantity_text = (coalesce(quantity,0) + v_qty)::text,
      location_name = coalesce(nullif(location_name,''), v_to_location),
      availability = case when greatest(0, coalesce(available,0) + v_available_delta) > 0 then 'dostupno' else availability end,
      updated_at = now(),
      internal_note = case
        when p_note is null or trim(p_note) = '' then internal_note
        else concat_ws(E'\n', nullif(internal_note,''), concat('Legacy return: ', p_note))
      end
  where id = v_item.id
  returning * into v_after;

  if to_regclass('public.equipment_item_locations') is not null then
    update public.equipment_item_locations
    set item_name = v_after.name,
        quantity = greatest(0, coalesce(quantity,0) + v_qty),
        updated_at = now(),
        note = concat_ws(E'\n', nullif(note,''), concat('legacy_return +', v_qty::text, '; condition=', coalesce(p_condition_status,'ok'), coalesce('; ', nullif(p_note,''))))
    where coalesce(equipment_legacy_id,'') = coalesce(v_after.legacy_id,'')
      and lower(location_type) = 'storage'
      and lower(location_name) = lower(v_to_location);

    if not found then
      begin
        insert into public.equipment_item_locations (
          equipment_legacy_id,item_name,location_type,location_name,quantity,note,updated_at
        ) values (
          v_after.legacy_id, v_after.name, 'storage', v_to_location, v_qty,
          concat('legacy_return +', v_qty::text, '; condition=', coalesce(p_condition_status,'ok'), coalesce('; ', nullif(p_note,''))), now()
        );
      exception when unique_violation then
        update public.equipment_item_locations
        set item_name = v_after.name,
            quantity = greatest(0, coalesce(quantity,0) + v_qty),
            updated_at = now()
        where coalesce(equipment_legacy_id,'') = coalesce(v_after.legacy_id,'')
          and lower(location_type) = 'storage'
          and lower(location_name) = lower(v_to_location);
      end;
    end if;
  end if;

  v_event_key := 'LEGACY-RETURN-' || coalesce(nullif(trim(p_client_event_id),''), gen_random_uuid()::text);

  insert into public.sov_armory_stock_movements (
    event_key,movement_type,equipment_legacy_id,item_name,quantity,
    from_location_name,to_location_name,condition_status,source_name,note,client_event_id,created_by
  ) values (
    v_event_key, 'legacy_return', v_after.legacy_id, v_after.name, v_qty,
    'stara / loše evidentirana posudba', v_to_location,
    coalesce(nullif(trim(p_condition_status),''),'ok'),
    coalesce(nullif(trim(p_source_name),''),'Povrat bez otvorene posudbe'),
    p_note, p_client_event_id, auth.uid()
  );

  perform public.sov_rebuild_equipment_catalog_manifest();

  return jsonb_build_object(
    'ok', true,
    'duplicate', false,
    'event_key', v_event_key,
    'movement_type', 'legacy_return',
    'item_id', v_after.id,
    'equipment_legacy_id', v_after.legacy_id,
    'item_name', v_after.name,
    'quantity_added', v_qty,
    'available_added', v_available_delta,
    'new_quantity', v_after.quantity,
    'new_available', v_after.available,
    'quantity_label', v_after.quantity_label,
    'available_label', v_after.available_label,
    'to_location_name', v_to_location
  );
end;
$$;

grant execute on function public.sov_armory_record_legacy_return(uuid,text,text,numeric,text,text,text,text,text) to authenticated;

-- Name cleanup: OCR/import had literal bad wording "stara stopa".
update public.equipment_items
set name = 'Stop descender (za provjeru / oštećen)',
    category_name = 'Sprave',
    subcategory = 'Stop/descender',
    status = case when lower(coalesce(status,'')) in ('aktivno','dostupno') then 'za_provjeru' else status end,
    availability = case when lower(coalesce(status,'')) in ('aktivno','dostupno') then 'za provjeru' else availability end,
    member_visible = false,
    updated_at = now(),
    internal_note = concat_ws(E'\n', nullif(internal_note,''), 'Naziv očišćen Build 4: iz OCR/import teksta "Stop sprava / stara stopa" u "Stop descender (za provjeru / oštećen)".')
where legacy_id = 'KLAICEVA-2026-0089'
   or lower(name) = lower('Stop sprava / stara stopa');

update public.equipment_items
set name = 'Stop descender (stari model)',
    category_name = 'Sprave',
    subcategory = 'Stop/descender',
    updated_at = now(),
    internal_note = concat_ws(E'\n', nullif(internal_note,''), 'Naziv očišćen Build 4: iz OCR/import teksta "Stara stopa" u "Stop descender (stari model)".')
where legacy_id = 'KLAICEVA-2026-0090'
   or lower(name) = lower('Stara stopa');

update public.equipment_item_locations l
set item_name = e.name,
    updated_at = now()
from public.equipment_items e
where l.equipment_legacy_id = e.legacy_id
  and e.legacy_id in ('KLAICEVA-2026-0089','KLAICEVA-2026-0090');

update public.sov_armory_stock_movements m
set item_name = e.name
from public.equipment_items e
where m.equipment_legacy_id = e.legacy_id
  and e.legacy_id in ('KLAICEVA-2026-0089','KLAICEVA-2026-0090');

select public.sov_rebuild_equipment_catalog_manifest();

-- Quick checks:
-- select legacy_id,name,quantity,available,quantity_label,available_label,updated_at from public.equipment_items where legacy_id in ('KLAICEVA-2026-0089','KLAICEVA-2026-0090');
-- select public.sov_armory_upsert_simple_item('KLAICEVA-2026-0090',null,'Stop descender (stari model)','Sprave','Stop/descender','kom',4,4,0,'Oružarstvo Klaićeva','aktivno','dostupno',null,null,true);
