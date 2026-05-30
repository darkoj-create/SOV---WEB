# SOV web build v5.58.24

Foundation refactor build za SOV Cloud web.

- Shared shell je dignut na `sov-shell-v55824` i pokriva app/admin/module header, mobile drawer, active state, role-aware linkove i skip link.
- Dashboard preview je Webmaster-only; Admin nema SQL/sync/tech ulaze.
- Karta koristi Arhivar paged full feed i full object detail, bez user-facing source/debug teksta.
- Arhivar, Oružarstvo i News CMS dobili su dodatni mobile/touch i user-facing cleanup.
- Početna stranica ima premium mobile hamburger, Facebook link u headeru i jači hero/news polish.
- Mobile hamburger na početnoj ima direktan click handler i otvara/zatvara drawer bez oslanjanja na legacy nav scroll.
- SQL alati su Webmaster-only i imaju produkcijski banner + dvostruku potvrdu.
- Novi SQL nije potreban za ovaj build.

# SOV web build v5.58.20

Patch na v5.58.19. Glavna promjena: `karta.html` sada pri kliku na objekt/Detalji povlači puni objekt detail iz Arhivar RPC-a i prikazuje ga kao normalan korisnički opis, ne samo kratka map kartica.

SQL nije potreban ako je već pokrenut Arhivar workflow SQL koji sadrži `sov_arhivar_get_object_detail(text)`.

SOV web build v5.58.16

- Oružarstvo: popravljen ulaz u Oružarski dio za Webmaster/Admin/Oružar.
- Oružarstvo: dodatni clean/mobile browser polish.
- Oružar master stranice: maknut inbox artefakt i version/dev tekstovi.
- sync-status.html usklađen na v5.58.16.

SOV web build v5.14 — promotion schema cache hardfix

Pokreni jednom: SUPABASE_SPELEO_BAZA_PROMOTION_SCHEMA_CACHE_FIX_v5_14.sql

Fix: kreira/obnavlja speleo_sql_promotion_batches, speleo_sql_promotion_audit i speleo_objects_live_sql te forsira Supabase schema cache reload. Live JSON baza se ne dira.

# SOV WEB v1.0 recovered full

Full site build: javni portal + puna povijest + Velebitaški duh + SOV Cloud + SOV Karta.

Prelijepi sve u root GitHub repozitorija i commit/push. Vercel povuče automatski.

Baza: data/sov-baza.json
SOV Karta: baza.html
