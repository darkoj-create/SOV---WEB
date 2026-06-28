# SOV web v6.1.45o — Oružarstvo clean no-pending fix

Scope: `oruzarstvo.html` only.

Root cause fixed:
- Previous page had years of stacked inline boot/render scripts plus external boot scripts.
- Multiple independent loaders competed for the same `#catalog`, `DATA`, `renderCatalog`, and network sources.
- It still requested `data/oruzarstvo-data.json` and Supabase manifest/catalog in parallel, so a pending request could keep the old loading panel alive.

Fix:
- Replaced `oruzarstvo.html` with a clean deterministic page.
- Removed all old layered boot scripts from this page.
- Removed dependency on `/data/oruzarstvo-data.json` and `sov_equipment_catalog_manifest` for this page.
- Catalog now loads directly from `public.sov_equipment_app_catalog_grouped` through Supabase REST.
- Supabase fetches have AbortController timeout so browser requests cannot remain pending forever.
- Kept: Osobna oprema + SRT merged category, SRT pack, cart, request submit, My requests.

SQL: none.
