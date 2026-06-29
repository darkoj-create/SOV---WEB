# SOV Web v5.59.8 — User approval sync fix

Problem: korisnik se mogao uspješno registrirati u Supabase Auth, ali se zbog RLS/email-confirm edge-casea nije kreirao `public.profiles` red. Admin UI i Role manager čitali su samo `profiles`, pa novi korisnik nije bio vidljiv za odobravanje.

Popravci:
- `SUPABASE_SOV_USER_APPROVAL_SYNC_v5_59_8.sql` dodaje Auth trigger `sov_auth_user_profile_sync` koji za svakog novog Auth usera kreira pending profil.
- SQL odmah backfilla stare Auth-only korisnike kroz `sov_admin_sync_missing_profiles()`.
- Dodan admin RPC `sov_admin_list_users()` koji spaja `auth.users` + `public.profiles`, pa više nema nevidljivih registracija.
- Dodan admin RPC `sov_admin_update_user_profile()` za approve/reject/role update bez zapinjanja na `profiles` RLS.
- Dodan public RPC `sov_register_pending_profile()` koji frontend zove nakon `auth.signUp`.
- `assets/auth.js` koristi nove RPC-ove i ima fallback na stari direktni `profiles` upis/update.

Nakon deploya weba OBAVEZNO pokrenuti SQL:
`SUPABASE_SOV_USER_APPROVAL_SYNC_v5_59_8.sql`

Očekivani rezultat: korisnik koji se već registrirao, ali ga nisi vidio, pojavit će se u Admin approval / Role manager listi odmah nakon SQL backfilla i refreshanja stranice.
