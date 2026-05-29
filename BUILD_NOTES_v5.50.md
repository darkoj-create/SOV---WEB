# SOV Web v5.50 — Izleti simple Cloud UI

- `izleti.html` više nije stari wizard s kartom, odabirom područja, točkama, GPX/KML importom i exportima.
- Izrada izleta ide kroz `izleti-cloud.html` kao jednostavni Supabase-backed sheet view.
- U formi ostaju samo stvarni podaci izleta: datum, voditelj, lokacija, cilj, opis, status, vidljivost i grad za prognozu.
- `sov-trips-cloud.js` više ne šalje bbox/center/file metadata pri izradi izleta.
- Google Sheet / Apps Script ne vraćamo kao backend.
