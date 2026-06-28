# SOV Web v6.1.43 - prerelease polish

## Scope

Pre-release cleanup of visible admin/development copy in the app UI.

## Changed

- Removed dashboard role preview toolbar and technical maintenance panels from the main UI.
- Removed visible build/version rows from the dashboard.
- Rephrased dashboard role/status copy to user-facing access wording.
- Removed technical navigation entries from the shell drawer.
- Rephrased login/registration copy without database/internal approval wording.
- Rephrased user approval and notification screens with neutral user-facing labels.
- Rephrased equipment loader/status messages to use catalog/evidencija wording instead of database/debug terms.
- Updated build manifest and cache bust to `6.1.43`.

## SQL

No SQL changes required.

## Verification

- `node --check assets/auth.js`
- `node --check assets/sov-shell-v55825.js`
- `node --check assets/oruzarstvo-boot-v615.js`
- `node --check assets/oruzarstvo-supabase.js`
- `python -m json.tool update.json`
