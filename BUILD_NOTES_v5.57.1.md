# SOV Web v5.57.1 — Arhivar base-status fix

- Arhivar worklist više ne zaključuje da objektu fali nacrt/zapisnik zato što nema upload u novoj SOV arhivi.
- Status se primarno računa iz postojeće speleo baze: `record_status`, `cadastre_status`, `field_tasks`, `workflow_raw`, koordinate i raw metadata.
- Uploadani nacrti/zapisnici u SOV arhivi prikazuju se kao privitci/brojila, ali nisu glavni dokaz za katastar.
- Dodan jasniji tekst u detalju objekta i status formi.
- RLS/SQL baza objekata se ne dira destruktivno.
