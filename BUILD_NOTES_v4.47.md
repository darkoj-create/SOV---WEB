# SOV web build v4.47 — Simplified Armory Model

## Što se mijenja
- Samo užad ima individualni kod/SKU.
- Sva ostala oprema vodi se kao količinski artikl po vrsti/modelu.
- Guest/član vidi samo kategorije, podkategorije i artikle za zahtjev.
- Oružar vidi detalje: ukupno, dostupno, posuđeno, lokacija, pragovi, status.
- Ručni unos više ne gura kodove za svaki komad; kod je stvaran samo za uže.

## SQL
Dodana je ne-destruktivna migracija:
`SUPABASE_ORUZARSTVO_v4_47_SIMPLIFIED_MODEL.sql`

Postojeće tablice ostaju kompatibilne.
