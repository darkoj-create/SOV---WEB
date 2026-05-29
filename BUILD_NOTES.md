## v5.45 — Armory Core Cleanup

- Kategorizacija: client više ne pokušava biti canonical mozak; web čita SQL/grouped view polja i koristi samo minimalni fallback.
- Statusi posudbi: uklonjen dead-end approval model; standard je pending/requested → issued → returned/partial_return (+ cancelled).
- Zahtjevi: kompatibilnost za multi-item request lines (`name` + `item_name`).

# v4.8 Main nav + brand cleanup

- Sređen gornji lijevi SOV logo da bolje stane u ovalni/zaobljeni okvir.
- Pročelništvo, Povijest i Velebitaški duh prebačeni pod glavni izbornik “O nama”.
- Glavni ekran više nema zasebne top-level linkove Pročelništvo i Povijest.
- Mobile nav dodatno podešen da se linkovi ne lome ružno.

## v4.10
- Polished main navigation and logo containment.
- Pročelništvo and Povijest integrated under O nama navigation.
- Rebuilt O nama page with internal sections/tabs and cleaner card logic.


v4.11: Polished O nama UX, removed floating duplicate internal menu behavior, improved logo fit and responsive nav.

## v5.44 — Final Equipment Grouping Rules
- Added canonical grouped equipment catalog source for web UI.
- Included `SUPABASE_ORUZARSTVO_APP_CATALOG_v5_44_FINAL_GROUPING_RULES.sql`.
