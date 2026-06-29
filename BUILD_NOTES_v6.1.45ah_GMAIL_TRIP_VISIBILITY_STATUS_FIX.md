# SOV web 6.1.45ah – Gmail najave visibility/status fix

Datum: 2026-06-29
Baza: `sov-web-build-v6.1.45ag-gmail-trip-category-approval-fix.zip`

## Problem
Odobravanje izleta iz zapisnika povučenih s Gmaila moglo je pasti na:

`new row for relation "sov_trips" violates check constraint "sov_trips_visibility_check"`

Nakon prethodnog popravka `trip_category`, isti flow je još mogao poslati `visibility='members'`, dok `sov_trips` prima `private`, `club`, `public`.

## Rješenje
- Dodan SQL helper `public.sov_normalize_trip_visibility(text)`.
- Dodan SQL helper `public.sov_normalize_trip_status(text)`.
- Postojeći trigger `trg_sov_trips_normalize_category` proširen je da normalizira:
  - `trip_category`
  - `visibility`
  - `status`
  - pripadajuće `meta` vrijednosti
- Web helper `assets/sov-trips-cloud.js` sada normalizira `visibility` i `status` prije RPC/direct save pokušaja.
- `assets/zapisnici-najave-v6143a.js` označen je novim parser tragom `6145ah`.

## SQL
SQL je uključen u:

`SUPABASE_SOV_GMAIL_TRIP_VISIBILITY_STATUS_FIX_v6_1_45ah.sql`

SQL je već primijenjen na live Supabase projekt `ncomefzkuixyfixisrhi`.

## Važno
Ovo ne mijenja poslovnu logiku ni dopuštene vrijednosti u tablici. Samo prevodi stare/vanjske nazive u postojeće kanonske vrijednosti.
