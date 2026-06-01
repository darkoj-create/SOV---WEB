# BUILD NOTES — SOV Web v6.0 · "WOW LAYER" (premium motion + atmosphere)

Date: 2026-06-01
Type: **Additive, non-destructive UX/visual layer.** No data flows, Supabase
calls, auth, routing, role logic, or existing class names were changed or removed.

## What this release does
Adds a single drop-in enhancement layer on top of the existing
`sov-polish-v55826` layer. It elevates the public portal and the cloud shell to a
"premium" feel without touching any backend wiring.

### New files
- `assets/sov-wow-v6.css` — visual/motion layer (scoped, namespaced `--wow-*`).
- `assets/sov-wow-v6.js`  — engine (feature-detected, never throws, reduced-motion aware).
- `showcase/sov-premium-showcase.html` — **self-contained** demo (no backend, no
  network beyond the Google font). Open it in any browser to see every effect live.
  Use it as a visual reference / stakeholder pitch.

### Injected into (head: font + css · body end: js)
index, o-drustvu, povijest, procelnistvo, velebitaski-duh, speleoskola,
pridruzi-nam-se, videos, vijesti, vijest, login, dashboard.
Injection is **idempotent** — re-running the injector will not duplicate tags.

## Features added
1. **Distinctive display type** — Bricolage Grotesque on H1/H2 heroes only; body
   stays Inter. Scoped, with full fallback.
2. **Scroll progress bar** — gradient lime→teal→blue, GPU-friendly rAF, hidden on reduced-motion.
3. **Pointer aurora** — soft spotlight that tracks the cursor across hero sections
   (desktop / fine-pointer only).
4. **Page-load orchestration** — staggered hero entrance (eyebrow → h1 → sub → CTA → stats).
5. **Scroll reveal** — IntersectionObserver on news/links/contact/gallery/cloud cards.
   **Mutation-aware**: re-tags the Supabase-injected news cards the moment
   `news-public-loader.js` swaps them in, so dynamic content animates too.
6. **Premium card interaction** — diagonal sheen sweep + lift + glow on news/link cards.
7. **Magnetic CTAs** — primary buttons + expedition CTA subtly follow the cursor.
8. **Count-up hero stats** — homepage gets a 4-stat strip (1954 / 56 / 1500+ / 70 god.)
   that animates from 0. **Edit the `STATS` array in `sov-wow-v6.js` to your real figures.**
9. **News loading skeleton** — shimmer placeholder only when the section is genuinely
   empty during a Supabase fetch (never hides the static fallback content).
10. **Global UX polish** — `:focus-visible` ring, custom selection + scrollbar,
    animated section-header underline, nav hover underline-grow, banner zoom + pulse CTA.

## Safety / accessibility
- Every effect respects `prefers-reduced-motion: reduce` (motion off, visuals kept).
- All JS is wrapped in try/catch per subsystem — one failing piece can't break the page.
- Loads **after** `sov-polish-v55826.js`, so `body.sov-public-page` / `sov-cloud-page`
  is already set; the layer also re-detects independently.
- No `localStorage`, no network calls, no third-party scripts except the Google font.

## How to extend
- Real club numbers: edit `STATS` in `sov-wow-v6.js`.
- Reveal more elements: add selectors to `REVEAL_SELECTORS`.
- Disable on a page: remove the two injected tags from that page's `<head>`/`</body>`.

## Rollback
Delete the two `<link>`/`<script>` lines for `sov-wow-v6.*` from any page (or all),
and delete `assets/sov-wow-v6.css` + `assets/sov-wow-v6.js`. The site returns to
the exact v5.59.11 state.

---

# v6.0 · CLOUD UPDATE — SOV Cloud member tools

Extends the WOW layer to the member/admin tools with a dedicated, **map-safe**
stylesheet and engine additions. No data flows, RPCs, auth, or markup changed.

### New file
- `assets/sov-wow-app-v6.css` — premium layer scoped to `body.sov-cloud-page`.
- `showcase/sov-cloud-showcase.html` — self-contained demo of oružarstvo + izleti + karta (no backend).

### Injected into (25 cloud pages)
oružarstvo (+ master/import/inventar/inventura/posudbe/notes), izleti, izleti-cloud,
kalendar-izleta, karta, arhivar (+ dashboard/predane-jame/izvoz/zahvati),
pregled-baze, pregled-zapisnika, topodroid (+ import), napisi-clanak,
speleo-zapisnik, novi-zapisnik, dokumentacija, inventura, and dashboard.

### Functionality check performed (static)
All app engines syntax-validated with `node --check`, **0 errors**:
oruzarstvo-supabase, sov-trips-cloud, sov-map-db, arhivar-workbench,
arhivar-export, speleo-submissions, sov-trip-assets, sov-inbox, auth, supabase-config.
No TODO/FIXME/HACK markers in the four core tool engines.
(Live data paths require the Supabase backend and were not exercised.)

### What got the premium treatment
- **Oružarstvo:** inventory tiles now have status-colored glow (good=lime/teal,
  warn=gold, danger=red) + big display numbers that **count up** when they scroll
  in — async-safe, so they animate the moment Supabase fills the value, and skip
  any tile whose value is text. Tables get sticky glowing headers, zebra rows, and
  row-hover. Category/item/scope cards get sheen-sweep + lift.
- **Izleti:** hero glow, segmented control + chips with active gradient, trip cards
  lift, calendar `calTrip` pills, and a glassy weather forecast card.
- **Karta:** floating panels (toolbar, legend, search, object popup, preview) become
  glassmorphism; legend dots glow by status. **The Leaflet canvas (`#map`,
  `.leaflet-*`) is hard-fenced** — the engine never tags it for reveal and CSS
  forcibly disables transform/filter/animation on it, so tiles never blur or drift.
- **Arhivar / baza:** panels, tables, and cards inherit the same glass + reveal vocabulary.

### Safety specific to this update
- `inMap()` guard in the engine prevents reveal/opacity ever touching map elements.
- Reveal + count-up are re-applied via debounced MutationObserver on tool containers,
  so JS-rendered content (tiles, rows, results, drawings) still animates after Supabase load.
- Tabs keep working: hidden `.tabpane` content reveals only when its tab is shown.
- Full `prefers-reduced-motion` support; every JS subsystem wrapped in try/catch.
