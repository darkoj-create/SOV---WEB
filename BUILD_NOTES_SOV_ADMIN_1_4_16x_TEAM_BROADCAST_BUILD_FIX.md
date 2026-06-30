# SOV Admin 1.4.16x — Team Broadcast Build Fix

Baseline: 1.4.16w-team-broadcast-messages

Fix:
- FieldPackageFeature.kt: nullable createdAt build error fixed with safe `.orEmpty()` before formatting message time.

Version:
- versionCode 900100
- versionName 1.4.16x-team-broadcast-build-fix

No web changes. No SQL changes beyond existing trip messages SQL.
