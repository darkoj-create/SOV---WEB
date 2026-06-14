# SOV Web v6.1.39e — Oružar XLS 3 taba

Bazirano na v6.1.39d.

## Promjena

Excel export iz **Inventar** i **Inventura** sada ima tri taba:

1. `Aktualna baza` — live Supabase stanje.
2. `Stara baza` — odabrani/stabilni snapshot stare baze.
3. `Kombinirano` — oba seta zajedno, s dodatnom kolonom `Baza` (`Aktualna baza` / `Stara baza`).

Tab `Kombinirano` sortira redove po najvećoj količini prema dolje, zatim po kategoriji/podkategoriji/nazivu.

## Nije dirano

- SQL
- artikli
- količine
- lokacije
- posudbe
- snapshot podaci

## Datoteke

- `assets/oruzar-master-clean.js`
- `oruzar-master-inventar.html`
- `oruzar-master-inventura.html`
