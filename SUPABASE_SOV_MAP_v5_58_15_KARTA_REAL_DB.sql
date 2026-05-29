-- SOV Cloud v5.58.15 — Karta reads the real Supabase speleo object database
-- Safe to run multiple times.

create or replace function public.sov_map_objects(p_limit integer default 20000)
returns table(data jsonb)
language plpgsql
security definer
set search_path = public
as $$
declare
  lim integer := greatest(1, least(coalesce(p_limit, 20000), 50000));
begin
  -- Primary source: the current real object DB used by Arhivar/workflow.
  if to_regclass('public.speleo_objects_staging') is not null then
    return query execute format(
      'select to_jsonb(t) from (select * from public.speleo_objects_staging limit %s) t', lim
    );
    return;
  end if;

  -- Fallback for older installs that promoted objects into live SQL.
  if to_regclass('public.speleo_objects_live_sql') is not null then
    return query execute format(
      'select to_jsonb(t) from (select * from public.speleo_objects_live_sql limit %s) t', lim
    );
    return;
  end if;

  -- Empty result instead of hard failure, so frontend can show a friendly message.
  return;
end;
$$;

grant execute on function public.sov_map_objects(integer) to anon, authenticated;

comment on function public.sov_map_objects(integer) is
'SOV Cloud v5.58.15 map feed. Returns JSON rows from the current real speleo object database, primarily speleo_objects_staging, for karta.html.';
