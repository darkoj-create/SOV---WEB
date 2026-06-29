# v5.12 — SQL Object Hub / linked modules sandbox

Sigurni nastavak SQL migracije iz v4.99/v5.09/v5.11:
- live JSON baza se ne dira
- nova stranica `speleo-sql-object-hub.html`
- po objektu iz `speleo_objects_staging` prikazuje:
  - pregled objekta
  - nacrte/dokumente/linkove
  - zahvate/radnje
  - audit log
- nove staging tablice:
  - `speleo_object_links_staging`
  - `speleo_object_reports_staging`
  - `speleo_object_audit_staging`

Ovo je priprema za vezanje nacrta, zahvata i edit historyja na stabilni `source_id` prije stvarnog SQL live prebacivanja.
