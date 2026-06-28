# SOV web v6.1.44h — SEO foundation + native zapisnici build guard

## Što je napravljeno

- Dodan `robots.txt`.
- Dodan `sitemap.xml` za javne stranice i novosti.
- Dodan `site.webmanifest`.
- U javne stranice dodani su:
  - SEO title,
  - meta description,
  - canonical URL prema `https://so-velebit.hr`,
  - Open Graph tagovi,
  - Twitter/X preview tagovi,
  - JSON-LD `Organization`, `WebSite`, `Article` i `BreadcrumbList` gdje ima smisla.
- Admin/cloud/private stranice dobile su `noindex,nofollow,noarchive`.
- Slike bez alt teksta dobile su sigurni fallback alt.
- Slike dobivaju `loading="lazy"` gdje nije bilo definirano.

## Zapisnici — važno

Stari zapisnički DOCX/DOC/ODT fajlovi nisu više dio web builda.

Uklonjeno iz builda: **288** raw zapisničkih dokumenata iz `assets/documents/zapisnici/`.

Zapisnici sada trebaju živjeti u Supabase tablici `meeting_minutes` kao native tekst i prikazivati se kroz `zapisnici-native.html`.

Stare stranice:

- `zapisnici-aktualni-2026.html`
- `zapisnici-arhiva-2017-2022.html`
- `zapisnici-cijela-arhiva.html`
- `pregled-zapisnika.html`

više ne nude DOCX download i preusmjeravaju na `zapisnici-native.html`.

## Index / noindex logika

Indexable javni dio:

- homepage,
- O društvu,
- Povijest,
- Speleoškola,
- Velebitaški duh,
- Velebiten,
- Pridruži nam se,
- Pročelništvo,
- Vijesti,
- Video,
- `novosti/*.html`.

Noindex:

- dashboard,
- login,
- admin,
- dokumenti/zapisnici interni dio,
- oružarstvo,
- arhivar,
- izleti cloud,
- karta/baza,
- SQL alati,
- sync/status/tracking.

## Kad domena sjedne

1. Provjeriti da je `https://so-velebit.hr` stvarna finalna domena.
2. Deployati ovaj build.
3. U Google Search Console dodati domenu.
4. Poslati `https://so-velebit.hr/sitemap.xml`.
5. URL Inspection za homepage, `speleoskola.html`, `vijesti.html` i par najvažnijih novosti.


## Dodatna zaštita

- Uklonjen je i jedini preostali `.docx` iz `documents/legacy-wordpress/`, tako da build ne nosi Word dokumente.
- `data/zapisnici-*.json` stari indeksi zamijenjeni su placeholderima koji upućuju na native zapisnike u bazi.
