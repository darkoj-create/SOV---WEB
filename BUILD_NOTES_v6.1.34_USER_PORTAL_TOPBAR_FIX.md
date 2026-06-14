# SOV Web v6.1.34 — User portal top bar fix

Baseline: sov-web-build-v6.1.33-role-view-check-fix.zip

## Changed
- dashboard.html: added hard guard so the user portal top bar always shows only:
  - Home
  - Odjava
- dashboard.html: removes accidental/cached public topbar/nav if it appears on the dashboard.
- dashboard.html: hides stray user/profile chips in the top bar.
- dashboard.html: version label bumped to v6.1.34.

## Not changed
- No SQL changes.
- No APK changes.
- No Supabase/RPC changes.
- No auth.js business logic changes.
- No trips/oružar/arhivar business logic changes.

## Reason
v6.1.32 changed the static dashboard header, but the shared/public shell/header could still appear from cached or injected markup. This build forces the dashboard/user portal header at runtime and via CSS.
