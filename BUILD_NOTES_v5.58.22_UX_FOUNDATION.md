# SOV Web v5.58.22 — UX foundation / a11y / SQL safety pass

Baza: v5.58.21.

Ovo je web-only foundation build koji pretvara analizu UX/UI friendlinessa u konkretne brze i nužne popravke.

## Uključeno

- Novi zajednički `assets/sov-foundation-v55822.css` na svim HTML stranicama.
- Novi zajednički `assets/sov-foundation-v55822.js` na svim HTML stranicama.
- Vidljiv focus-ring za tipkovnicu i pristupačnost na cijelom webu.
- `prefers-reduced-motion` fallback za korisnike koji ne žele animacije.
- Touch target baseline od 44px za gumbe, tabove, chipove, inpute i navigaciju.
- Horizontalne nav trake dobivaju scroll hint / desni fade sa strelicom na mobitelu.
- Legacy tablice dobivaju mobile card prikaz preko JS data-label mapiranja.
- `oruzarstvo.html` dobiva loading skeleton dok se čeka Supabase katalog.
- SQL alati `speleo-sql-*` dobivaju produkcijski safety banner i dvostruku potvrdu za opasne radnje.
- `sync-status.html` je usklađen na v5.58.22.

## Namjerno nije napravljeno u ovom buildu

- Potpuni rewrite svih 5 header sustava u jedan server-side partial. Statički HTML build još nema templating layer; ovaj build uvodi zajednički foundation sloj koji ih vizualno i ponašanjem ujednačava bez rizika pucanja pojedinih modula.
- Gašenje statičkih `novosti/*` fallbackova. News CMS ostaje DB-first, a statičke novosti ostaju sigurnosni fallback dok urednik/CMS ne bude potpuno stabilan.
- Razbijanje `oruzarstvo.html` monolita. Dodan je skeleton i UX mitigacija; modularizacija ide kao veći refactor.

## SQL

Nema novog SQL-a.
