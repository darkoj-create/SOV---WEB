# SOV Admin v1.4.16i — Trips collapsible cards

Base: v1.4.16h inventory global progress.

## Changed
- `FieldPackageFeature.kt`
  - SOV Cloud trip cards are now collapsed by default.
  - Active trips show a compact/simple row with date, location, goal and leader.
  - Participants/drivers are shown as compact one-line chips when collapsed.
  - Full details, transport, tracking, shared packages, mail announcement, edit/delete, signup and weather render only after expanding the card.
  - Weather fetch is gated behind expansion so the active trip list stays fast and clean.

## Version
- versionCode: 900085
- versionName: 1.4.16i-trips-collapsible-cards

## Not changed
- No SQL changes.
- No web changes.
- Session refresh, DB-gate, cache-first armory and inventory global progress remain from previous builds.
