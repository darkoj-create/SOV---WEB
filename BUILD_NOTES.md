
## v3.1 notes
Ne dirati javni portal/news. Promjene su samo: `kalendar-izleta.html`, `izleti.html`, dokumentacija verzije.
Google Sheet endpoint ostaje isti kao Android app: `action=addTripV2` prema FIELD_TRIPS webappu.

# Build notes

VAŽNO:
- Ne pregaziti javni portal parcijalnim registered buildom.
- Novosti su glavna stranica (`index.html`).
- Article tekstovi se ne prepravljaju ručno; dizajn mijenja layout, ne sadržaj.
- Registered dijelovi su u ovom dev buildu otvoreni bez login provjere dok se ne spoji Supabase.

# Build notes — v2.1

This is a DEV PREVIEW build. Registered pages are intentionally open without login until Supabase auth is connected.

Do not remove public pages when updating registered tools. Always ship full site + SOV Cloud together.

# Build notes

Ne dirati javni landing/Povijest/Velebitaški duh u map-only fixevima. Registered SOV Karta je u `baza.html`.

## v2.3 note
Ne pregaziti `index.html`, `vijesti.html`, `/novosti/*` ni postojeće registered alate. Ovaj build mijenja dashboard vizualno i UX-strukturno, ali ne mijenja auth guard logiku.


## v3.0 Kalendar izleta
Izvor logike je Android app: `FieldPackageSheetSyncClient.kt` i `FieldPackageFeature.kt`. Web je statički i koristi direktan browser fetch prema Apps Scriptu; ako CORS blokira poziv, prikazuje demo podatke. Za produkciju preporuka: dodati Vercel API proxy `/api/trips`.

## v3.8 Auth baseline

Od ove verzije registered dio više nije dev-open. Koristi se Supabase Auth.
Prije deploya obavezno upisati URL i anon key u `assets/supabase-config.js` i pokrenuti SQL iz `SUPABASE_SETUP.md`.

Ne vraćati lokalni `sovCloudUsers` auth kao produkcijsku logiku.


## Mobile rules from v3.9
- Ne dirati desktop dizajn ako nije nužno.
- Sve nove stranice moraju učitati `assets/mobile.css`.
- Karte na mobitelu: karta ide gore, alati/search ispod, horizontalni scroll za toolbare.
- Minimalni touch target: 44px.


## v4.1 SOV hero brand refresh
- Zamijenjen glavni header/hero photo fotografijom ulaza u Munižabu.
- Dodan okrugli SOV logo i SOV wordmark u gornji header i hero zonu.
- Dodan diskretan dark overlay i responsive logo ponašanje za mobile.


## v4.2
- Dodan Kontakt u top navigaciju na main pageu.
- Dodana Kontakt sekcija: pročelnik, tajnik, adresa, Google Maps i Wednesday open hours.


## v4.3 UI cleanup verified
- cleaned public copy on O društvu, Pročelništvo, Speleoškola, Dokumenti and dashboard
- removed development-style explanatory text and placeholder labels
- hid dev-preview floating badges from production UI
- added safer responsive wrapping to prevent overlapping words/letters in nav, cards and headings
- removed WordPress ad/share/like cruft from imported article pages
- preserved existing public pages, registered modules, Supabase auth and content structure
