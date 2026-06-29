# sov-web-build-v6.1.45ah-gmail-trip-visibility-status-fix

- Fix za odobravanje Gmail/zapisnik najava u Izlete: `visibility='members'` sada se normalizira u `club` prije upisa u `sov_trips`.
- Dodana je i normalizacija `status` vrijednosti da se izbjegne sljedeći isti tip check-constraint greške.
- SQL je uključen u `SUPABASE_SOV_GMAIL_TRIP_VISIBILITY_STATUS_FIX_v6_1_45ah.sql` i već je primijenjen na live Supabase.

# sov-web-build-v6.1.45ag-gmail-trip-category-approval-fix

- Fix: odobravanje izleta iz Gmail/native zapisnika više ne puca na `sov_trips_trip_category_check`.
- Supabase SQL dodaje normalizaciju kategorije prije upisa u `sov_trips`.
- Frontend zapisnika i cloud izleta sada šalje kanonske kategorije.
- SQL patch: `SUPABASE_SOV_GMAIL_TRIP_CATEGORY_APPROVAL_FIX_v6_1_45ag.sql` (već primijenjen na SOV-web Supabase).

# SOV web 6.1.45af — Oružarstvo dashboard icons + safe undo

Datum: 2026-06-29
Baza: `sov-web-build-v6.1.45ae-oruzarstvo-microcopy-safety.zip`
SQL: nije potreban

## Promjene

1. Release hygiene
   - Usklađeni `VERSION.txt`, `BUILD_VERSION.txt`, `update.json` i fallback u `assets/sov-version.js`.
   - Novi cache bust: `6145af-oruzarstvo-dashboard-icons-safe-undo`.

2. Oružarstvo main dashboard
   - Kartice imaju jasne ikone i pomoćni tekst: Posudbe, Povrat stare opreme, Inventar, Inventura, Bilješke.
   - Brand oznaka oružarskog prostora više nije prazna/SOV fallback, nego jasna ikona opreme.

3. Manje tehničkog teksta u sučelju
   - Članovska oprema ne prikazuje Supabase/build/pending poruke korisniku.
   - Dashboard radni alati imaju normalnije opise bez nepotrebnog backend/API/sync žargona.
   - Oružarski uvoz više ne izbacuje sirovi JSON u vidljivi log.

4. Članovski flow opreme
   - Statusi zahtjeva su prevedeni u ljudski tekst.
   - Oružarski link se prikazuje samo korisnicima koji imaju oružarsku/admin ovlast.
   - Quick paketi imaju smislenije ponašanje i ne završavaju na “paket nije definiran”.
   - Posudbe koriste isti siguran flow izdavanja/povrata i kad je uključen loan-shop prikaz.

5. Sigurnije poništavanje izdavanja
   - Nakon “Izdaj opremu” toast nudi “Poništi”.
   - Poništavanje vraća zahtjev u čekanje, pokušava vratiti količine u Oružarstvo i zatvoriti materializiranu posudbu ako postoji.
   - Ako online dio ne uspije, UI jasno javi da treba ručno provjeriti stanje, bez glumljenja da je sve sigurno vraćeno.

## Namjerno nije mijenjano

- Nema promjene strukture baze.
- Nema novih SQL migracija.
- Nema promjene postojećih API/RPC naziva.

---

## v5.45 — Armory Core Cleanup

- Kategorizacija: client više ne pokušava biti canonical mozak; web čita SQL/grouped view polja i koristi samo minimalni fallback.
- Statusi posudbi: uklonjen dead-end approval model; standard je pending/requested → issued → returned/partial_return (+ cancelled).
- Zahtjevi: kompatibilnost za multi-item request lines (`name` + `item_name`).

# v4.8 Main nav + brand cleanup

- Sređen gornji lijevi SOV logo da bolje stane u ovalni/zaobljeni okvir.
- Pročelništvo, Povijest i Velebitaški duh prebačeni pod glavni izbornik “O nama”.
- Glavni ekran više nema zasebne top-level linkove Pročelništvo i Povijest.
- Mobile nav dodatno podešen da se linkovi ne lome ružno.

## v4.10
- Polished main navigation and logo containment.
- Pročelništvo and Povijest integrated under O nama navigation.
- Rebuilt O nama page with internal sections/tabs and cleaner card logic.


v4.11: Polished O nama UX, removed floating duplicate internal menu behavior, improved logo fit and responsive nav.

## v5.44 — Final Equipment Grouping Rules
- Added canonical grouped equipment catalog source for web UI.
- Included `SUPABASE_ORUZARSTVO_APP_CATALOG_v5_44_FINAL_GROUPING_RULES.sql`.
