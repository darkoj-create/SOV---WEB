# SOV web build v6.1.44e — Gmail zapisnici kao native tekst

Baseline: `sov-web-build-v6.1.44d-wordpress-featured-media.zip`

## Cilj

Zapisnici iz Gmail/DOCX izvora više ne moraju biti uploadani kao DOCX datoteke u web/storage. Apps Script iz DOCX-a izvuče tekst, Supabase ga spremi u `meeting_minutes`, a web ga prikazuje kao lijepi native zapisnik koji se kasnije može exportati u DOCX.

## Dodano

- `zapisnici-native.html`
- `assets/zapisnici-native-v6144e.css`
- `assets/zapisnici-native-v6144e.js`
- `SUPABASE_SOV_GMAIL_ZAPISNICI_NATIVE_v6_1_44e.sql`
- `SOV_GMAIL_ZAPISNICI_APPS_SCRIPT_v6_1_44e_NATIVE_QUEUE.gs`

## Promijenjeno

- `dokumenti.html` dobiva ulaz `Živi zapisnici iz Gmaila`.
- `zapisnici-najave.html` Gmail opis sada jasno kaže da se sprema native tekst bez DOCX uploada.

## Nije dirano

- Izleti core
- Oružarstvo
- Arhivar
- Karta
- WordPress media build 44d
- APK

## Kako postaviti

1. U Supabase SQL editoru pokrenuti:
   `SUPABASE_SOV_GMAIL_ZAPISNICI_NATIVE_v6_1_44e.sql`

2. U Google Apps Script zalijepiti:
   `SOV_GMAIL_ZAPISNICI_APPS_SCRIPT_v6_1_44e_NATIVE_QUEUE.gs`

3. U Apps Scriptu jednom ručno pokrenuti:
   `installSovGmailZapisniciTriggers()`

4. U webu otvoriti:
   `zapisnici-najave.html` za Gmail sync / najave iz zapisnika
   `zapisnici-native.html` za native prikaz samih zapisnika

## Napomena

`INGEST_KEY` je zadržan kao postojeći placeholder `SOV_GMAIL_ZAPISNICI_2026_CHANGE_ME` da ostane kompatibilno s dosadašnjim scriptom. Kasnije ga treba zamijeniti stvarnom tajnom vrijednošću u SQL-u i Apps Scriptu u istom trenutku.
