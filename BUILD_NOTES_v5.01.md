# SOV web build v5.01 — Speleo Baza SQL source

Što je dodano:
- `speleo_objects` SQL schema za pravu Speleo Bazu.
- `speleo_object_aliases` i `speleo_object_import_log`.
- JSON `data/baza_velebit_2026_appready.json` u buildu kao app-ready import/fallback.
- `assets/speleo-sql-loader.js` — prvo pokušava čitati Supabase SQL, a ako nema podataka fallbacka na JSON.
- `baza.html` i `pregled-baze.html` sada koriste SQL kao glavni izvor kad je tablica popunjena.
- Dodan gumb `JSON → SQL` u Baza toolbaru za jednokratni import.

Kako koristiti:
1. Pokreni `SUPABASE_SPELEO_BAZA_SQL_v5_01.sql` u Supabase SQL editoru.
2. Deployaj build.
3. U Baza sekciji klikni `JSON → SQL`.
4. Nakon importa stranica se refresha i treba pokazivati izvor `Supabase SQL`.

Napomena:
- `data/sov-baza.json` ostaje legacy fallback.
- Nacrti, zahvati i arhivar editovi sada imaju stabilniji temelj jer se mogu vezati na `speleo_objects.id`.
