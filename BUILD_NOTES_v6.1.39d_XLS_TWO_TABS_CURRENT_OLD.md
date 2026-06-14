# SOV Web v6.1.39d — XLS export: aktualna + stara baza

Baseline: v6.1.39c.

## Promjena
- Inventar Excel export sada generira točno 2 worksheet taba:
  1. `Aktualna baza` — live stanje iz Supabasea
  2. `Stara baza` — spremljeni snapshot / restore point stare baze
- Inventura Excel export radi isto: 2 taba, aktualna + stara baza.
- Export više ne radi jedan tab po kategoriji, jer korisnik želi usporedbu aktualne i stare baze u istoj datoteci.
- Stari tab se uzima iz selektiranog snapshot-a ako je aktivan snapshot view; inače preferira snapshot `v6.1.39c-initial` / “Stara baza”.

## Nije dirano
- SQL nije mijenjan.
- Artikli, količine, lokacije, posudbe i RLS nisu dirani.

## Datoteke
- `assets/oruzar-master-clean.js`
- cache bust u master/ožurar HTML stranicama na `6.1.39d-xls-two-tabs`.
