# SOV Admin APK 1.4.5.2 — Armory sync unblock

Fixes Oružarstvo refresh getting stuck/disabled.

- Refresh button is never permanently disabled.
- Forced refresh cancels/restarts stuck sync.
- Equipment sync has a 60s timeout and always clears loading state.
- Shows a clear sync error instead of leaving the UI greyed out.
- No SQL changes.
