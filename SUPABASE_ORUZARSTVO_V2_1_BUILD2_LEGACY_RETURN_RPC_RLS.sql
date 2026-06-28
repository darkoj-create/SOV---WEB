-- SOV Oružarstvo v2.1 / Build 2
-- Legacy return RPC + conservative RLS grants for stock movement workflow
--
-- Purpose:
-- 1) After Build 1 opening balance, allow old/poorly documented returns to be recorded safely.
-- 2) Frontend/APK should call RPC instead of directly updating stock quantities.
-- 3) This does NOT delete old loans, does NOT reconstruct legacy loans, does NOT use QR/barcodes.
--
-- IMPORTANT:
-- - Run Build 1 first: SUPABASE_ORUZARSTVO_V2_1_BUILD1_KLAICEVA_OPENING_BALANCE.sql
-- - Then run this file.
-- - This patch intentionally does NOT open broad direct-write RLS on equipment_assets.
--   If the current web/APK still writes directly into equipment_assets, that app code must be moved to RPC,
--   or a separate compatibility policy must be consciously added.

begin;

create extension if not exists pgcrypto;

-- Keep core tables present even if this patch is run on a partially prepared DB.
create table if not exists public.equipment_items (
  id uuid primary key default gen_random_uuid(),
  legacy_id text unique,
  catalog_id text,
  name text not null,
  category_id text,
  category_name text,
  subcategory text,
  unit text default 'kom',
  tracking_type text,
  quantity numeric default 0,
  loaned numeric default 0,
  available numeric default 0,
  minimum numeric,
  status text default 'aktivno',
  availability text default 'dostupno',
  member_visible boolean not null default true,
  internal_note text,
  source_sheet text,
  item_kind text,
  code_required boolean not null default false,
  physical_code_note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

alter table public.equipment_items
  add column if not exists legacy_id text,
  add column if not exists catalog_id text,
  add column if not exists name text,
  add column if not exists category_name text,
  add column if not exists subcategory text,
  add column if not exists unit text default 'kom',
  add column if not exists quantity numeric default 0,
  add column if not exists loaned numeric default 0,
  add column if not exists available numeric default 0,
  add column if not exists status text default 'aktivno',
  add column if not exists availability text default 'dostupno',
  add column if not exists member_visible boolean not null default true,
  add column if not exists internal_note text,
  add column if not exists updated_at timestamptz not null default now();

create unique index if not exists equipment_items_legacy_id_uq on public.equipment_items(legacy_id);
create index if not exists equipment_items_name_idx on public.equipment_items(lower(name));

create table if not exists public.equipment_item_locations (
  id uuid primary key default gen_random_uuid(),
  equipment_legacy_id text,
  item_name text not null,
  location_type text not null default 'storage',
  location_name text not null default 'Oružarstvo Klaićeva',
  quantity integer not null default 0,
  note text,
  updated_at timestamptz not null default now(),
  constraint equipment_item_locations_qty_nonnegative check (quantity >= 0)
);

create unique index if not exists equipment_item_locations_unique
on public.equipment_item_locations (
  coalesce(equipment_legacy_id,''),
  lower(item_name),
  lower(location_type),
  lower(location_name)
);

create table if not exists public.sov_armory_stock_movements (
  id uuid primary key default gen_random_uuid(),
  event_key text unique,
  movement_type text not null,
  equipment_legacy_id text,
  item_name text not null,
  quantity numeric not null default 0,
  from_location_name text,
  to_location_name text not null default 'Oružarstvo Klaićeva',
  condition_status text,
  source_name text,
  source_row integer,
  note text,
  client_event_id text,
  created_by uuid default auth.uid(),
  created_at timestamptz not null default now()
);

create index if not exists sov_armory_stock_movements_type_idx on public.sov_armory_stock_movements(movement_type, created_at desc);
create index if not exists sov_armory_stock_movements_item_idx on public.sov_armory_stock_movements(equipment_legacy_id, item_name);
create unique index if not exists sov_armory_stock_movements_client_event_uq
  on public.sov_armory_stock_movements(client_event_id)
  where client_event_id is not null;

-- Main simple legacy return RPC.
-- For bulk items: increase physical stock in Klaićeva and log movement legacy_return.
-- Availability increases only when condition is OK and item itself is active/available.
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
  v_item record;
  v_qty_integer integer;
  v_available_delta numeric;
  v_event_key text;
  v_existing public.sov_armory_stock_movements%rowtype;
begin
  if p_quantity is null or p_quantity <= 0 then
    raise exception 'Quantity must be greater than zero' using errcode = '22023';
  end if;

  if p_item_id is null and nullif(trim(coalesce(p_equipment_legacy_id,'')),'') is null and nullif(trim(coalesce(p_item_name,'')),'') is null then
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
     or (p_equipment_legacy_id is not null and e.legacy_id = p_equipment_legacy_id)
     or (
       p_item_id is null
       and p_equipment_legacy_id is null
       and p_item_name is not null
       and lower(e.name) = lower(trim(p_item_name))
     )
  order by
    case when p_item_id is not null and e.id = p_item_id then 0 else 1 end,
    case when p_equipment_legacy_id is not null and e.legacy_id = p_equipment_legacy_id then 0 else 1 end,
    e.updated_at desc nulls last
  limit 1;

  if not found then
    raise exception 'Equipment item not found. Add the article first, then record legacy_return.' using errcode = 'P0002';
  end if;

  v_qty_integer := greatest(1, round(p_quantity))::integer;

  v_available_delta := case
    when lower(coalesce(p_condition_status,'ok')) in ('ok','ispravno','available','dostupno')
     and lower(coalesce(v_item.status,'aktivno')) not in ('za_provjeru','quarantine','oštećeno','osteceno','damaged','superseded')
    then p_quantity
    else 0
  end;

  update public.equipment_items
  set quantity = coalesce(quantity,0) + p_quantity,
      available = greatest(0, coalesce(available,0) + v_available_delta),
      updated_at = now(),
      internal_note = case
        when p_note is null or trim(p_note) = '' then internal_note
        else concat_ws(E'\n', nullif(internal_note,''), concat('Legacy return: ', p_note))
      end
  where id = v_item.id;

  update public.equipment_item_locations
  set quantity = greatest(0, coalesce(quantity,0) + v_qty_integer),
      updated_at = now(),
      note = concat_ws(E'\n', nullif(note,''), concat('legacy_return +', p_quantity::text, '; condition=', coalesce(p_condition_status,'ok'), coalesce('; ', nullif(p_note,''))))
  where coalesce(equipment_legacy_id,'') = coalesce(v_item.legacy_id,'')
    and lower(item_name) = lower(v_item.name)
    and lower(location_type) = 'storage'
    and lower(location_name) = lower(coalesce(nullif(trim(p_to_location_name),''),'Oružarstvo Klaićeva'));

  if not found then
    begin
      insert into public.equipment_item_locations (
        equipment_legacy_id,item_name,location_type,location_name,quantity,note,updated_at
      ) values (
        v_item.legacy_id,
        v_item.name,
        'storage',
        coalesce(nullif(trim(p_to_location_name),''),'Oružarstvo Klaićeva'),
        v_qty_integer,
        concat('legacy_return +', p_quantity::text, '; condition=', coalesce(p_condition_status,'ok'), coalesce('; ', nullif(p_note,''))),
        now()
      );
    exception when unique_violation then
      update public.equipment_item_locations
      set quantity = greatest(0, coalesce(quantity,0) + v_qty_integer),
          updated_at = now(),
          note = concat_ws(E'\n', nullif(note,''), concat('legacy_return +', p_quantity::text, '; condition=', coalesce(p_condition_status,'ok'), coalesce('; ', nullif(p_note,''))))
      where coalesce(equipment_legacy_id,'') = coalesce(v_item.legacy_id,'')
        and lower(item_name) = lower(v_item.name)
        and lower(location_type) = 'storage'
        and lower(location_name) = lower(coalesce(nullif(trim(p_to_location_name),''),'Oružarstvo Klaićeva'));
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
    p_quantity,
    'stara / loše evidentirana posudba',
    coalesce(nullif(trim(p_to_location_name),''),'Oružarstvo Klaićeva'),
    coalesce(nullif(trim(p_condition_status),''),'ok'),
    coalesce(nullif(trim(p_source_name),''),'Povrat bez otvorene posudbe'),
    p_note,
    p_client_event_id,
    auth.uid()
  );

  return jsonb_build_object(
    'ok', true,
    'duplicate', false,
    'event_key', v_event_key,
    'movement_type', 'legacy_return',
    'item_id', v_item.id,
    'equipment_legacy_id', v_item.legacy_id,
    'item_name', v_item.name,
    'quantity_added', p_quantity,
    'available_added', v_available_delta,
    'to_location_name', coalesce(nullif(trim(p_to_location_name),''),'Oružarstvo Klaićeva')
  );
end;
$$;

-- Optional helper: add a new simple bulk item and immediately record a legacy return.
-- Use this only when returned equipment does not exist in catalog yet.
create or replace function public.sov_armory_add_item_and_legacy_return(
  p_item_name text,
  p_category_name text default 'Za provjeru',
  p_subcategory text default null,
  p_unit text default 'kom',
  p_quantity numeric default 1,
  p_condition_status text default 'za_provjeru',
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
  v_legacy_id text;
  v_item_id uuid;
  v_result jsonb;
begin
  if nullif(trim(coalesce(p_item_name,'')),'') is null then
    raise exception 'Item name is required' using errcode = '22023';
  end if;

  select id, legacy_id into v_item_id, v_legacy_id
  from public.equipment_items
  where lower(name) = lower(trim(p_item_name))
  order by updated_at desc nulls last
  limit 1;

  if v_item_id is null then
    v_legacy_id := 'LEGACY-RETURN-' || upper(substr(replace(gen_random_uuid()::text,'-',''),1,12));

    insert into public.equipment_items (
      legacy_id,catalog_id,name,category_name,subcategory,unit,tracking_type,
      quantity,loaned,available,status,availability,member_visible,source_sheet,item_kind,internal_note,created_at,updated_at
    ) values (
      v_legacy_id,
      v_legacy_id,
      trim(p_item_name),
      coalesce(nullif(trim(p_category_name),''),'Za provjeru'),
      nullif(trim(coalesce(p_subcategory,'')),''),
      coalesce(nullif(trim(p_unit),''),'kom'),
      'bulk',
      0,0,0,
      case when lower(coalesce(p_condition_status,'')) in ('ok','ispravno','available','dostupno') then 'aktivno' else 'za_provjeru' end,
      case when lower(coalesce(p_condition_status,'')) in ('ok','ispravno','available','dostupno') then 'dostupno' else 'za provjeru' end,
      case when lower(coalesce(p_condition_status,'')) in ('ok','ispravno','available','dostupno') then true else false end,
      'Legacy return manual add',
      'legacy_return',
      concat('Created from legacy_return workflow. ', coalesce(p_note,'')),
      now(),now()
    ) returning id into v_item_id;
  end if;

  v_result := public.sov_armory_record_legacy_return(
    p_item_id := v_item_id,
    p_equipment_legacy_id := null,
    p_item_name := null,
    p_quantity := p_quantity,
    p_to_location_name := 'Oružarstvo Klaićeva',
    p_condition_status := p_condition_status,
    p_source_name := p_source_name,
    p_note := p_note,
    p_client_event_id := p_client_event_id
  );

  return v_result;
end;
$$;

-- Grants: frontend/APK call RPC; no direct table update is required for legacy_return.
grant execute on function public.sov_armory_record_legacy_return(uuid,text,text,numeric,text,text,text,text,text) to authenticated;
grant execute on function public.sov_armory_add_item_and_legacy_return(text,text,text,text,numeric,text,text,text,text) to authenticated;

grant select on public.equipment_items to authenticated;
grant select on public.equipment_item_locations to authenticated;
grant select on public.sov_armory_stock_movements to authenticated;

-- Conservative read policies only for newly introduced/read-used tables.
-- Writes are intentionally through SECURITY DEFINER RPC above.
do $$
begin
  if to_regclass('public.equipment_items') is not null then
    execute 'alter table public.equipment_items enable row level security';
    execute 'drop policy if exists equipment_items_authenticated_read on public.equipment_items';
    execute 'create policy equipment_items_authenticated_read on public.equipment_items for select to authenticated using (true)';
  end if;

  if to_regclass('public.equipment_item_locations') is not null then
    execute 'alter table public.equipment_item_locations enable row level security';
    execute 'drop policy if exists equipment_item_locations_authenticated_read on public.equipment_item_locations';
    execute 'create policy equipment_item_locations_authenticated_read on public.equipment_item_locations for select to authenticated using (true)';
  end if;

  if to_regclass('public.sov_armory_stock_movements') is not null then
    execute 'alter table public.sov_armory_stock_movements enable row level security';
    execute 'drop policy if exists sov_armory_stock_movements_authenticated_read on public.sov_armory_stock_movements';
    execute 'create policy sov_armory_stock_movements_authenticated_read on public.sov_armory_stock_movements for select to authenticated using (true)';
  end if;
end $$;

commit;

-- VALIDACIJA:
-- 1) Primjer test-povrata nad postojećim artiklom. Prvo pronađi legacy_id:
-- select legacy_id,name,quantity,available,status from public.equipment_items where name ilike '%Karabiner%' order by name limit 10;
--
-- 2) Test, zamijeni legacy_id stvarnim:
-- select public.sov_armory_record_legacy_return(
--   p_equipment_legacy_id := 'KLAICEVA-2026-XXXX',
--   p_quantity := 1,
--   p_source_name := 'TEST',
--   p_note := 'TEST legacy return - obrisati/poništiti ručno ako ne treba',
--   p_client_event_id := 'TEST-LEGACY-RETURN-001'
-- );
--
-- 3) Provjera:
-- select * from public.sov_armory_stock_movements where client_event_id='TEST-LEGACY-RETURN-001';
-- select legacy_id,name,quantity,available from public.equipment_items where legacy_id='KLAICEVA-2026-XXXX';
