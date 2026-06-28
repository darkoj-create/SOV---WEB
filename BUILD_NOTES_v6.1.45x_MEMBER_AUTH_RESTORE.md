# SOV web v6.1.45x — Member auth restore

Popravak nakon što je clean URL routing zaobišao dio članskog login/session sustava.

## Ključni fix

- `assets/auth.js` sada pravilno normalizira clean URL rute:
  - `/dashboard` → `dashboard.html`
  - `/oruzarstvo` → `oruzarstvo.html`
  - `/admin-users` → `admin-users.html`
  - `/system-status` → `system-status.html`
  - itd.
- `autoProtect()` ponovno prepoznaje zaštićene stranice i radi redirect na `login.html?next=...` ako nema odobrene Supabase sesije.
- `system-status`, `sov-system-status` i `status` su dodani u webmaster/admin protected flow.
- `login.html`, `assets/supabase-config.js` i postojeći Supabase auth flow nisu mijenjani osim zaštite ruta.

## Nema SQL promjena

Ovo je frontend routing/auth guard fix.
