# SOV Web v6.1.36 — Oružarstvo SAVE FIX (+ XLS canonical import keys)

## Problem
"Baza se loada, ali ne pamti izmjene." Katalog se prikazivao, ali ručni edit artikla
nije ostajao nakon reloada.

## Root cause (NIJE bio cache ni RLS)
`oruzar-master-clean.js → saveItem()` je za POSTOJEĆI artikl slao izmjenu kroz
`SOVArmoryDB.updateInventoryCount()`, koji upisuje SAMO: available, quantity_label,
available_label, status, last_inventory_date, internal_note. Sva ostala polja iz forme
(quantity / naziv / kategorija / podkategorija / lokacija / minimum) su se tiho bacala.
Audit log to potvrđuje: editi su mijenjali samo labele/datum/notu, `available` 30→30.

Dodatno: stavke importirane iz XLS-a imale su `legacy_id` i `catalog_id` = NULL, pa ih
druge funkcije (retire, loan linking, upsert) nisu mogle adresirati.

## Fix
1. `oruzarstvo-supabase.js`: nova `updateEquipmentItemFull(keys, fields)` — upisuje PUNI
   set polja u `equipment_items`, ključ `legacy_id.eq | catalog_id.eq | id.eq` (radi i kad
   je legacy_id NULL), i busta lokalni katalog cache nakon spremanja.
2. `oruzar-master-clean.js → saveItem()`: postojeći artikl sada ide kroz
   `updateEquipmentItemFull` (sva polja), a ne više kroz uski `updateInventoryCount`.
   Inventura quick-count (čisti popis "Dostupno") i dalje smije koristiti updateInventoryCount.
3. Cache-bust `?v=6.1.36-save-fix` na oruzarstvo-supabase.js i oruzar-master-clean.js u svim
   HTML-ovima (oruzarstvo, oruzar-master, -inventar, -inventura, -posudbe, -notes, -import).

## DB (već primijenjeno na ncomefzkuixyfixisrhi)
- Import 556 stavki iz `SOV_oruzarstvo_jednostavna_popunjena_evidencija.xlsx` u
  `equipment_items` (source_sheet='xls_jednostavna_evidencija_2026'), 12 kuriranih kategorija,
  sve lokacije mapirane. Vidi SUPABASE_ORUZARSTVO_v6_1_36_IMPORT_AND_SAVE_FIX.sql.
- Backfill: legacy_id = catalog_id = id::text za importirane stavke (bile NULL).
- Manifest se sam ažurira preko trigera (equipment_items_count=556).

## Napomena
`equipment_categories` i dalje sadrži ~49 starih kategorija s duplikatima ("Užad"/"Užeta",
"Alpinistička oprema"/"...penjačka..."). Sada ih ništa ne koristi (artikli su na 12 čistih).
Opcionalni cleanup zaseban.
