# SOV Web v6.1.45h — Oružarstvo safe UX remake fix

Fixes regression from v6.1.45g where the aggressive custom catalog renderer/CSS could hide legacy category grids and icons before data/render hooks were ready.

Changes:
- Based on v6.1.45f working cart flow.
- Removes aggressive v6.1.45g custom renderer from the delivered page.
- Adds `assets/oruzarstvo-safe-ux-v6145h.css` as a safe visual remake layer.
- Keeps original category/icon/catalog renderer as source of truth.
- Preserves `Zatraži -> cart drawer` behavior.
- Does not modify Supabase or SQL.

Test:
- /oruzarstvo.html loads categories and icons.
- Clicking category opens subcategories/items.
- Clicking Zatraži opens cart drawer and adds item locally.
