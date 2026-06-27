# SOV Web v6.1.45g — Oružarstvo full UX remake

Scope: visual/UX layer for `oruzarstvo.html`.

## Main change
- Catalog now behaves like a webshop: category → subcategory/search → item card → add to cart.
- Green “Zatraži” button only adds to cart and opens checkout drawer.
- Checkout drawer clearly states that nothing is sent until “Pošalji zahtjev oružaru”.
- My requests view is rendered as order/status cards instead of a raw table.
- Mobile-first layout, sticky topbar/tabs, floating cart button, clearer cards, smaller icons, better spacing.

## Files added
- `assets/oruzarstvo-ux-remake-v6145g.css`
- `assets/oruzarstvo-ux-remake-v6145g.js`

## Files changed
- `oruzarstvo.html`

## No backend changes
No SQL or Supabase permissions changed in this build. Existing request/loan backend flow remains unchanged.
