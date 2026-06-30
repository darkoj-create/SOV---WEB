# SOV web build v6.1.45ak
## v6.1.45ak — Karta refresh loop hard fix

- `Karta.html` više nije redirect-wrapper i više nema `<meta refresh>`.
- `Karta.html` sada je ista funkcionalna karta kao `karta.html`, pa stari linkovi ne mogu ući u petlju.
- Dodani su kompatibilni aliasi `KARTA.HTM`, `KARTA.HTML`, `Karta.htm` i `karta.htm` bez redirecta.
- Nema SQL promjena i nema promjene poslovne logike karte.

Build: `sov-web-build-v6.1.45aj-trips-display-month-calendar-fix`

## Što je novo

- Popravljen je prikaz izleta odobrenih iz Gmail/native zapisnika.
- Stranica `izleti-cloud.html` više ne ostaje prazna na tekućem mjesecu ako je prvi budući izlet u idućem mjesecu.
- Stranica `kalendar-izleta.html` sada se nakon učitavanja automatski prebaci na mjesec prvog budućeg izleta/događaja ako u trenutnom mjesecu nema stavki.
- Osvježavanje rasporeda vraća automatski odabir najbližeg relevantnog mjeseca.
- Nema SQL promjena, nema promjene RPC/API ugovora i nema promjene poslovne logike odobravanja.
