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
