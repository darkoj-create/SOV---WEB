-- SOV Oružarstvo v4.73 — safe integer quantity cleanup
-- Pokrenuti jednom u Supabase SQL editoru.
-- Cilj: ukloniti decimalne/krive količine iz brojača bez brisanja artikala.
-- Radi backup prije izmjena. Ne dira kategorije, nazive, lokacije ni posudbe osim quantity polja.

create table if not exists public.equipment_items_quantity_backup_v4_73 as
select * from public.equipment_items;

create table if not exists public.equipment_request_items_quantity_backup_v4_73 as
select * from public.equipment_request_items;

create table if not exists public.equipment_loans_quantity_backup_v4_73 as
select * from public.equipment_loans;

-- Helper: očisti numeričke kolone samo ako postoje i ako su decimalne/negativne.
do $$
declare
  r record;
  tables text[] := array['equipment_items','equipment_request_items','equipment_loans'];
  cols text[] := array['quantity','available','loaned','minimum'];
  t text;
  c text;
begin
  foreach t in array tables loop
    foreach c in array cols loop
      select table_name, column_name, data_type
      into r
      from information_schema.columns
      where table_schema='public'
        and table_name=t
        and column_name=c
        and data_type in ('smallint','integer','bigint','numeric','real','double precision');

      if found then
        execute format(
          'update public.%I set %I = greatest(0, trunc(%I::numeric)) where %I is not null and (%I::numeric < 0 or %I::numeric <> trunc(%I::numeric))',
          t, c, c, c, c, c, c
        );
      end if;
    end loop;
  end loop;
end $$;

-- Ako postoje label polja, ostavi ih kao tekstualni trag, ali ne koriste se za brojače.
-- Finalna provjera: sve količine koje ulaze u UI moraju biti cijeli brojevi.
select
  'equipment_items' as table_name,
  count(*) filter (where quantity is not null and quantity::numeric <> trunc(quantity::numeric)) as decimal_quantity,
  count(*) filter (where available is not null and available::numeric <> trunc(available::numeric)) as decimal_available,
  count(*) filter (where loaned is not null and loaned::numeric <> trunc(loaned::numeric)) as decimal_loaned
from public.equipment_items;
