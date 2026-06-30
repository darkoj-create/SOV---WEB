-- SOV Admin v1.4.23 — read-RPC za aktivne posudbe/zahtjeve opreme
-- Cilj: APK (i web) više ne moraju znati za kolonu armory_hidden;
-- "nije skriveno" pravilo živi u bazi. Vraća setof equipment_requests,
-- pa je JSON oblik identičan kao equipment_requests?select=* (APK fallback radi bez promjena).
--
-- Sigurno za produkciju: samo SELECT, bez mutacija, bez brisanja, bez RLS izmjena.

create or replace function public.sov_armory_get_active_requests(p_limit integer default 150)
returns setof public.equipment_requests
language sql
security definer
set search_path = public
as $$
  select *
  from public.equipment_requests
  where armory_hidden is not true            -- uključuje false I null (legacy + novi redovi)
  order by created_at desc
  limit greatest(1, least(coalesce(p_limit, 150), 500));
$$;

grant execute on function public.sov_armory_get_active_requests(integer) to authenticated;
