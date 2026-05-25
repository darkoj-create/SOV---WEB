# v4.71 Clean Oružar Master stability rebuild
- Rebuilt Oružar Master, Inventar, Inventura and Posudba as clean standalone HTML pages.
- Removed inherited tab/overlay logic from these pages that caused content to appear and then disappear.
- Inventar uses one stable renderer: categories -> subcategories -> items.
- Posudba uses two stable cards: requested loans and issued loans.
- No SQL required for every action. v4.70 SQL/RLS only needs to be run once if requests are not visible between accounts.
