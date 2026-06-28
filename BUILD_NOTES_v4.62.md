# v4.62 hard recovery

- Popravljen uzrok praznog Inventara: novi inventar HTML je gledao `window.DATA`, a postojeći loader je koristio globalni `DATA`.
- Dodan hard data hydration: prvo Supabase live nakon importa, fallback na `data/oruzarstvo-data.json`.
- Katalog i Oružar Inventar ponovno renderaju kategorije/podkategorije/artikle nakon importa.
- Oružar Master navigacija očišćena na Inventar / Inventura / Posudbe.
