# SOV Admin/Unified APK v1.2.0 — role preview

This build is the first Android step toward one unified APK controlled by Supabase roles.

## Added
- Supabase auth login inside Settings.
- Fetches `sov_current_user_permissions` after login.
- Stores role/permission snapshot locally for offline field use.
- Shows permission pills: SOV, Katastar, Edit, Nacrti, Izleti, Oružar, Admin.
- Search source picker starts respecting `can_view_katastar` from the cached permission snapshot.

## Kept
- Admin package id remains `com.darko.speleov1admin`, so it updates over the existing admin APK when signed with the same keystore.
- Fast drawings v2 endpoint remains active.
- Izleti fixes remain active.
- No SQL object database logic was changed in the app source.

## Required before testing roles
- Web v5.32.1 role SQL must be successfully applied.
- User must exist in `profiles`, be `approved`, and have a role that exists in `sov_role_permissions`.

## APK file name
`SOV-ADMIN-1.2.0-unified-role-preview.apk`
