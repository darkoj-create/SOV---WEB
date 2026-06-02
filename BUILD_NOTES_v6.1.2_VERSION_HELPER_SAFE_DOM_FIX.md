# SOV web v6.1.2 — version helper safe DOM fix

## Problem
`assets/sov-version.js` in v6.1.1 set `document.body.dataset.sovVersion`, then updated all `[data-sov-version]` nodes with `textContent = version`.
Because `body.dataset.sovVersion` creates `data-sov-version` on `<body>`, the helper matched `<body>` and replaced the entire page with only `6.1.1`.

## Fix
- `sov-version.js` now ignores `<html>` and `<body>` when updating version labels.
- `<html>` and `<body>` now use `data-sov-build-version` instead of `data-sov-version`.
- All HTML references to `assets/sov-version.js` bumped to `?v=6.1.2`.
- `update.json`, `VERSION.txt`, and `BUILD_VERSION.txt` bumped to 6.1.2.

## SQL
No SQL changes.

## Rollback
Return to v6.1.0/v6.0.9 only if needed, but do not redeploy v6.1.1 because it contains the body wipe bug.
