# SOV Web v6.1.45f — Oružarstvo Zatraži ide u košaricu

## Fix
- `oruzarstvo.html`: zeleni gumb **Zatraži** više ne kreira odmah backend zahtjev.
- Gumb sada radi kao webshop: dodaje artikl u lokalnu košaricu/drawer **Moj zahtjev**.
- Zahtjev se šalje oružaru tek iz drawera klikom na submit/slanje zahtjeva.
- Popravljeno mapiranje item polja: koristi `id/name` uz fallbackove (`_id`, `catalog_id`, `legacy_id`, `sku`, `display_name`, `model`).
- Zadržan drawer flow i loan-shop UI iz v6.1.45d.

## Test
1. Otvori `/oruzarstvo.html`.
2. Uđi u kategoriju i klikni zeleni **Zatraži**.
3. Mora se otvoriti drawer **Moj zahtjev / košarica**.
4. Stavka mora biti vidljiva u košarici.
5. Tek slanje iz drawera kreira zahtjev oružaru.
