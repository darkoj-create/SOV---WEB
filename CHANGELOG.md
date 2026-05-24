
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
