# v5.15 — SQL Go Live consolidated

Što radi:
- Baza i Pregled baze prvo čitaju `speleo_objects_live_sql`.
- Ako SQL live nema redova, koristi `speleo_objects_staging` kao SQL fallback.
- Ako Supabase ne radi, tek tada pada na lokalni JSON.
- Dodan `speleo-sql-go-live.html` za promociju svih staging objekata u SQL live.
- Svi SQL-ovi spojeni u jedan: `SUPABASE_SPELEO_BAZA_CONSOLIDATED_GO_LIVE_v5_15.sql`.

Koraci:
1. Deploy ZIP.
2. Pokreni SQL jednom.
3. Otvori `/speleo-sql-go-live.html`.
4. Klikni `Promoviraj SVE staging → SQL live`.
5. Otvori `/baza.html` i `/pregled-baze.html`.
