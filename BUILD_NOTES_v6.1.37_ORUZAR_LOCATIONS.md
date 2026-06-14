# SOV Web v6.1.37 — Lokacije (dodavanje, prikaz, odabir, filter)

## Što je novo
1. **Dodavanje lokacija iz appa** — u "Uredi artikl" lokacija je sad **dropdown** (popis iz
   `equipment_locations`) s opcijom **➕ Nova lokacija…** (prompt → odmah se kreira i odabere).
2. **Odabir lokacije po artiklu** — umjesto slobodnog teksta, biraš iz popisa (manje tipfelera,
   uredno povezivanje).
3. **location_id linkanje** — kad spremiš artikl, `location_name` se automatski poveže s
   `location_id` (ilike na equipment_locations) → točan filter/grupiranje, ne samo tekst.
4. **Filter po lokaciji** — dropdown "📍 Sve lokacije" na ekranu kategorija, u listi artikala i u
   rezultatima pretrage; vrijedi na **inventaru i inventuri** (dijele isti render).
5. Lokacija se i dalje prikazuje na svakoj kartici (badge) — sad i filtrabilno.

## Datoteke
- `assets/oruzarstvo-supabase.js`:
  - grouped grana sad učitava `out.locations` (prije samo fallback) → master ima popis lokacija.
  - nove funkcije `loadLocations()` i `createLocation(name,type)` (+ export).
  - `updateEquipmentItemFull` razrješava `location_id` iz `location_name`.
- `assets/oruzar-master-clean.js`:
  - `locList()`, `locFilterHtml()`, `locationSelectHtml()`, `onLocChange()`, `pickLoc()`.
  - modal: dropdown lokacije; toolbari: filter lokacije; `STATE.locFilter` u `filtered()`.
- Cache token: `?v=6.1.37-locations`.

## RLS
`equipment_locations` ima otvorene INSERT/UPDATE/DELETE policyje (SOV v4.85) — provjereno da
`anon` smije dodavati. Bez DB promjena u ovom buildu.

## Test
Deploy → Ctrl+Shift+R → otvori artikl → Lokacija je dropdown → odaberi ili ➕ dodaj novu →
Spremi → reload (ostaje). Na listi/inventuri gore je "📍 Sve lokacije" filter.
