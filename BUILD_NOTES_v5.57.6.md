# SOV Web v5.57.6 — Arhivar HTML real full SQL detail fix

- `arhivar.html` sada ima vidljiv v5.57.6 badge, da se odmah vidi da je deploy stvarno povukao novu datoteku.
- Klik na objekt više ne ostaje zaglavljen na “učitavam” ako detail RPC padne; greška se prikazuje u panelu.
- Detail panel sada otvara “SVA SQL POLJA” sekciju by default.
- SQL RPC sada vraća `to_jsonb(speleo_objects_staging)` i spojeni `raw` JSON, ne samo ručno odabrane raw ključeve.
- Pločica se traži i kroz stvarne SQL kolone i kroz raw/import JSON.
