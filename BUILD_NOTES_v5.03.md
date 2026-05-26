# SOV Web v5.03 — Chunked Speleo SQL Import

Fix za zapinjanje na "Učitavam bazu" nakon JSON → SQL importa.

## Promjene
- JSON → SQL import više ne šalje cijeli JSON u jednoj RPC operaciji.
- Import radi u batchovima od 150 objekata.
- Dodan progress tekst + progress bar.
- Import je idempotentan: može se pokrenuti opet, radi `upsert` po `id`.
- Baza i dalje prvo čita `speleo_objects` iz Supabasea, a JSON koristi samo kao fallback ako SQL nema podataka ili pukne.

## SQL
Nema nove SQL promjene u odnosu na v5.01/v5.02.
