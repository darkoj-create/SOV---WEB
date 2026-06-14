# SOV Web v6.1.39k — Oružar XLS: tab po snapshotu + kombinirano

Baseline: v6.1.39j (real XLS workbook + user loan packages).

## Problem
Export je bio zakovan na 2 izvora (live + jedan "stari" snapshot), pa ma koliko
snapshota postojalo, dobila bi se uvijek samo 3 taba. Kombinirana kolona je znala
samo "Aktualna baza"/"Stara baza".

## Promjena (samo frontend)
- `loadExportPair` (par) -> `loadExportSnapshots`: učita live I sve snapshote,
  sekvencijalno (activeExportRowsFromData privremeno mijenja STATE.data, pa bi
  paralelno učitavanje zatrovalo redove).
- Inventar i Inventura sada generiraju:
  1. `Aktualna baza` (live)
  2. jedan tab po SVAKOM snapshotu (ime taba = ime snapshota)
  3. `Kombinirano` — svi izvori, s kolonom `Baza / snapshot`
- Kolona `Baza` -> `Baza / snapshot`; vrijednost = ime snapshota + datum
  (npr. `Stara baza (2026-06-09)`), tako da se u merged viewu vidi izvor svakog retka.
- `xlsWorkbook` sada DEDUPLICIRA imena tabova (Excel traži jedinstvena imena <=31 znak).
  Bez toga bi dva snapshota s default datumskim imenom srušila cijeli file.
- Prazni snapshoti (RPC vrati 0 stavki) tiho se preskaču umjesto praznog taba.

## Nije dirano
- SQL / RPC (koristi postojeći sov_armory_get_catalog_snapshot iz 39g)
- artikli, količine, lokacije, posudbe, auth, kategorije.
- Kombinirano i dalje sortira po najvećoj količini (kao 39e).

## Datoteke
- `assets/oruzar-master-clean.js`
- `oruzar-master-inventar.html` (cache-bust + gumb "Excel: tab po snapshotu")
- `oruzar-master-inventura.html` (isto)

## Deploy
Deploy full ZIP, hard refresh. Cache-bust: `6.1.39k-xls-per-snapshot`.
