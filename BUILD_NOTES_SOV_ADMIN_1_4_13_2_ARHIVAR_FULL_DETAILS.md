# SOV Admin APK 1.4.13.2 — Arhivar full details

- Arhivar detail card now shows full object context from the Supabase worklist view.
- Includes base speleo text, report/research text, drawing attachments and raw source record when available.
- Cache key bumped so old thin arhivar worklist does not hide new detail fields.
- No destructive DB changes. Requires SQL `SUPABASE_SOV_ARHIVAR_WORKFLOW_v5_57_2_FULL_DETAILS_FIX.sql`.
