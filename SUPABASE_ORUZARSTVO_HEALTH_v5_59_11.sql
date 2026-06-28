-- SOV Web v5.59.11 — Oružarstvo health RPC
-- Purpose: sync-status.html can check Oružarstvo without hard failing if one optional table/view is missing.

create or replace function public.sov_oruzarstvo_health()
returns jsonb
language plpgsql
security definer
set search_path = public
as $fn$
declare
  v_equipment_items int := null;
  v_equipment_ropes int := null;
  v_equipment_pieces int := null;
  v_equipment_locations int := null;
  v_equipment_requests int := null;
  v_equipment_request_items int := null;
  v_equipment_loans int := null;
  v_grouped_catalog int := null;
  v_raw_catalog int := null;
  v_manifest int := null;
  v_open_requests int := null;
  v_by_category jsonb := '{}'::jsonb;
  v_manifest_row jsonb := null;
  v_total int := 0;
begin
  if to_regclass('public.equipment_items') is not null then
    execute 'select count(*)::int from public.equipment_items' into v_equipment_items;
  end if;

  if to_regclass('public.equipment_ropes') is not null then
    execute 'select count(*)::int from public.equipment_ropes' into v_equipment_ropes;
  end if;

  if to_regclass('public.equipment_pieces') is not null then
    execute 'select count(*)::int from public.equipment_pieces' into v_equipment_pieces;
  end if;

  if to_regclass('public.equipment_locations') is not null then
    execute 'select count(*)::int from public.equipment_locations' into v_equipment_locations;
  end if;

  if to_regclass('public.equipment_requests') is not null then
    execute 'select count(*)::int from public.equipment_requests' into v_equipment_requests;
    execute $sql$select count(*)::int from public.equipment_requests
             where lower(coalesce(status,'')) in ('pending','issued','partial_return','approved','prepared','reserved')$sql$
      into v_open_requests;
  end if;

  if to_regclass('public.equipment_request_items') is not null then
    execute 'select count(*)::int from public.equipment_request_items' into v_equipment_request_items;
  end if;

  if to_regclass('public.equipment_loans') is not null then
    execute 'select count(*)::int from public.equipment_loans' into v_equipment_loans;
  end if;

  if to_regclass('public.sov_equipment_app_catalog_grouped') is not null then
    execute 'select count(*)::int from public.sov_equipment_app_catalog_grouped' into v_grouped_catalog;
    begin
      execute $sql$
        select coalesce(jsonb_object_agg(main_category, cnt), '{}'::jsonb)
        from (
          select coalesce(nullif(main_category,''), 'Ostalo') as main_category, count(*)::int as cnt
          from public.sov_equipment_app_catalog_grouped
          group by 1
        ) s
      $sql$ into v_by_category;
    exception when others then
      v_by_category := '{}'::jsonb;
    end;
  end if;

  if to_regclass('public.sov_equipment_app_catalog') is not null then
    execute 'select count(*)::int from public.sov_equipment_app_catalog' into v_raw_catalog;
  end if;

  if to_regclass('public.sov_equipment_catalog_manifest') is not null then
    execute 'select count(*)::int from public.sov_equipment_catalog_manifest' into v_manifest;
    begin
      execute $sql$select to_jsonb(m) from public.sov_equipment_catalog_manifest m limit 1$sql$ into v_manifest_row;
    exception when others then
      v_manifest_row := null;
    end;
  end if;

  v_total := greatest(
    coalesce(v_grouped_catalog,0),
    coalesce(v_raw_catalog,0),
    coalesce(v_equipment_items,0) + coalesce(v_equipment_ropes,0) + coalesce(v_equipment_pieces,0)
  );

  return jsonb_build_object(
    'ok', v_total > 0,
    'checked_at', now(),
    'catalog_total', v_total,
    'open_requests', coalesce(v_open_requests,0),
    'tables', jsonb_build_object(
      'equipment_items', coalesce(v_equipment_items,0),
      'equipment_ropes', coalesce(v_equipment_ropes,0),
      'equipment_pieces', coalesce(v_equipment_pieces,0),
      'equipment_locations', coalesce(v_equipment_locations,0),
      'equipment_requests', coalesce(v_equipment_requests,0),
      'equipment_request_items', coalesce(v_equipment_request_items,0),
      'equipment_loans', coalesce(v_equipment_loans,0),
      'grouped_catalog', coalesce(v_grouped_catalog,0),
      'raw_catalog', coalesce(v_raw_catalog,0),
      'manifest', coalesce(v_manifest,0)
    ),
    'by_category', v_by_category,
    'manifest', v_manifest_row,
    'frontend_cache_key', 'sov_armory_catalog_cache_v55911',
    'build', '5.59.11'
  );
end;
$fn$;

grant execute on function public.sov_oruzarstvo_health() to authenticated;

-- Quick manual test:
-- select public.sov_oruzarstvo_health();
