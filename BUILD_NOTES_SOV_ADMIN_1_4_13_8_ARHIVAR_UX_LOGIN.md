# SOV Admin 1.4.13.8 — Arhivar UX + Home login

## Promjene

- Arhivar ekran je očišćen za praktičniji rad na mobitelu.
- Lista objekata je primarni ekran; detalji više ne guraju listu prema dolje.
- Klik na objekt otvara scrollable bottom sheet s punim opisom, statusima i checklistom.
- Uklonjen je developer/SQL objašnjavajući tekst iz detail prikaza.
- Prikazuju se svi filtrirani objekti, bez starog umjetnog limita od 120 redova.
- Home screen ima malu login/role ikonicu gore lijevo; vodi na Settings gdje je postojeći SOV login + permission sync.

## SQL

Nije potreban novi SQL. Koristi postojeće Arhivar RPC-eve i status save flow.

## Build

- versionCode: 900054
- versionName: 1.4.13.8-arhivar-ux-login
- APK name: SOV-ADMIN-1.4.13.8.apk
