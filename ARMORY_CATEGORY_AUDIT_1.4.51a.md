# SOV v1.4.51a — Oružarstvo category audit

Usporedba Android offline asseta `app/src/main/assets/oruzarstvo-xls-canonical-v6.1.5.json` s pravilima iz `SUPABASE_ORUZARSTVO_CATEGORY_PRIORITY_ALIGN_v6_1_45ai.sql`.

## Rezultat

Razlika u redoslijedu/prioritetima **nije pronađena**. Asset već nosi iste canonical kategorije i iste priority vrijednosti koje koristi web/Supabase v6.1.45ai.

| Priority | SQL canonical kategorija | Asset kategorija | Broj stavki u assetu | Status |
|---:|---|---|---:|---|
| 10 | Osobni SRT komplet / Osobna oprema | Osobni SRT komplet | 20 | OK |
| 20 | Sidrišta i opremanje | Sidrišta i opremanje | 50 | OK |
| 30 | Tehničko spašavanje i Čisto podzemlje | Tehničko spašavanje i Čisto podzemlje | 19 | OK |
| 40 | Mjerenje, crtanje i dokumentacija | Mjerenje, crtanje i dokumentacija | 20 | OK |
| 50 | Proširivanje i regulirana oprema | Proširivanje i regulirana oprema | 41 | OK |
| 60 | Rasvjeta, elektronika i komunikacija | Rasvjeta, elektronika i komunikacija | 43 | OK |
| 70 | Alpinistička i penjačka oprema | Alpinistička i penjačka oprema | 19 | OK |
| 80 | Ronilačka oprema | Ronilačka oprema | 8 | OK |
| 90 | Alat i održavanje | Alat i održavanje | 40 | OK |
| 100 | Užad | Užad | 35 | OK |
| 110 | Logor, ekspedicija i kuhinja | Logor, ekspedicija i kuhinja | 186 | OK |
| 120 | Medicinska oprema | Medicinska oprema | 65 | OK |

## Napomena

`EquipmentSupabaseRepository.kt` već mapira XLS/web alias `Osobni SRT komplet` u prikazni naziv `Osobna oprema`, te ima canonical mapu za iste nazive kategorija. Zato u ovom buildu nije generiran novi JSON i nije mijenjana sync logika.
