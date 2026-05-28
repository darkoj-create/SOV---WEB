# BUILD NOTES v5.32 — Role manager

Datum: 2026-05-28

## Što je dodano

- Nova admin stranica `role-manager.html`.
- Role/permission matrica za budući unified/admin-capable SOV APK.
- Upravljanje korisnicima i rolama iz istog ekrana.
- APK permission JSON preview/copy.
- Novi SQL: `SUPABASE_SOV_ECOSYSTEM_ROLES_v5_32.sql`.
- Dashboard link `Role manager` vidljiv samo Adminu.
- `sync-status.html` i `admin-users.html` imaju link prema Role manageru.

## Što SQL v5.32 radi

- Dodaje/učvršćuje `sov_role_permissions`.
- Dodaje admin RLS policy za insert/update/delete permissiona.
- Dodaje profile policies za admin user/role management.
- Dodaje helper funkcije:
  - `sov_current_role()`
  - `sov_current_status()`
  - `sov_is_admin()`
  - `sov_has_permission(permission_name)`
- Dodaje viewove:
  - `sov_current_user_permissions`
  - `sov_role_manifest`

## Namjerno nije dirano

- SQL baza objekata.
- `speleo_objects_live_sql` / staging / promotion logika.
- Nacrti sync pipeline.
- Izleti GS.
- Oružarstvo data model.
- Vijesti editor logika.

## Preporučeni test

1. Deploy web v5.32.
2. U Supabase SQL editoru pokreni `SUPABASE_SOV_ECOSYSTEM_ROLES_v5_32.sql`.
3. Otvori `role-manager.html`.
4. Klikni `Učitaj sve`.
5. Promijeni jednu permission kvačicu i klikni `Spremi permissione`.
6. Vrati permission ako je test bio privremen.
7. Provjeri da `admin-users.html` i dalje normalno odobrava korisnike.
