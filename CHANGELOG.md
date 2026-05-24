
## v3.7 — DOF + Geo karta layeri
- Iz Android app WMS preset logike preneseni DGU DOF i Geo karta/HOK layeri.
- SOV Karta sada ima TK25, DOF, Geo karta/HOK i OSM fallback kao base layere.
- Izleti karta dobila iste layer gumbe i Leaflet layer control.
- TopoDroid/Nacrti karta dobila isti skup base layera za pregled nacrta uz teren.
- TK25 ostaje default.


## v3.6 — TopoDroid web bridge
- Dodana nova stranica `topodroid.html` za nacrte, TopoDroid exporte i survey fileove.
- Upload nacrta/exporta u browseru s automatskim matchanjem na objekte iz `data/sov-baza.json`.
- Dodan preview/download, link na Drive arhiv, link na full objekt u Bazi i lokalni JSON index export.
- Dodano osnovno georeferenciranje: station, azimut, procijenjena duljina i napomena; prikaz smjera/protegnutosti na karti.
- Dashboard dobio direktan ulaz `Nacrti / TopoDroid`; SOV Karta dobila link i bogatiji panel za nacrte.
- Pripremljena logika za budući backend: Supabase/Drive sync, thumbnail cache, offline izlet paket i pravi TopoDroid parser.

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

## v3.4 — Zapisnici editor + DOCX workflow
- Dokumenti: Zapisnici sada imaju samo Novi zapisnik, Pregled zapisnika i Zapisnici skupštine.
- Dodan `novi-zapisnik.html`: datum se automatski popunjava, traži se tko vodi sastanak i tko je zapisničar.
- Editor koristi stalni format prema primjeru: SPELEO IZLETI, INI IZLETI, NAJAVE, RAZNO, Sastanak vodio, Zapisnik vodila.
- Dodan live preview u stilu zapisnika.
- “Spremi i pošalji” sprema zapisnik za urednika i generira DOCX download.
- Dodan `pregled-zapisnika.html`: lista pending zapisnika, preview, approve, edit, delete i DOCX export.
- Dodan `zapisnici-skupstine.html` kao posebna arhiva skupštinskih zapisnika.


## v3.5 — Nacrti / TopoDroid Drive match
- U Baza dodan panel za Nacrti / TopoDroid.
- Dodan link na glavni Google Drive folder nacrta.
- Dodan lokalni upload/index nacrta i TopoDroid exporta.
- Web automatski matcha fileove s objektima po nazivu jame/spilje.
- Full kartica objekta prikazuje pronađene nacrte s Prikaži / Skini akcijama.
- Dodan export lokalnog indexa nacrta kao JSON za kasnije spajanje na backend/Drive API.

## v3.8 — Supabase login + role permissions

- Replaced localStorage/dev-preview auth with Supabase Auth flow.
- Added `assets/supabase-config.js` for Supabase URL/anon key.
- Added `profiles` role/status model: `admin`, `user`, `editor`, `oruzar`.
- Registration now creates pending user profile.
- Login blocks users until admin approval.
- Admin approval page now loads users from Supabase and supports approve/reject/role changes.
- Registered pages are protected again.
- Oružarstvo is limited to `admin` and `oruzar`.
- Admin-only user approval is hidden from non-admin users.
- Added `SUPABASE_SETUP.md` with SQL and setup instructions.


## v3.9 — Mobile-first UX polish
- Dodan globalni `assets/mobile.css` koji se učitava na svim HTML stranicama.
- Sređena mobilna navigacija: sticky header, horizontalni scroll meniji, veći touch targeti.
- Dashboard preuređen za mobitel: logo kompaktniji, kartice čitljivije, izbornici u jednoj koloni.
- SOV Karta/Baza prilagođena mobitelu: karta gore, search i detalji ispod, toolbar i filteri skrolabilni.
- Kalendar izleta, dokumenti, zapisnici, editor članaka i admin panel dobili responsive kartice i forme.
- Poboljšani inputi, gumbi, dialogi, tablice, article stranice i news kartice na malim ekranima.
- Desktop layout ostaje netaknut; izmjene su aktivne samo na tablet/mobile breakpointovima.


## v4.0 Links main home
- Dodana sekcija Linkovi na glavnu stranicu.
- Dodani PDS Velebit, Komisija za speleologiju HPS, HPS, HGSS, Katastar speleoloških objekata RH i UIS.
- Ubačeni priloženi logotipi u assets/links.
- Dodan nav link na #linkovi i responsive/mobile layout.
