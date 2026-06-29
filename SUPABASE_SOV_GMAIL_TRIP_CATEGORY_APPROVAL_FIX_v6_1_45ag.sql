-- SOV v6.1.45ag — Gmail/native zapisnici: fix odobravanja izleta u kalendar
-- Problem: najave iz zapisnika mogu nositi trip_category kao male/nekontrolirane vrijednosti
--          npr. izlet, seminar, ekspedicija, vježba.
--          sov_trips ima check constraint koji prima kanonske vrijednosti:
--          Izlet, Seminar, Skup, Ekspedicija, Inventura, Skupština, Predavanje.
-- Rješenje: prije svakog INSERT/UPDATE u sov_trips normalizirati kategoriju.
-- Sigurno za ponovljeno pokretanje.

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

create or replace function public.sov_trips_normalize_category_trigger()
returns trigger
language plpgsql
as $$
begin
  new.trip_category := public.sov_normalize_trip_category(new.trip_category);
  new.meta := coalesce(new.meta, '{}'::jsonb) || jsonb_build_object('trip_category', new.trip_category);
  return new;
end;
$$;

drop trigger if exists trg_sov_trips_normalize_category on public.sov_trips;
create trigger trg_sov_trips_normalize_category
before insert or update of trip_category, meta on public.sov_trips
for each row
execute function public.sov_trips_normalize_category_trigger();

update public.sov_trips
set trip_category = public.sov_normalize_trip_category(trip_category),
    meta = coalesce(meta, '{}'::jsonb) || jsonb_build_object('trip_category', public.sov_normalize_trip_category(trip_category))
where trip_category is null
   or trip_category <> public.sov_normalize_trip_category(trip_category)
   or coalesce(meta->>'trip_category','') <> public.sov_normalize_trip_category(trip_category);

commit;

-- Brza provjera:
-- select raw, public.sov_normalize_trip_category(raw) as normalized
-- from (values ('izlet'),('seminar'),('ekspedicija'),('vježba'),('Skupština')) as v(raw);
