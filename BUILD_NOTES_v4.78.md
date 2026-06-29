# v4.78 — Import schema compatibility fix

Fixes Supabase/PostgREST error:
`Could not find the 'category' column of 'equipment_items' in the schema cache`

Root cause: some legacy UI/import rows still carried display alias `category` while real DB column is `category_name`.

Changes:
- hard sanitizer before every Supabase upsert
- `equipment_items` now sends only real DB columns
- cache-busted `assets/oruzarstvo-supabase.js?v=4.78`
- SQL compatibility file included for simplified-model columns + open preview RLS

Run once if needed:
`SUPABASE_ORUZARSTVO_v4_78_SCHEMA_COMPAT_OPEN_PREVIEW_FIX.sql`
