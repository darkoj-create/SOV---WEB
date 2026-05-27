# v5.18 Trip map area export real fix

- Kalendar izleta → Karta sada šalje bbox offline područja (`minLat/maxLat/minLon/maxLon`) u `baza.html`.
- `baza.html` prepoznaje bbox iz URL-a, crta pravokutnik, zooma na njega i filtrira objekte samo unutar tog terena.
- Export KML na Baza stranici izvozi vidljive objekte iz tog područja.
- Skini mapu (.mbtiles) koristi isti bbox kao Map download područje.
- Ako izlet nema offline bbox, fallback je centar lokacije.
