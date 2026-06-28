# SOV Web v5.47.4 — Oružar Master visibility hotfix

Problem nakon v5.47 DB konsolidacije:
- Oružar Master je mogao ostati prazan ako live Supabase read layer / RLS / view vrati 0 redova.
- Dio Master HTML stranica još je vukao stari cache parametar `oruzarstvo-supabase.js?v=4.86`, pa je browser mogao koristiti staru logiku.

Fix:
- Sve `oruzar-master*` stranice sada vuku `oruzarstvo-supabase.js?v=5.47.4` i `oruzar-master-clean.js?v=5.47.4`.
- Oružar Master uvijek učita `data/oruzarstvo-data.json` kao sigurnosni fallback.
- Ako Supabase/live katalog vrati 0 ili premalo redova, Master više ne ostaje prazan nego prikaže static fallback katalog.
- Ako live katalog radi i ima dovoljno redova, koristi live podatke.
- Master inventar preferira raw katalog (`raw_app_catalog`) umjesto grouped kataloga, jer oružar mora vidjeti stvarne retke za inventar/inventuru.
- Supabase helper sada koristi `.limit(7000)` kod učitavanja kataloga da ne odreže veliku bazu.

SQL:
- Nema novog SQL-a za ovaj hotfix.
- Nemoj ponovno pokretati v5.47/v5.47.1/v5.47.2.
- Ako je već prošao v5.47.3, ostavi ga.
