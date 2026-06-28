# SOV Web v6.1.33 — Role view correctness fix

Baseline: sov-web-build-v6.1.32-user-portal-header-clean.zip

## Checked
- Dashboard role filtering via `data-dash-visible`
- Webmaster preview mode visibility
- User/internal module separation
- Shell role links for the normalized drawer

## Found
- Regular user correctly did not see role cards, but the "Rad po ulozi" section header could remain visible because the JS selector targeted `.sov-section-head` while the real element uses `.v609-zone-head`.
- Oružar was incorrectly excluded from regular member Izleti/Tracking links in dashboard and shell.

## Changed
- dashboard.html: fixed selector so the internal role section header hides when no role module is visible.
- dashboard.html: added `oruzar` to Izleti and Field tracking visibility, because Oružar is still also a member.
- assets/sov-shell-v55825.js: added `oruzar` to Izleti link roles in shell drawer.
- dashboard.html: version markers updated to v6.1.33.

## Not changed
- No SQL changes.
- No APK changes.
- No Supabase/RPC changes.
- No auth logic changes.
- No role permissions/RLS changes; this is only UI visibility correctness.

## Role matrix after fix
- user: Karta, Predaj novu jamu, Izleti, Tracking, Oprema, Dokumenti, Napiši članak. No internal role section.
- editor: user tools + Urednik vijesti.
- oruzar: user tools + Oružarstvo + direct Oružar panel.
- arhivar: user tools + Arhivar, Predane jame, TopoDroid import.
- admin: role operations except webmaster-only technical system details.
- webmaster: all tools + preview + technical system details.
