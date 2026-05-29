-- SOV Cloud v5.58.17 — karta.html uses the same source as Arhivar
-- Fixes: old set-returning map RPC / PostgREST cap of 1000 rows, wrong status source,
-- and mismatch between Karta and Arhivar missing-category logic.
-- Safe to run multiple times.

-- Give authenticated/web users read access to the read-only Arhivar views when they exist.
do $$
begin
  if to_regclass('public.sov_arhivar_worklist') is not null then
    execute 'grant select on public.sov_arhivar_worklist to anon, authenticated';
  end if;
  if to_regclass('public.sov_arhivar_dashboard') is not null then
    execute 'grant select on public.sov_arhivar_dashboard to anon, authenticated';
  end if;
exception when others then
  raise notice 'Grant on Arhivar views skipped: %', sqlerrm;
end $$;

-- New page RPC: returns ONE jsonb object per call, so PostgREST row caps no longer truncate
-- the map feed at 1000 rows. Frontend calls it repeatedly with p_offset/p_limit.
create or replace function public.sov_map_objects_page(
  p_offset integer default 0,
  p_limit integer default 1000
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  off integer := greatest(0, coalesce(p_offset, 0));
  lim integer := greatest(1, least(coalesce(p_limit, 1000), 1000));
  rows jsonb := '[]'::jsonb;
  src text := 'none';
begin
  -- Primary source: EXACT same logical source as Arhivar.
  if to_regclass('public.sov_arhivar_worklist') is not null then
    execute format($q$
      select coalesce(jsonb_agg(to_jsonb(t) order by t.priority_score desc nulls last, t.object_name asc nulls last), '[]'::jsonb)
      from (
        select *
        from public.sov_arhivar_worklist
        order by priority_score desc nulls last, object_name asc nulls last
        offset %s limit %s
      ) t
    $q$, off, lim)
    into rows;
    src := 'sov_arhivar_worklist';
    return jsonb_build_object('source', src, 'offset', off, 'limit', lim, 'rows', rows, 'count', jsonb_array_length(rows));
  end if;

  -- Fallback if somebody skipped Arhivar SQL but has staging table.
  if to_regclass('public.speleo_objects_staging') is not null then
    execute format($q$
      select coalesce(jsonb_agg(to_jsonb(t) order by coalesce(t.updated_at, now()) desc nulls last), '[]'::jsonb)
      from (
        select *
        from public.speleo_objects_staging
        order by updated_at desc nulls last
        offset %s limit %s
      ) t
    $q$, off, lim)
    into rows;
    src := 'speleo_objects_staging';
    return jsonb_build_object('source', src, 'offset', off, 'limit', lim, 'rows', rows, 'count', jsonb_array_length(rows));
  end if;

  if to_regclass('public.speleo_objects_live_sql') is not null then
    execute format($q$
      select coalesce(jsonb_agg(to_jsonb(t)), '[]'::jsonb)
      from (
        select *
        from public.speleo_objects_live_sql
        offset %s limit %s
      ) t
    $q$, off, lim)
    into rows;
    src := 'speleo_objects_live_sql';
    return jsonb_build_object('source', src, 'offset', off, 'limit', lim, 'rows', rows, 'count', jsonb_array_length(rows));
  end if;

  return jsonb_build_object('source', src, 'offset', off, 'limit', lim, 'rows', rows, 'count', 0);
end;
$$;

grant execute on function public.sov_map_objects_page(integer, integer) to anon, authenticated;

-- Keep legacy RPC name alive, but point it to Arhivar worklist first.
-- It remains only a fallback because set-returning functions can still be capped by API settings.
create or replace function public.sov_map_objects(p_limit integer default 20000)
returns table(data jsonb)
language plpgsql
security definer
set search_path = public
as $$
declare
  lim integer := greatest(1, least(coalesce(p_limit, 20000), 50000));
begin
  if to_regclass('public.sov_arhivar_worklist') is not null then
    return query execute format(
      'select to_jsonb(t) from (select * from public.sov_arhivar_worklist order by priority_score desc nulls last, object_name asc nulls last limit %s) t', lim
    );
    return;
  end if;

  if to_regclass('public.speleo_objects_staging') is not null then
    return query execute format(
      'select to_jsonb(t) from (select * from public.speleo_objects_staging limit %s) t', lim
    );
    return;
  end if;

  if to_regclass('public.speleo_objects_live_sql') is not null then
    return query execute format(
      'select to_jsonb(t) from (select * from public.speleo_objects_live_sql limit %s) t', lim
    );
    return;
  end if;

  return;
end;
$$;

grant execute on function public.sov_map_objects(integer) to anon, authenticated;

comment on function public.sov_map_objects_page(integer, integer) is
'SOV Cloud v5.58.17 paged map feed. Primary source is sov_arhivar_worklist, matching Arhivar missing-category/status logic. Returns a single JSON object to avoid PostgREST 1000-row truncation.';
