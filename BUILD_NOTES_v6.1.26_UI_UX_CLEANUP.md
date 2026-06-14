# SOV Web v6.1.26 — UI/UX cleanup

UI-only cleanup build based on v6.1.25 trips-refresh-stability.

## Changed
- Dashboard title/version text updated to v6.1.26.
- Dashboard role preview is visible only for webmaster when preview mode is explicitly enabled (`?preview=1` or `SOV_OPEN_PREVIEW_MODE=true`).
- Calendar developer/source panels hidden from user UI while keeping their hook buttons in DOM.
- Calendar user-facing status/copy cleaned from technical wording.
- Tracking onboarding text simplified into clear 3-step action copy.
- Documents page cleaned: placeholder tutorial cards hidden and replaced with a single note.
- Public nav label changed from “SOV Cloud” to “Članski ulaz”.
- Redirect page `izleti.html` copy simplified and dark flash preserved.

## Not changed
- No SQL changes.
- No Supabase/RPC/from/auth business logic changes.
- No APK changes.
- No trips save/list/delete logic changes.

## Validation
- HTML/JS syntax checks run for touched pages.
- ZIP integrity checked.
