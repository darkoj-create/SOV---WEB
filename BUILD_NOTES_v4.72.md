# v4.72 — Category de-dup canonical fix

Popravlja duple glavne kategorije nastale iz različitih naziva iz Sheeta/importa.

Primjeri spajanja:
- Medicina / Medicinska -> Medicinska oprema
- Elektro i foto -> Elektro i foto oprema
- Užad -> Užad i užetna oprema
- Ekspedicijska i kamp oprema -> Oprema za logor
- Alpinistička i ronilačka -> kanonizirano prema nazivu artikla ili u Alpinistička oprema

Dodano:
- normalizacija u importu
- normalizacija u live Supabase loaderu
- normalizacija u Oružar Master inventaru
- SQL cleanup za postojeću bazu
