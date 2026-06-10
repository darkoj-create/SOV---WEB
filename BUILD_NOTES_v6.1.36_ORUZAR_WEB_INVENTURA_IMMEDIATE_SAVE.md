# SOV Web v6.1.36 — Oružar inventura immediate save

Baseline: sov-web-build-v6.1.35-oruzar-inventura-search.zip

## Changed
- oruzar-master-inventura.html: cache-bust dignut na v6.1.36 za oružar master JS/CSS.
- assets/oruzar-master-clean.js: inventura stranica sada koristi posebne inventurne kartice umjesto inventar edit kartica.
- assets/oruzar-master-clean.js: svaka inventurna kartica ima polje "stvarno" i gumb "Spremi u bazu".
- assets/oruzar-master-clean.js: klik na "Spremi u bazu" odmah poziva postojeći SOVArmoryDB.upsertSimpleItem() i ažurira quantity/available/last_inventory_date.
- assets/oruzar-master-clean.js: prikazuje razliku OK / višak / fali prije spremanja.
- assets/oruzar-master-clean.css: dodan compact responsive layout za inventurne count kartice.

## Not changed
- Nema SQL promjena.
- Nema APK promjena.
- Nema promjena u Supabase schema/RPC.
- Nema promjena u delete/edit/export logici inventara.
- Nema promjena u posudbama, izletima, tracking/broadcast logici.

## Notes
- Ovo je web paralela APK fixu v1.4.22: unos stvarnog broja u inventuri više nije samo lokalni UI state, nego odmah pokušava spremiti u bazu.
- Ako Supabase veza ili prava ne rade, korisnik dobiva jasnu poruku da spremanje nije uspjelo.
