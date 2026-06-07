# SOV web v4.92 — Admin/Arhivar Nacrti Sync + Public Drawings View

Promjena logike nacrta:
- sync nacrta više nije za svakog usera, nego za admin/arhivar workflow
- svi useri vide već syncane nacrte u detalju objekta
- objekt prikazuje thumbnail nacrta
- klik otvara full view/lightbox
- Drive folder link se ne prikazuje u UI-u
- lokalni upload nacrta maknut iz glavnog user flowa

Potrebno:
1. Pokrenuti `SUPABASE_SPELEO_NACRTI_SYNC_v4_92_ADMIN_SYNC_PUBLIC_VIEW.sql`
2. Deployati `GOOGLE_APPS_SCRIPT_NACRTI_SYNC_v4_92.gs`
3. U `assets/supabase-config.js` upisati `/exec` URL u `window.SOV_DRAWINGS_SYNC_ENDPOINT`
4. Admin/arhivar klikne `Sync nacrte iz arhive`
