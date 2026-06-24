-- SOV web v6.1.15 — hard delete fix for trips
-- Run once in Supabase SQL editor.

begin;

-- Include Webmaster in trip manager helper. Older trip setup only allowed admin/editor/urednik.
create or replace function public.sov_can_manage_trips_safe()
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_is_webmaster boolean := false;
begin
  begin
    if to_regprocedure('public.sov_is_webmaster()') is not null then
      execute 'select public.sov_is_webmaster()' into v_is_webmaster;
    end if;
  exception when others then
    v_is_webmaster := false;
  end;

  return v_is_webmaster
    or public.sov_has_any_role_safe(array['webmaster','admin','editor','urednik']);
end;
$$;

grant execute on function public.sov_can_manage_trips_safe() to anon, authenticated;

-- Explicit admin/webmaster delete RPC. This avoids silent 0-row deletes through RLS.
create or replace function public.sov_delete_trip_admin(p_trip_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_deleted uuid;
  v_exists boolean;
begin
  if auth.uid() is null then
    raise exception 'Nisi prijavljen.' using errcode = '42501';
  end if;

  if not public.sov_can_manage_trips_safe() then
    raise exception 'Nemaš ovlasti za brisanje izleta.' using errcode = '42501';
  end if;

  select exists(select 1 from public.sov_trips where id = p_trip_id) into v_exists;
  if not v_exists then
    return jsonb_build_object('deleted', false, 'id', p_trip_id, 'message', 'Izlet nije pronađen u bazi.');
  end if;

  -- Child tables have ON DELETE CASCADE where applicable. This explicit delete is kept
  -- narrow and lets constraints/cascades do the correct cleanup.
  delete from public.sov_trips
  where id = p_trip_id
  returning id into v_deleted;

  if v_deleted is null then
    return jsonb_build_object('deleted', false, 'id', p_trip_id, 'message', 'Izlet nije obrisan.');
  end if;

  return jsonb_build_object('deleted', true, 'id', v_deleted);
end;
$$;

grant execute on function public.sov_delete_trip_admin(uuid) to authenticated;

commit;
