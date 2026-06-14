# v4.91 — Speleo Baza nacrti sync za sve usere

- Svi useri mogu kliknuti `Sync nacrte iz arhive` u Speleo Bazi.
- UI više ne prikazuje Drive folder link.
- Sync povlači popis fileova iz službene Drive arhive preko konfiguriranog endpointa/API keya.
- Fileovi se fuzzy/partial matchaju s objektima po imenu.
- Match se sprema u `speleo_object_drawings`.
- Detalj objekta prikazuje povezane nacrte.

SQL: `SUPABASE_SPELEO_NACRTI_SYNC_v4_91.sql`
Apps Script helper: `GOOGLE_APPS_SCRIPT_NACRTI_SYNC_v4_91.gs`
