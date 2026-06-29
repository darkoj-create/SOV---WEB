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
