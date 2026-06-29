# SOV web v6.0.8 — Public site polish

Base: v6.0.7 dashboard clean mode.

## Cilj
Javni dio weba više ne smije izgledati kao dev build, nego kao završena moderna stranica društva.

## Promjene
- Dodan `assets/sov-public-polish-v608.css` kao jedinstveni polish sloj za javne stranice.
- Usklađeni javni pagevi: `index.html`, `o-drustvu.html`, `procelnistvo.html`, `velebiten.html`, `vijesti.html`, `vijest.html`, `pridruzi-nam-se.html`, `speleoskola.html`, `videos.html`.
- O nama stranice dobile horizontalni public tabs navigacijski sloj.
- `procelnistvo.html` dobio pretragu, year filter i decade filter bez mijenjanja XLS-derived podataka.
- `update.json`, `VERSION.txt`, `BUILD_VERSION.txt`, `sync-status.html` usklađeni na v6.0.8.
- Oružarstvo single boot i system health ostaju iz prethodnih buildova; nema SQL-a.

## Rollback
Ako nešto pukne u javnom UI-u, vrati v6.0.7 ZIP. Baza nije dirana.
