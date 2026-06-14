# SOV web v6.1.35 — Smart categories web visibility fix

Purpose: old 6.1.35 catalog remains the source of truth, but the web UI must show the new smart category taxonomy after the SQL category apply.

Changed only Oružarstvo web display logic:
- Prefer `category_name` from Supabase over old `xls_category` / old `main_category` fallback.
- Keep `xls_category` only as fallback so old rows still render.
- Update user catalog and Oružar master category cards/order/icons to the smart taxonomy.
- Keep inventory save RPC fix intact.
- Cache-bust affected JS files with `v=6.1.35-smartcat-webfix`.

Not changed:
- No database reset.
- No quantity changes.
- No item deletion.
- No automatic XLS import as main truth.
- Other modules are untouched.

Expected result:
- After running the SMART_CATEGORIES_APPLY_BY_NAME_FIXED.sql, web category cards should show: Osobni SRT komplet, Užad, Sidrišta i opremanje, Tehničko spašavanje i Čisto podzemlje, etc.
- If only the preview SQL was run, web remains effectively unchanged because no DB category values were written.
