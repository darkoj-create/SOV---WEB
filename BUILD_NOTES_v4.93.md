# v4.93 — Inline Nacrti Viewer + Proper Sync Button

- U Baza toolbar dodan jasan gumb `Sync nacrte` za admin/arhivar.
- Maknut je stari toolbar link prema TopoDroid/Dokumenti iz Baza sekcije.
- U detalju objekta nacrti se prikazuju inline kao thumbnail grid.
- Klik na thumbnail/gumb otvara full view modal/lightbox.
- Useri ne vide Drive folder link i ne idu u Documents sekciju.
- Sync je zaključan na admin/arhivar; svi useri mogu vidjeti već povezane nacrte.
- Dodani sigurni fallback helperi za role check i object overrides da Baza ne pukne ako nema Supabase responsea.
