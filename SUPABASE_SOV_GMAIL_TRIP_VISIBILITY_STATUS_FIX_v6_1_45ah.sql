-- SOV web v6.1.45ah
-- Fix: Gmail/zapisnici approval into sov_trips must normalize visibility/status too.
-- Safe/idempotent. Already applied to Supabase project ncomefzkuixyfixisrhi on 2026-06-29.

begin;


create or replace function public.sov_normalize_trip_category(p_category text)
returns text
language sql
immutable
as $$
  select case
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%seminar%' then 'Seminar'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%skupstin%' then 'Skupština'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%skup%' then 'Skup'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%ekspedic%' then 'Ekspedicija'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%expedic%' then 'Ekspedicija'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%invent%' then 'Inventura'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%predav%' then 'Predavanje'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%vjezb%' then 'Izlet'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%trening%' then 'Izlet'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%samospas%' then 'Izlet'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%izlet%' then 'Izlet'
    when translate(lower(btrim(coalesce(p_category,''))), 'čćšžđ', 'ccszd') like '%teren%' then 'Izlet'
    when btrim(coalesce(p_category,'')) in ('Izlet','Seminar','Skup','Ekspedicija','Inventura','Skupština','Predavanje') then btrim(p_category)
    else 'Izlet'
  end;
$$;

create or replace function public.sov_normalize_trip_visibility(p_visibility text)
returns text
language sql
immutable
as $$
  select case
    when translate(lower(btrim(coalesce(p_visibility,''))), 'čćšžđ', 'ccszd') in ('public','javno','javna','open','otvoreno','otvorena') then 'public'
    when translate(lower(btrim(coalesce(p_visibility,''))), 'čćšžđ', 'ccszd') in ('private','privatno','privatna','hidden','skriveno','skrivena','draft') then 'private'
    when translate(lower(btrim(coalesce(p_visibility,''))), 'čćšžđ', 'ccszd') in ('club','members','member','clanovi','clan','authenticated','staff','admin','internal','sov','samo clanovi') then 'club'
    else 'club'
  end;
$$;

create or replace function public.sov_normalize_trip_status(p_status text)
returns text
language sql
immutable
as $$
  select case
    when translate(lower(btrim(coalesce(p_status,''))), 'čćšžđ', 'ccszd') in ('draft','nacrt') then 'draft'
    when translate(lower(btrim(coalesce(p_status,''))), 'čćšžđ', 'ccszd') in ('active','aktivan','u tijeku','u_tijeku') then 'active'
    when translate(lower(btrim(coalesce(p_status,''))), 'čćšžđ', 'ccszd') in ('done','finished','zavrseno','zavrsen','odrzano','odrađeno','odradeno') then 'done'
    when translate(lower(btrim(coalesce(p_status,''))), 'čćšžđ', 'ccszd') in ('cancelled','canceled','otkazano','otkazan') then 'cancelled'
    when translate(lower(btrim(coalesce(p_status,''))), 'čćšžđ', 'ccszd') in ('archived','arhivirano','arhiviran') then 'archived'
    else 'planned'
  end;
$$;

create or replace function public.sov_trips_normalize_category_trigger()
returns trigger
language plpgsql
as $$
begin
  new.trip_category := public.sov_normalize_trip_category(new.trip_category);
  new.visibility := public.sov_normalize_trip_visibility(new.visibility);
  new.status := public.sov_normalize_trip_status(new.status);
  new.meta := coalesce(new.meta, '{}'::jsonb)
    || jsonb_build_object(
      'trip_category', new.trip_category,
      'visibility', new.visibility,
      'status', new.status
    );
  return new;
end;
$$;

drop trigger if exists trg_sov_trips_normalize_category on public.sov_trips;
create trigger trg_sov_trips_normalize_category
before insert or update of trip_category, visibility, status, meta on public.sov_trips
for each row
execute function public.sov_trips_normalize_category_trigger();

update public.sov_trips
set trip_category = public.sov_normalize_trip_category(trip_category),
    visibility = public.sov_normalize_trip_visibility(visibility),
    status = public.sov_normalize_trip_status(status),
    meta = coalesce(meta, '{}'::jsonb)
      || jsonb_build_object(
        'trip_category', public.sov_normalize_trip_category(trip_category),
        'visibility', public.sov_normalize_trip_visibility(visibility),
        'status', public.sov_normalize_trip_status(status)
      )
where trip_category is null
   or visibility is null
   or status is null
   or trip_category <> public.sov_normalize_trip_category(trip_category)
   or visibility <> public.sov_normalize_trip_visibility(visibility)
   or status <> public.sov_normalize_trip_status(status)
   or coalesce(meta->>'trip_category','') <> public.sov_normalize_trip_category(trip_category)
   or coalesce(meta->>'visibility','') <> public.sov_normalize_trip_visibility(visibility)
   or coalesce(meta->>'status','') <> public.sov_normalize_trip_status(status);

commit;
