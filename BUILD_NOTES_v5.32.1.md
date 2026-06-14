# SOV web v5.32.1 — Role manager SQL view type fix

Fixes Supabase SQL error:

`ERROR: 42P16: cannot change data type of view column "status" from sov_user_status to text`

Cause: `CREATE OR REPLACE VIEW` cannot change an existing view column type.

Fix: SQL now drops/recreates `sov_current_user_permissions` and `sov_role_manifest` views before creating them. This does not delete data tables.
