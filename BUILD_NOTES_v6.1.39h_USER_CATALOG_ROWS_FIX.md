# SOV web v6.1.39h — Oružarstvo user catalog rows fix

## Problem
User-facing `oruzarstvo.html` could remain on the DB loading card with:

`rows is not defined`

This happened in the external DB-gate boot script because the user catalog renderer called `rows()` but did not define it inside the same script scope. Older inline scripts had their own scoped `rows()` helpers, but those are not globally available to the external boot file.

## Fix
- Added a local `rows()` helper to `assets/oruzarstvo-boot-v615.js`.
- The helper reads `window.DATA.items`, `window.DATA.ropes`, and `window.DATA.pieces` after Supabase data is loaded.
- It normalizes id/name/category/subcategory/quantity/available/unit/status/member visibility enough for the user catalog cards and filters.
- Updated the boot build label to `6.1.39h-user-catalog-rows-fix`.
- Updated `oruzarstvo.html` cache-bust for `assets/oruzarstvo-boot-v615.js`.

## Not changed
- No SQL changes.
- No item/category/location/loan data changes.
- No auth changes.
- No master inventory/inventura export logic changes.

## Quick regression
- `node --check assets/oruzarstvo-boot-v615.js` passes.
- User catalog should now render categories after Supabase live load instead of stopping at `rows is not defined`.
