# SOV Web v5.38 — TopoDroid Import Pipeline

Base: v5.37 Arhiva/Nacrti canonical.

Dodano:
- `topodroid-import.html` batch import/review stranica
- SQL `SUPABASE_TOPO_DROID_IMPORT_PIPELINE_v5_38_SAFE.sql`
- import batch tablice: `sov_topodroid_import_batches`, `sov_topodroid_import_items`
- RPC `sov_publish_topodroid_import_item(item_id uuid)` za objavu u `speleo_object_drawings`
- RLS Admin/Arhivar write, ostali read-only kroz postojeći public drawings view
- link iz `topodroid.html` i dashboarda

Ne dira bazu objekata. RLS ostaje uključen.
