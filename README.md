# SOV web build v6.1.45ah

Build: `sov-web-build-v6.1.45ah-gmail-trip-visibility-status-fix`

Ovaj hotfix popravlja odobravanje izleta iz Gmail/native zapisnika u Izlete kada parser ili stariji SQL/RPC flow pošalje stare vrijednosti poput `members` umjesto `club`.

## Što je promijenjeno

- Supabase normalizira `trip_category`, `visibility` i `status` prije upisa u `sov_trips`.
- Web helper `assets/sov-trips-cloud.js` prije spremanja šalje kanonske vrijednosti.
- SQL je uključen u `SUPABASE_SOV_GMAIL_TRIP_VISIBILITY_STATUS_FIX_v6_1_45ah.sql`.

## Deploy

1. Deployaj sadržaj ZIP-a kao statički web build.
2. SQL je već primijenjen na live Supabase projekt `ncomefzkuixyfixisrhi`.
3. Ako se baza ikad resetira, ponovno pokreni SQL iz datoteke u ZIP-u.

