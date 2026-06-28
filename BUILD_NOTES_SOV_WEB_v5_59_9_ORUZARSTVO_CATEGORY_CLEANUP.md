# SOV Web v5.59.9 — Oružarstvo category cleanup

Sređena je kanonska kategorizacija Oružarstva u statičkom JSON-u, web UI-u i SQL helper funkcijama.

## Glavne promjene
- Obične AA/AAA/9V/USB/powerbank/solarne baterije i punjači idu u **Elektro, rasvjeta i foto**.
- Bosch/Hilti/Makita baterije, punjači, bušilice i svrdla idu u **Bušilice i svrdla**.
- Dron baterije, punjači, elise, kontroleri i koferi idu u posebnu kategoriju **Dronovi**.
- Kompas/busola/Suunto/DistoX/TopoDroid idu u **Oprema za crtanje**.
- `Užad` je ujednačeno u **Užad i užetna oprema**.
- `Ostali alat` je ujednačen u **Alat i radionica**.
- Static catalog `data/oruzarstvo-data*.json` je presložen po novim kategorijama i podkategorijama.

## SQL
Pokrenuti `SUPABASE_ORUZARSTVO_CATEGORY_CLEANUP_v5_59_9.sql` nakon deploya weba da Supabase viewovi vrate istu logiku kao statički katalog.
