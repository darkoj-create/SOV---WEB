# SOV Admin APK 1.3.0 — Identity + Sync Layer

Baza: `sov-admin-v1.2.2-sync-foundation-source.zip`

## Što je dodano
- `versionCode 900009`
- `versionName 1.3.0-identity-sync-layer`
- `SovCloudIdentity` helper iz postojećeg Supabase permission cachea
- `permissionSummaryHr()` za konzistentan prikaz prava u APK-u
- `SovOfflineSyncQueueStore` kao lokalna offline queue podloga za budući pravi cloud sync
- tipovi queue entiteta: trip, waypoint, track, news, drawing, object note, equipment
- operacije: create/update/delete/upsert

## Namjena
Ovo još ne forsira migraciju svih modula na Supabase. Dodaje sigurnu sredinu: APK može nastaviti raditi offline, a nove akcije se mogu postupno upisivati u queue i slati prema web/Supabase sloju.

## Build
U Android Studiju napravi signed APK istim admin keystoreom.
