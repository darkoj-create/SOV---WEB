# SOV Web v6.1.45ae — Oružarstvo microcopy + sigurnosne potvrde

Scope: `oruzar-master.html`, `oruzar-master-posudbe.html`, `oruzar-master-inventar.html`, `oruzar-master-inventura.html`, `assets/oruzar-master-clean.js`, `assets/oruzar-ux-microcopy-v6145ae.css`.

Bez promjene poslovne logike, API/RPC poziva, JS funkcija, ID-eva ili strukture baze.

## Prije → poslije

- `assets/oruzar-master-clean.js`, `dbLoadingHtml()`:
  - `DB gate v6.1.6`, `bez cachea · bez statike` → uklonjeno iz vidljivog UI-ja
  - `Čekam da se oružarstvo napuni iz baze…` → `Učitavam podatke, samo trenutak…`
  - `Inventar... Supabase... XLS canonical... JSON/cache...` → `Popis opreme će se prikazati čim se podaci učitaju.`

- `assets/oruzar-master-clean.js`, `openLegacyReturn()`:
  - `Upis ide kroz RPC legacy_return...` → `Za opremu koja se vratila, a nije bila uredno zadužena.`
  - `Spremi legacy_return` → `Spremi`
  - Funkcije i RPC pozivi nisu preimenovani.

- `assets/oruzar-master-clean.js`, `requestCard()`:
  - `Označi izdano` → `Izdaj opremu`
  - `Odbij / zatvori` → `Otkaži zahtjev`
  - `Povrat po artiklu` → `Vrati opremu`

- `assets/oruzar-master-clean.js`, `issueLoan()`:
  - prije izdavanja dodan confirm: `Izdati ovu opremu korisniku [ime]?`
  - success poruka: `Oprema izdana.`
  - Undo za izdavanje nije automatski dodan jer izdavanje može stvoriti povezanu posudbu u bazi; u kodu je komentar gdje treba dodati backend/API-safe undo.

- `assets/oruzar-master-clean.js`, `setStatus()`:
  - prije otkazivanja dodan confirm: `Otkazati zahtjev korisnika [ime]?`
  - nakon otkazivanja dodan toast `Zahtjev je otkazan. [Poništi]` koji vraća prethodni status kroz postojeći `setStatus` flow.

- `assets/oruzar-master-clean.js`, `itemPills()`:
  - dugi popisi stavki sada se sažimaju u `<details>` blok.
  - primjer: `Osobni SRT komplet (20 stavki) · Prikaži stavke`.

- `assets/oruzar-ux-microcopy-v6145ae.css`:
  - dekorativne ikone u oružarskim karticama skrivene.
  - dodan stil za sažete stavke i toast akciju `Poništi`.

- `oruzar-master*.html`:
  - maknute dekorativne emoji oznake iz headera/gumba.
  - `Oružar Master` → `Oružarstvo` u vidljivom tekstu.
  - tehnički izrazi u opisima zamijenjeni običnim jezikom.
