# SOV Web v5.33 — Role Control Center

Bazirano na `v5.32.1-role-manager-sql-view-type-fix`.

## Što je promijenjeno
- Web više nije full-open preview po defaultu; koristi pravi Supabase auth/session flow.
- Preview role switcher je opt-in: `?preview=1` ga uključi, `?preview=0` ili × ga ugasi.
- `SOVAuth.can()` sada prvo koristi `sov_current_user_permissions` i permission matricu iz Supabasea.
- Dashboard gating je usklađen s rolama:
  - Admin: korisnici, role manager, SQL alati, sync status.
  - Urednik: news/article tools.
  - Oružar: oružarstvo i inventure.
  - Arhivar: zahvati/arhiva/nacrti.
  - Član: javni/field approved alati.
- SQL floating shortcuti su sakriveni svima osim adminu s SQL permissionom.
- Role manager APK manifest sada nosi verziju `5.33`.

## Važno
Ako Supabase još nema role permission view, prvo pokreni postojeći SQL:
`SUPABASE_SOV_ECOSYSTEM_ROLES_v5_32_1_VIEW_TYPE_FIX.sql`

Ovaj build ne mijenja živu SQL bazu objekata i ne dira staging/live object logiku.
