# BUILD NOTES v5.30 — Drawings fast-search sync

- Web nacrti sync prebacen na SOV Drawings Index WebApp v2.0.2 FAST SEARCH.
- Novi endpoint: https://script.google.com/macros/s/AKfycbx1Hg_s6mAdWgB7p559USC8dAMIhteJQ3RFhFgp8rkqzYEVqMfwZm-lrl2v7UmW8gvSyg/exec
- Web vise ne ocekuje stari full `files` Drive scan kao primarnu logiku; cita cached index preko `?action=listDrawings&limit=2000`.
- Sync prihvaca samo obicne nacrte: PDF, JPG/JPEG, PNG, WEBP, TIF/TIFF.
- Izbaceni su TopoDroid/GPX/KML/ZIP/TDR/TH2 formati iz web nacrti sync filtera.
- Upsert ide samo u `speleo_object_drawings`; SQL baza objekata i live object update logika nisu dirani.
- Izleti endpoint i kalendar logika nisu dirani.
