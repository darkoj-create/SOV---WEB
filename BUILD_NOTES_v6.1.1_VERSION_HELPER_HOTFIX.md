# SOV web v6.1.1 — Version helper hotfix

## Why
The v6.1.0 package still contained an old `assets/sov-version.js` hardcoded to `6.0.5`. That helper patched page labels back to 6.0.5 after load, which made the deployed web look like it had reverted.

## Changes
- Rebuilt `assets/sov-version.js` with v6.1.1 fallback and dynamic `update.json` manifest reading.
- Updated `update.json`, `VERSION.txt`, and `BUILD_VERSION.txt` to v6.1.1.
- Updated HTML `assets/sov-version.js?v=...` cache-bust references to v6.1.1.
- No database changes.

## Deploy
Copy the entire ZIP over the repo and push. After deploy, open with cache bypass:

- `/update.json?cb=611999`
- `/assets/sov-version.js?v=611999`
- `/sync-status.html?b=611999`
