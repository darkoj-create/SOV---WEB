# SOV web v4.32 — Oružarstvo duplicate SKU import fix

Popravak importa oružarstva:
- `equipment_ropes` se više ne importa samo po `legacy_id` conflict keyu.
- Užad sada koristi `sku` kao primarni conflict key jer je `sku` unique fizički identifikator užeta.
- Import deduplicira užad po SKU prije slanja u Supabase.
- Redovi bez SKU fallbackaju na `legacy_id`.
- Time se uklanja greška: `duplicate key value violates unique constraint "equipment_ropes_sku_key"`.

Vizualni polish iz v4.31 ostaje uključen.
