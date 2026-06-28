# SOV Web v6.1.45d — Oružarstvo loan/shop workflow

Scope: UI/functionality polish for oružarstvo borrowing flow.

## Changed
- Added `assets/oruzarstvo-loan-shop-v6145d.css`.
- Added `assets/oruzarstvo-loan-shop-v6145d.js`.
- Injected the loan-shop layer into `oruzarstvo.html` and `oruzar-master-posudbe.html`.
- Member side now behaves like a webshop: catalog → cart/request → sent request status.
- Request drawer now has checkout flow, item counter and sticky floating cart button.
- `Moji zahtjevi` now displays order cards with a clear status pipeline.
- Oružar posudbe now shows a fulfillment board: `Za izdati`, `Izdano vani`, and `Zatvoreno`.
- Manual request entry remains available for live/phone/society-room requests.

## Not changed
- No SQL changes.
- No Supabase schema changes.
- Existing request status updates still call existing `setReqStatus` / `CleanArmory.setStatus` / `SOVArmoryDB.updateRequestStatus`.
- Backend materializer from prior step still handles issued → loan table materialization.
