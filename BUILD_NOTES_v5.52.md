# SOV Web v5.52 — Izleti opis izleta

- Dodano/istaknuto polje **Opis izleta** u jednostavnom Cloud unosu izleta.
- Opis je jasno odvojen od opcionalnih dodataka GPX/KML/track/karta.
- Tablični prikaz sada prikazuje **Opis izleta** umjesto generičkog opisa.
- Search traži i po opisu izleta.
- Nema novog SQL-a; koristi postojeći `description` stupac iz `sov_trips`.
