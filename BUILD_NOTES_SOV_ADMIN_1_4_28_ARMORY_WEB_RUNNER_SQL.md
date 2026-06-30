# SOV Admin APK 1.4.28 — Armory Web Match + Runner SQL

- Oružarstvo APK: category order, quick grid, subtitles and icons aligned with web oružarstvo UI (`oruzarstvo.html`).
- APK keeps grouped catalog for member browsing/request flow and raw app catalog for inventory mode.
- Speleo Runner: Supabase SQL leaderboard/submit RPC is now the canonical path.
- Legacy Google Sheet CSV remains fallback and is imported idempotently to SQL via `client_key`, so existing scores are not lost.
- Legacy Apps Script submit remains emergency fallback when Supabase submit fails.

Build: `versionCode 900116`, `versionName 1.4.28-armory-web-runner-sql`.
