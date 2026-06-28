# SOV Web v5.58.3 — News CMS / Urednik DB-first

## Što je novo
- `sov_news` je sada izvor istine za javne vijesti.
- SQL seed migrira sve postojeće statičke novosti iz `novosti/*.html` u bazu.
- Novi `news-editor.html` omogućuje uredniku/adminu:
  - uređivanje postojećih vijesti
  - dodavanje nove vijesti
  - naslov, slug, kategoriju, autora, datum
  - kratki opis i puni tekst
  - naslovnu fotografiju preko URL-a ili Supabase Storage uploada
  - galeriju fotografija
  - PDF/CTA linkove
  - objavljeno/skriveno, prikvačeno, featured
- Novi javni detail view: `vijest.html?slug=...`.
- `index.html` i `vijesti.html` pokušavaju učitati vijesti iz baze; ako SQL još nije pokrenut ili je Supabase nedostupan, stari statički sadržaj ostaje fallback.
- `dashboard.html` modul “Vijesti” sada otvara pravi `news-editor.html`, a stari `napisi-clanak.html` preusmjerava na editor.

## Obavezno
Prije deploya weba pokrenuti:
`SUPABASE_SOV_NEWS_CMS_v5_58_3.sql`

SQL pokušava napraviti public Supabase Storage bucket `sov-news` za upload fotki.
Ako Storage policy/bucket dio bude preskočen u SQL editoru, bucket ručno napraviti u Supabase Storageu s imenom `sov-news` i public read.
