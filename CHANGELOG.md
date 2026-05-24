## v3.2 — Dokumenti + Speleo zapisnik link
- Dashboard: **Speleo zapisnik** sada vodi na službeni Google Forms link.
- Sekcija **Zapisnici** preimenovana u **Dokumenti**.
- `dokumentacija.html` podijeljena na: Zapisnici, Upute, Dopuštenja.
- `speleo-zapisnik.html` pretvoren u jednostavnu landing stranicu s gumbom za otvaranje obrasca.


## v3.1 — Kalendar arhiva + kreiranje izleta u Sheet
- Kalendar izleta sada ima eksplicitan pregled prošlih izleta / arhive.
- Gumb Kreiraj izlet vodi na postojeću stranicu `izleti.html`.
- `izleti.html` sada šalje izlet u isti Google Sheet endpoint i istom `addTripV2` logikom kao Android app.
- Dodano polje Grad / regija za prognozu (`weatherCity`) koje se sprema u Sheet i koristi za vremensku prognozu u kalendaru.
- Ako browser/CORS blokira upis, payload se sprema u `localStorage.sov_pending_web_trips` za debug.

# v2.9 — YouTube video integration

- Main page Video section now embeds the SOV YouTube uploads list.
- Added standalone `videos.html` page.
- Added direct links to `https://www.youtube.com/@speleologija/videos` and channel fallback.
- No YouTube API key required for this static build.
- Public news, dashboard and registered modules preserved.


## v2.8 — Gallery + Video on main page
- Main page now includes a cinematic Galerija section.
- Added Video section with embedded Velebitaški duh video and SOV YouTube channel link.
- Added Galerija and Video anchors to top navigation.
- Copied same update to vijesti.html so the news landing stays consistent.

# v2.7 — Dashboard extra actions

- Dashboard: dodana lijeva ikona/menu item **Speleo zapisnik**.
- Dashboard: dodana desna ikona/menu item **Napiši članak**.
- Dodane privremene static stranice `speleo-zapisnik.html` i `napisi-clanak.html` za dev preview.
- Javni news dio i baza nisu dirani.


## v2.6 — Baza alphabetic + SOV app link
- `pregled-baze.html` pretvoren u konkretan abecedni popis objekata.
- Dodan search po nazivu, lokaciji, pločici, katastru i opisu.
- Klik na objekt otvara full info panel.
- Dodan link na SOV Kartu s direktnim otvaranjem odabranog objekta.
- Dodan link na nacrte/dokumentaciju kada je nacrt evidentiran u bazi.
- Dashboard SOV app kartica sada vodi na Google Drive link za aktualni Android build.

# Changelog

## v2.2-news-first-editorial — 2026-05-24
- Landing page je prebačen na Novosti-first logiku.
- Dodano 9 modernih article stranica u `/novosti/`.
- Članci su poredani kronološki od najnovijeg prema starijima.
- Tekstovi članaka nisu mijenjani; preuzet je originalni `.entry-content` iz WordPress HTML exporta.
- Dodan `data/news.json` za budući CMS/Supabase/WordPress-style unos.
- Registered/SOV Cloud dio iz v2.1 ostaje otvoren bez login guarda za dev preview.

# Changelog

## v2.1 — Dev open registered preview
- Registered/SOV Cloud pages are temporarily accessible without login while Supabase is not connected.
- Removed client-side auth redirects from dashboard, SOV Karta, Izleti, Pregled baze, Oružarstvo, Dokumentacija and Admin preview.
- Login/register page remains available as visual flow for later Supabase wiring.
- Added visible DEV PREVIEW badge on registered pages so this is not mistaken for production auth.

## v1.9 — SOV Karta popup + Map download
- Klik na marker otvara full popup pločicu s podacima objekta.
- Klik na search rezultat centrira kartu, otvara isti popup i desni detaljni panel.
- MBTiles wording preimenovan u Map download.
- Download odabranog područja ostaje stvarni .mbtiles generiran iz WMS tileova u browseru.


## v2.3 — Dashboard redesign
- Registered dashboard redizajniran kao premium SOV Cloud cockpit.
- Dodan veliki cinematic hero, operativni panel, status kartice, glavni moduli, mini-map preview, quick actions i razvojni status.
- Javni news-first landing, novosti i registered open preview nisu dirani.


## v2.4 — dodatne vijesti
- Dodane nove vijesti: Zec il žaba, Duman, Žumberački odred za čistoću, Fugro/QGIS/Orux.
- Regeneriran kronološki news feed i /novosti article stranice.
- Poboljšan fallback za naslovne slike: og:image → prva velika slika iz članka → default SOV hero.
- Javni i registered/dashboard dijelovi nisu funkcionalno mijenjani.


## v2.4 — dodatne vijesti
- Dodane nove vijesti: Zec il žaba, Duman, Žumberački odred za čistoću, Fugro/QGIS/Orux.
- Regeneriran kronološki news feed i /novosti article stranice.
- Poboljšan fallback za naslovne slike: og:image → prva velika slika iz članka → default SOV hero.
- Javni i registered/dashboard dijelovi nisu funkcionalno mijenjani.

## v2.5 — Dashboard menu layout
- Dashboard pojednostavljen na jedan hero/cockpit ekran.
- Lijeva kartica sada je stvarni glavni izbornik: SOV Karta, Baza, Izleti, GPX/KML.
- Desna kartica sada ima: Kalendar izleta, Oružarstvo, Zapisnici, SOV app.
- Izbačeni su admin approval i nepotrebni statistički/module blokovi s dashboarda.
- Sredina je očišćena: SOV logo + kratki CTA.


## v3.0 — Kalendar izleta Android sync
- Dodan `kalendar-izleta.html` kao web verzija Android logike za zajednički raspored.
- Učitavanje izleta iz Apps Script endpointa `action=listTrips`.
- Prijava na izlet preko `signupTrip`, uključujući opciju vozača.
- Raspored automobila preko `kreirajTab` + spremanje URL-a preko `updateRasporedUrl`.
- Weather integracija preko Open‑Meteo geocoding/forecast logike.
- Dashboard link `Kalendar izleta` sada vodi na novu dedicated stranicu.
