# SOV web v6.1.3 — sync-status + system health hotfix

Fixes two issues observed after v6.1.2 deployment:

- `sync-status.html` still compared `update.json.cacheBust` against `610`, so v6.1.2 showed a false Build/cache mismatch.
- `sov_system_health()` could report `profiles / pending ERR · column "approval_status" does not exist` on Supabase projects where `profiles` has `status` but not `approval_status`.

No destructive database changes. The SQL hotfix only replaces health helper functions/RPC.
