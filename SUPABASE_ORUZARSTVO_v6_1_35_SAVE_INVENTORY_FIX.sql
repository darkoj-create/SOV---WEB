-- SOV Oružarstvo v6.1.35 SAVE INVENTORY FIX
-- Purpose:
-- 1) Keep the old Oružarstvo catalog as the main catalog.
-- 2) Use Inventura only as a manual correction of current Klaićeva counts.
-- 3) Fix web save by using a SECURITY DEFINER RPC instead of direct browser updates blocked by RLS.
--
-- Run this in Supabase SQL editor BEFORE deploying the matching web build.

create or replace function public.sov_armory_save_inventory_count(
  p_identifier text,
  p_item_name text default null,
  p_available numeric default 0,
  p_status text default 'aktivno',
  p_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_available integer := greatest(0, coalesce(round(p_available)::integer, 0));
  v_item public.equipment_items%rowtype;
  v_status text := nullif(trim(coalesce(p_status,'aktivno')), '');
  v_note text := nullif(trim(coalesce(p_note,'')), '');
  v_total integer;
begin
  if nullif(trim(coalesce(p_identifier,'')), '') is null and nullif(trim(coalesce(p_item_name,'')), '') is null then
    raise exception 'Missing item identifier/name';
  end if;

  select * into v_item
  from public.equipment_items ei
  where
    (nullif(trim(coalesce(p_identifier,'')), '') is not null and (
      ei.id::text = trim(p_identifier)
      or ei.legacy_id = trim(p_identifier)
      or ei.catalog_id = trim(p_identifier)
    ))
    or (
      nullif(trim(coalesce(p_item_name,'')), '') is not null
      and lower(ei.name) = lower(trim(p_item_name))
    )
  order by
    case
      when ei.legacy_id = trim(coalesce(p_identifier,'')) then 1
      when ei.catalog_id = trim(coalesce(p_identifier,'')) then 2
      when ei.id::text = trim(coalesce(p_identifier,'')) then 3
      else 9
    end,
    ei.updated_at desc nulls last
  limit 1;

  if not found then
    raise exception 'Equipment item not found: % / %', p_identifier, p_item_name;
  end if;

  -- Manual count = what is physically available in Klaićeva.
  -- Existing loan counter is preserved, so total does not erase active loans.
  v_total := v_available + greatest(0, coalesce(v_item.loaned,0));

  update public.equipment_items ei
  set
    available = v_available,
    quantity = v_total,
    available_label = v_available::text,
    quantity_label = v_total::text,
    availability = case when v_available > 0 then 'dostupno' else 'nedostupno' end,
    status = coalesce(v_status, case when v_available > 0 then 'aktivno' else 'za provjeru' end),
    last_inventory_date = current_date,
    internal_note = left(concat_ws(E'\n', nullif(ei.internal_note,''), v_note), 4000),
    updated_at = now()
  where ei.id = v_item.id
  returning * into v_item;

  if to_regclass('public.equipment_item_locations') is not null then
    begin
      -- Best effort location mirror. Do not fail the real save if this auxiliary table has different constraints.
      insert into public.equipment_item_locations(equipment_legacy_id,item_name,location_type,location_name,quantity,updated_at)
      values(coalesce(v_item.legacy_id, v_item.catalog_id, v_item.id::text), v_item.name, 'storage', 'Oružarstvo Klaićeva', v_available, now())
      on conflict do nothing;
    exception when others then
      null;
    end;
  end if;

  return jsonb_build_object(
    'ok', true,
    'id', v_item.id,
    'legacy_id', v_item.legacy_id,
    'name', v_item.name,
    'available', v_item.available,
    'quantity', v_item.quantity,
    'loaned', v_item.loaned,
    'status', v_item.status
  );
end;
$$;

grant execute on function public.sov_armory_save_inventory_count(text,text,numeric,text,text) to authenticated;

-- Optional cache/manifest bump, only if the manifest table exists.
do $$
begin
  if to_regclass('public.sov_equipment_catalog_manifest') is not null then
    update public.sov_equipment_catalog_manifest
    set catalog_version = concat(coalesce(catalog_version::text,'catalog'), '-savefix-', to_char(now(),'YYYYMMDDHH24MISS'))
    where true;
  end if;
exception when others then
  null;
end $$;
