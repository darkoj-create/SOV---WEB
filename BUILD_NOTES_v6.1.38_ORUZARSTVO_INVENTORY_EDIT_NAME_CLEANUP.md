# SOV web v6.1.38 — Oružarstvo inventory edit + name cleanup

## Problem
- Legacy return / inventory edit could write a movement or local UI state, but visible inventory count still did not reliably change.
- Root cause: some flows still used direct browser upsert into `equipment_items`, while the safe architecture requires SECURITY DEFINER RPC.
- Some Klaićeva import names were too literal from OCR/photo notes, e.g. `Stara stopa` instead of a normal article name.

## Fix
- Added SQL Build 4: `SUPABASE_ORUZARSTVO_V2_1_BUILD4_INVENTORY_EDIT_AND_NAME_CLEANUP.sql`.
- Added RPC `sov_armory_upsert_simple_item(...)` for web inventory count edits.
- Web `Uredi` now calls this RPC instead of direct `equipment_items` upsert.
- Legacy IDs are normalized by stripping `item:` prefix before RPC lookup.
- Legacy return RPC is replaced again with stricter lookup and counter updates.
- Catalog cache/manifest is invalidated after edits.
- Cleaned obvious OCR/import names:
  - `Stop sprava / stara stopa` → `Stop descender (za provjeru / oštećen)`
  - `Stara stopa` → `Stop descender (stari model)`

## Deploy order
1. Run SQL Build 4 in Supabase.
2. Deploy this web ZIP.
3. Hard refresh browser.
4. Test:
   - Oružar Master → Inventar → Uredi → change total/available count.
   - Oružar Master → Inventar → Povrat stare.
   - Search `stop descender`; the bad names should be gone.

## Not changed
- APK is not changed in this build.
- No broad browser write RLS was opened.
