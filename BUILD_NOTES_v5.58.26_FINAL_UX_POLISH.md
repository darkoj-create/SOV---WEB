# SOV Web v5.58.26 — final UX polish

Baseline: `sov-web-build-v5.58.25-codex-review-public-hamburger-fix.zip`.

## Cilj
Još jedan sigurni polish sloj za naslovnicu, javne stranice, dashboard i mobile browser UX bez promjene Supabase sheme i bez diranja poslovne logike.

## Promjene

- Dodan globalni polish sloj:
  - `assets/sov-polish-v55826.css`
  - `assets/sov-polish-v55826.js`
- Svi aktivni HTML-ovi dobili su novi polish CSS/JS include.
- Naslovnica je vizualno dotjerana:
  - bolji hero overlay i čitljivija poruka,
  - dodani brzi CTA gumbi: SOV Cloud, Speleoškola, Sve vijesti,
  - bolji expedition banner,
  - premium kartice vijesti s jasnijim hover/touch stanjem,
  - čišći footer bez starog `Moderni statički portal · v...` teksta.
- Public mobile hamburger dodatno je ojačan:
  - ako header iz nekog razloga nema hamburger, polish JS ga dodaje,
  - drawer state zaključava body scroll dok je otvoren.
- Dashboard i app kartice dobile su završni premium spacing/shadow/radius polish.
- Mobile UX:
  - hero i kartice bolje sjedaju na male ekrane,
  - CTA gumbi prelaze u jedan stupac,
  - role preview na dashboardu ostaje kompaktan,
  - dodan back-to-top gumb.
- Uklonjeni su javni dev/legacy tekstovi tipa `Moderni statički portal` i `Open Wednesday`.
- `sync-status.html`, `BUILD_VERSION.txt`, `VERSION.txt` i changelog dignuti su na v5.58.26.

## SQL
Nema novog SQL-a.

## APK
Nema APK promjena.

## Test checklist

1. Mobile homepage: hamburger se otvara i zatvara.
2. Mobile homepage: hero CTA gumbi su vidljivi i ne pucaju iz ekrana.
3. Public footer više ne pokazuje stari version/dev tekst.
4. Dashboard mobile: kartice su touch-friendly, role preview ne prekriva sadržaj kritično.
5. Karta/Arhivar/Oružarstvo: postojeći flow se ne mijenja jer je ovo samo CSS/JS polish.
