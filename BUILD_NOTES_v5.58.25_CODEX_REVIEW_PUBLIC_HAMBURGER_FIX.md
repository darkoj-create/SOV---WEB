# SOV Web v5.58.25 — Codex review + public hamburger fix

## Nalaz

Codex v5.58.24 je donio dobar foundation refactor, ali glavni javni ekran (`index.html`) nije stvarno učitavao `sov-shell-v55824.css/js`. Uz to je `sov-shell-v55824.js` bio ograničen samo na Cloud/app stranice, pa javni homepage nije mogao dobiti funkcionalan drawer/hamburger.

## Fix

- Dodan `assets/sov-shell-v55825.css`.
- Dodan `assets/sov-shell-v55825.js`.
- `index.html` i ostale javne stranice sada učitavaju shell CSS/JS.
- Shell sada razlikuje app stranice i public stranice.
- Public drawer ima linkove: Novosti, Sve vijesti, O nama, Speleoškola, Pridruži nam se, SOV Cloud.
- Hamburger click koristi delegated event handler, pa radi i ako header/gumb naknadno pomakne drugi script.
- App/Cloud stranice zadržavaju role-aware drawer.
- `sync-status.html`, `CHANGELOG.md`, `README.md`, `VERSION.txt` i `BUILD_VERSION.txt` dignuti su na 5.58.25.

## SQL

Nema novog SQL-a.

## APK

Nema APK promjena.

## Dodatna mobile provjera

Public homepage ima strukturu `header.topbar > nav.nav > brand + navlinks`. Zato v5.58.25 CSS eksplicitno ne skriva cijeli `nav.nav` na public/mobile, nego skriva samo `.navlinks`, da logo/brand ostane vidljiv uz hamburger.
