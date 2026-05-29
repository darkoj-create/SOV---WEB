# SOV Web v5.51 — Izleti optional attachments

- Izleti ostaju jednostavan Cloud Sheet View; karta/točke/trackovi nisu obavezni wizard.
- Dodan optional section "Dodaci po želji" za GPX/KML/KMZ/GeoJSON/PDF/ZIP/slike.
- Upload ide u Supabase Storage bucket `sov-trip-files` i metadata u `sov_trip_files` preko RPC `sov_add_trip_file`.
- Tablica prikazuje GPX/KML count i gumb "Dodaci" za pregled vezanih fileova.
- Nema novog SQL-a; koristi postojeći Trips Cloud v5.49 SQL.
