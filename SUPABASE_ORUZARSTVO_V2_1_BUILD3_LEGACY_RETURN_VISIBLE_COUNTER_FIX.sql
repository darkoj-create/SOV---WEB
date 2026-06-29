-- SOV Oružarstvo v2.1 Build 3
-- Fix: legacy_return must change the counters that the web inventory actually reads.
-- Safe to run multiple times. It does not delete data.

create extension if not exists pgcrypto;

-- Ensure manifest table exists, because web/APK cache invalidation depends on it in newer builds.
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
    md5('sov-armory-visible-counter-fix|' || clock_timestamp()::text),
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

create or replace function public.sov_mark_equipment_catalog_manifest_dirty()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.sov_rebuild_equipment_catalog_manifest();
  return coalesce(new, old);
end;
$$;

drop trigger if exists trg_sov_equipment_manifest_items_dirty on public.equipment_items;
create trigger trg_sov_equipment_manifest_items_dirty
after insert or update or delete on public.equipment_items
for each statement execute function public.sov_mark_equipment_catalog_manifest_dirty();

-- Replace RPC with version that updates BOTH the movement/location log AND equipment_items counters.
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
  v_qty_integer integer;
  v_available_delta numeric;
  v_event_key text;
  v_existing public.sov_armory_stock_movements%rowtype;
  v_lookup_legacy text;
  v_to_location text;
  v_condition text;
begin
  if p_quantity is null or p_quantity <= 0 then
    raise exception 'Quantity must be greater than zero' using errcode = '22023';
  end if;

  v_qty_integer := greatest(1, round(p_quantity))::integer;
  v_lookup_legacy := nullif(trim(regexp_replace(coalesce(p_equipment_legacy_id,''), '^item:', '', 'i')), '');
  v_to_location := coalesce(nullif(trim(p_to_location_name),''),'Oružarstvo Klaićeva');
  v_condition := lower(coalesce(nullif(trim(p_condition_status),''),'ok'));

  if p_item_id is null and v_lookup_legacy is null and nullif(trim(coalesce(p_item_name,'')),'') is null then
    raise exception 'Provide p_item_id, p_equipment_legacy_id, or p_item_name' using errcode = '22023';
  end if;

  if p_client_event_id is not null then
    select * into v_existing
    from public.sov_armory_stock_movements
    where client_event_id = p_client_event_id
    limit 1;

    if found then
      return jsonb_build_object(
        'ok', true,
        'duplicate', true,
        'movement_id', v_existing.id,
        'event_key', v_existing.event_key,
        'message', 'client_event_id already recorded; no duplicate stock change was applied'
      );
    end if;
  end if;

  select e.* into v_item
  from public.equipment_items e
  where (p_item_id is not null and e.id = p_item_id)
     or (v_lookup_legacy is not null and (e.legacy_id = v_lookup_legacy or e.catalog_id = v_lookup_legacy))
     or (
       p_item_id is null
       and v_lookup_legacy is null
       and p_item_name is not null
       and lower(trim(e.name)) = lower(trim(p_item_name))
     )
  order by
    case when p_item_id is not null and e.id = p_item_id then 0 else 1 end,
    case when v_lookup_legacy is not null and e.legacy_id = v_lookup_legacy then 0 else 1 end,
    case when v_lookup_legacy is not null and e.catalog_id = v_lookup_legacy then 0 else 1 end,
    e.updated_at desc nulls last
  limit 1;

  if not found then
    raise exception 'Equipment item not found. Add the article first, then record legacy_return.' using errcode = 'P0002';
  end if;

  v_available_delta := case
    when v_condition in ('ok','ispravno','available','dostupno','raspolozivo','raspoloživo')
     and lower(coalesce(v_item.status,'aktivno')) not in ('za_provjeru','quarantine','oštećeno','osteceno','damaged','superseded','rashod','otpis')
    then v_qty_integer
    else 0
  end;

  update public.equipment_items
  set quantity = coalesce(quantity,0) + v_qty_integer,
      available = greatest(0, coalesce(available,0) + v_available_delta),
      loaned = greatest(0, coalesce(loaned,0)),
      quantity_label = (coalesce(quantity,0) + v_qty_integer)::text,
      available_label = (greatest(0, coalesce(available,0) + v_available_delta))::text,
      location_name = coalesce(nullif(location_name,''), v_to_location),
      availability = case
        when greatest(0, coalesce(available,0) + v_available_delta) > 0 and lower(coalesce(status,'')) not in ('za_provjeru','quarantine','oštećeno','osteceno','damaged','superseded','rashod','otpis')
        then 'dostupno'
        else availability
      end,
      updated_at = now(),
      internal_note = case
        when p_note is null or trim(p_note) = '' then internal_note
        else concat_ws(E'\n', nullif(internal_note,''), concat('Legacy return: ', p_note))
      end
  where id = v_item.id
  returning * into v_after;

  update public.equipment_item_locations
  set quantity = greatest(0, coalesce(quantity,0) + v_qty_integer),
      updated_at = now(),
      note = concat_ws(E'\n', nullif(note,''), concat('legacy_return +', v_qty_integer::text, '; condition=', coalesce(p_condition_status,'ok'), coalesce('; ', nullif(p_note,''))))
  where coalesce(equipment_legacy_id,'') = coalesce(v_item.legacy_id,'')
    and lower(item_name) = lower(v_item.name)
    and lower(location_type) = 'storage'
    and lower(location_name) = lower(v_to_location);

  if not found then
    begin
      insert into public.equipment_item_locations (
        equipment_legacy_id,item_name,location_type,location_name,quantity,note,updated_at
      ) values (
        v_item.legacy_id,
        v_item.name,
        'storage',
        v_to_location,
        v_qty_integer,
        concat('legacy_return +', v_qty_integer::text, '; condition=', coalesce(p_condition_status,'ok'), coalesce('; ', nullif(p_note,''))),
        now()
      );
    exception when unique_violation then
      update public.equipment_item_locations
      set quantity = greatest(0, coalesce(quantity,0) + v_qty_integer),
          updated_at = now(),
          note = concat_ws(E'\n', nullif(note,''), concat('legacy_return +', v_qty_integer::text, '; condition=', coalesce(p_condition_status,'ok'), coalesce('; ', nullif(p_note,''))))
      where coalesce(equipment_legacy_id,'') = coalesce(v_item.legacy_id,'')
        and lower(item_name) = lower(v_item.name)
        and lower(location_type) = 'storage'
        and lower(location_name) = lower(v_to_location);
    end;
  end if;

  v_event_key := 'LEGACY-RETURN-' || coalesce(nullif(trim(p_client_event_id),''), gen_random_uuid()::text);

  insert into public.sov_armory_stock_movements (
    event_key,movement_type,equipment_legacy_id,item_name,quantity,
    from_location_name,to_location_name,condition_status,source_name,note,client_event_id,created_by
  ) values (
    v_event_key,
    'legacy_return',
    v_item.legacy_id,
    v_item.name,
    v_qty_integer,
    'stara / loše evidentirana posudba',
    v_to_location,
    coalesce(nullif(trim(p_condition_status),''),'ok'),
    coalesce(nullif(trim(p_source_name),''),'Povrat bez otvorene posudbe'),
    p_note,
    p_client_event_id,
    auth.uid()
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
    'quantity_added', v_qty_integer,
    'available_added', v_available_delta,
    'new_quantity', v_after.quantity,
    'new_available', v_after.available,
    'to_location_name', v_to_location
  );
end;
$$;

grant execute on function public.sov_armory_record_legacy_return(uuid,text,text,numeric,text,text,text,text,text) to authenticated;

-- Rebuild manifest now, so browser cache knows catalog changed.
select public.sov_rebuild_equipment_catalog_manifest();

-- Optional check after testing a return:
-- select legacy_id, name, quantity, available, quantity_label, available_label, updated_at
-- from public.equipment_items
-- where name ilike '%karabiner%'
-- order by updated_at desc
-- limit 20;
