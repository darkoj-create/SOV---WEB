# SOV Web v6.1.7 — Cloud Documents Hub

## Što je dodano
- Na `dashboard.html` u glavnom Cloud dijelu dodana je ikonica/kartica `📄 Dokumenti`.
- Nova stranica `dokumenti.html` služi kao dokument centar.
- Stranica je podijeljena na:
  - `Zapisnici sastanaka`
  - `Tutoriali`

## Linkovi na stranici
- Pregled zapisnika → `pregled-zapisnika.html`
- Novi zapisnik → `novi-zapisnik.html`
- Zapisnici skupštine → `zapisnici-skupstine.html`
- SOV Cloud osnove → `dokumentacija.html#upute`
- TopoDroid i nacrti → `topodroid.html`
- Video tutoriali → `videos.html`

## Tehnički
- `dokumenti.html` je registriran u `assets/auth.js` i shell navigaciji.
- Dashboard top nav i role preview poštuju istu vidljivost za sve odobrene role.
- Nema SQL promjena.
- Oružarstvo DB-gate v6.1.6 nije diran.
