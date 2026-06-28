# SOV Web v6.1.31 — Client error logs

Baseline: sov-web-build-v6.1.30-oruzar-grid-visual-fix.zip
Requires SQL: SUPABASE_SOV_CLIENT_ERROR_LOGS_v6_1_31.sql

## Changed
- Added `assets/sov-client-logger.js`.
- Web now logs:
  - JavaScript runtime errors
  - unhandled promise rejections
  - failed Supabase fetch responses
  - manual logs via `window.SOVClientLogger.*`
- Added `admin-client-errors.html` for admin/webmaster review.
- Added dashboard webmaster link: `Greške korisnika`.
- Injected logger script into logged-in HTML pages that already load `auth.js`.
- Updated `update.json`, `VERSION.txt`, `BUILD_VERSION.txt` to v6.1.31.

## Not changed
- No APK code in this web ZIP.
- No trips save/list/delete logic changed.
- No armory business logic changed.
- No archive business logic changed.
- No auth.js changes.
- No existing Supabase RPC changed.

## Deploy
1. Run `SUPABASE_SOV_CLIENT_ERROR_LOGS_v6_1_31.sql` in Supabase.
2. Deploy this web ZIP.
3. Open `admin-client-errors.html` as admin/webmaster.
4. Use `Pošalji test log` to confirm logging works.
