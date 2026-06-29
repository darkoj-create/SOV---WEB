# v4.24 — Oružarstvo Supabase SQL backend

Dodano:
- `SUPABASE_ORUZARSTVO_SETUP.sql` za pravi backend oružarstva.
- Role-based RLS za `admin`, `oruzar`, `user`.
- `assets/oruzarstvo-supabase.js` bridge za zahtjeve opreme i import početnog modela.
- `oruzarstvo.html` sada koristi Supabase za zahtjeve ako je konfiguriran, a lokalni JSON ostaje fallback.
- `oruzarstvo-import.html` za admin/oružar import iz `data/oruzarstvo-data.json` u Supabase.

Workflow:
1. Pokrenuti bazni `SUPABASE_SETUP.md` SQL za auth/profile.
2. Pokrenuti `SUPABASE_ORUZARSTVO_SETUP.sql`.
3. Upisati Supabase URL i anon key u `assets/supabase-config.js`.
4. Otvoriti `oruzarstvo-import.html` kao admin/oružar i importati početni katalog.
5. Oružarstvo zahtjevi članova dalje idu u Supabase.
