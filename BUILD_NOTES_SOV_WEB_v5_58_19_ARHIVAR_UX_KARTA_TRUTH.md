# SOV Web v5.58.19 — Arhivar UX/mobile + Karta truth status fix

## Arhivar UI/UX
- Clean/premium responsive pass for `arhivar.html`, `arhivar-predane-jame.html`, and `arhivar-izvoz.html`.
- Better mobile browser layout: horizontal nav, touch-friendly buttons, one-column panels, sticky action tabs, less cramped detail sections.
- Updated versions/cache-busting for Arhivar assets.

## Karta truth fix
- `karta.html` status color now follows Arhivar truth first.
- Negative statuses such as `nije_u_katastru`, `nije u katastru`, `nepotpuno`, missing categories, or `fali...` are evaluated before any green `u katastru` phrase.
- Fixes cases like “Zidana pec” where text containing `nije u katastru` could previously match the green substring `u katastru`.
- `assets/sov-map-db.js` normalizes negative `katastar_readiness` so `base_in_cadastre` cannot override it on the map.

## Sync
- `sync-status.html` updated to v5.58.19.

## SQL
- No mandatory SQL required if v5.58.17 map RPC is already installed.
- Included optional SQL file only as packaged reference for the current map feed.
