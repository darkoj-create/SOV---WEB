# SOV Web v6.1.45b — system status + posudbe hotfix

## Fix
- `system-status.html` exists at root.
- Added aliases: `status.html`, `sov-system-status.html`.
- Dashboard floating status link uses `/system-status.html` absolute path.
- Oružarstvo issue flow now calls `sov_armory_materialize_loan_from_request` after marking request issued; SQL trigger also handles it backend-side.

## SQL
`SUPABASE_ORUZARSTVO_LOANS_MATERIALIZER_v6_1_45b.sql` was applied live on Supabase on 2026-06-27.

## Regression notes
- Does not change request creation.
- Does not remove old request flow.
- Existing issued request was backfilled into `equipment_loans` and `equipment_loan_items`.
