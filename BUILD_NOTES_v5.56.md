# SOV Web v5.56 — Izleti prijave + prijevoz

## Fokus
- Detalji izleta postaju mini-centar izleta: opis, prognoza, prijave, prijevoz i najava maila.
- Dodan flow za prijavu člana i automatsku tablicu prijevoza.

## Dodano
- Gumb **Prijavi se** u detalju izleta.
- Modal prijave:
  - ime i prezime
  - email/kontakt
  - status: idem / možda / ne idem
  - prijevoz: trebam prijevoz / imam auto / sam se snalazim
  - slobodna mjesta
  - mjesto polaska
  - napomena
- Popis prijavljenih u desnom detail panelu.
- Automatska tablica prijevoza.
- Export tablice prijevoza u CSV.
- SQL helper `sov_trip_signup(...)` i view `sov_trip_members_transport_view`.

## SQL
Pokrenuti:
`SUPABASE_SOV_TRIPS_SIGNUPS_TRANSPORT_v5_56.sql`

## Nema promjena
- Stari Google Sheet nije vraćen kao backend.
- GPX/KML dodaci ostaju opcionalni.
- Izrada izleta ostaje jednostavna.
