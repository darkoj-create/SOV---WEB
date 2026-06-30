# SOV APK v1.4.20 — Role view check fix

Baseline: v1.4.19-crashlytics

## Changed
- HomeScreen: home cards are now filtered by the current SOV permissions instead of showing cards that only fail after tap.
- CloudScreen: module cards are role-aware; trips, archive and sync are shown only when the current role/permissions allow them.
- ToolsScreen: archive shortcut is shown only to Arhivar/Admin/Webmaster or roles with archive permissions.
- ArchiveDrawingsReadOnlyScreen: added role guard fallback if the route is opened directly without archive permissions.
- Kept Oružarstvo visible for approved members; management actions remain controlled by existing canManageEquipment logic.

## Not changed
- No SQL changes.
- No web changes.
- No repository changes.
- No Supabase RPC changes.
- No navigation architecture changes.
- Trips/tracking/broadcast logic unchanged.

## Validation
- Checked that role visibility is no longer only toast-based on Home/Cloud/Tools.
- ZIP integrity should be verified after packaging.
