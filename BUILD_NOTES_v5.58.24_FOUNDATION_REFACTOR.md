# SOV Web v5.58.24 — Foundation refactor stabilization

## Promjene

- `sov-shell-v55824.css/js` nastavlja shared header/shell: role-aware drawer, active state, skip-link, focus ring i 44px touch targeti.
- Dashboard role preview je sada samo Webmaster-only; Admin ostaje bez tech/SQL/sync alata.
- SQL/sync/audit/role-manager ulazi su Webmaster-only, a SQL alati zadržavaju PRODUKCIJA banner i dvostruku potvrdu.
- `karta.html` ostaje na Arhivar paged full feedu, ne limitira feed na 1000 objekata i ne prikazuje `Izvor:` korisniku.
- Arhivar i predane jame imaju dodatne mobile kartice/touch polish; full edit polja ostaju prisutna.
- News editor i članska predaja članka više ne prikazuju SQL/dev upute u normalnom UI-u.
- `index.html` dobio je premium mobile home polish: hamburger drawer, top-right Facebook link, jači hero UX i mobilne touch/layout dorade.
- `sync-status.html`, `CHANGELOG.md`, `README.md`, `VERSION.txt` i `BUILD_VERSION.txt` dignuti su na v5.58.24.

## SQL

Nema novog SQL-a.

## APK

Nema APK promjena.
