# SOV Web v5.59.5 — Tracking simplified team UI

## Scope
Web-only UX patch for `tracking.html` on top of v5.59.4.

## Changes
- Reworked `tracking.html` into a simple three-step flow: Izlet → Team → Prati na karti.
- Simplified team creation: open a new team for the selected trip, choose Lite or Ruta/GPX, get join code immediately.
- Added clear join-by-code block.
- Map + team list are now the main screen, with KPIs and GPX export kept compact.
- Improved mobile layout for field use: one-column flow, large touch targets, map card + bottom team list.
- Kept existing Supabase RPCs and data model; no SQL required.
- Updated `sync-status.html`, `VERSION.txt`, `BUILD_VERSION.txt`.

## Test checklist
- Open `/tracking.html`.
- Select a trip.
- Click `Dodaj team`, create a team, verify join code appears.
- Select one team or all teams and click `Prati na karti`.
- Verify markers/trails appear after Android app sends points.
- Test mobile width: controls should be single-column and touch-friendly.
