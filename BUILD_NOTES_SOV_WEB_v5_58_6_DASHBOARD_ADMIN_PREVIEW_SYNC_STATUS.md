# SOV Web v5.58.6 — Dashboard admin card + role preview + sync-status update

## Dashboard
- Added one consolidated Admin card on `dashboard.html`.
- Moved technical/admin links into that Admin card: users, roles, sync status, audit/devices, SQL sandbox/go-live/safe/object hub.
- Removed floating SQL developer shortcuts from dashboard.
- Added dashboard-only top-right role preview slider: Admin, Oružar, Arhivar, Urednik, User.
- Slider only filters dashboard cards visually; it does not change real permissions or unlock protected pages.
- System health panels are now admin-only.

## Auth protection
- `sync-status.html` and `audit-status.html` now require Admin.
- Arhivar dashboard/submission/export pages are explicitly protected as Arhivar/Admin pages.

## Sync status
- Updated `sync-status.html` from old v5.32 wording to v5.58.6.
- Added checks for `sov_news`, `speleo_object_submissions`, and `speleo_object_submission_files`.
- Updated build/version block to current SOV web/news/arhivar context.
- Future rule: update `sync-status.html` with relevant web builds.

## SQL/APK
- No new SQL required.
- No APK change required.
