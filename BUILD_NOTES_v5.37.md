# SOV Web v5.37 — Arhiva/Nacrti canonical

## Što je novo
- `topodroid.html` preimenovan i pozicioniran kao canonical Arhiva/Nacrti modul.
- Članovi imaju read-only pregled public nacrta.
- Admin/Arhivar mogu spremati metadata nacrta u Supabase.
- Dodan backend prikaz iz `sov_drawings_public` viewa.
- Dodan safe SQL: `SUPABASE_SPELEO_NACRTI_ARHIVA_v5_37_CANONICAL_ROLE_AUDIT.sql`.

## SQL
Pokreni samo novi v5.37 SQL.
RLS ostaje uključen.
Ne dira SQL bazu objekata.

## Napomena
Lokalni upload datoteka i dalje ostaje browser-local za preview; u Supabase se sprema metadata/linkovi. Pravi file upload/Drive pipeline ide u idućem koraku.
