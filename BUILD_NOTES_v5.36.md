# SOV Web v5.36 — Oružarstvo canonical role/audit

Baseline: v5.35 audit devices control.

Scope:
- Web only. APK untouched.
- Oružarstvo is integrated with unified permissions (`can_manage_equipment`).
- New safe SQL: `SUPABASE_ORUZARSTVO_v5_36_CANONICAL_ROLE_AUDIT.sql`.
- Existing object SQL/base logic is not changed.

Deploy order:
1. Upload web build.
2. Run SQL in Supabase once.
3. Test as Admin, Oružar and User.

