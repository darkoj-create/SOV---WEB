# SOV web v4.50 — Hard date/year import fix

Fix:
- Import više ne pokušava spremiti vrijednosti tipa `2.2024` / `02.2024` u integer/numeric polja.
- `production_year` za užad se izvlači kao godina (`2024`) iz formata `MM.YYYY`.
- Mjesečni datumi tipa `06.2024` i dalje idu u date polja kao prvi dan mjeseca.
- Količinska/numerička polja sada ignoriraju tekstualne i datumske vrijednosti umjesto da ruše import.

Zadržano:
- kategorije upsert fix
- duplicate rope SKU fix
- pojednostavljeni model: samo užad ima individualni kod, ostalo količinski artikli
