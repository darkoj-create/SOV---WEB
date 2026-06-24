# SOV Web v6.1.43a — vraćen zapisnik → Izleti flow

- Vraćena stranica `zapisnici-najave.html` koja je ispala iz v6.1.43 ZIP-a.
- Vraćen pregled Gmail importa, ručni DOCX import, parser sekcije NAJAVE, uređivanje i filteri.
- Vraćeni dedupe postupci: mogući duplikat, ipak novi izlet i potvrđeni duplikat.
- Odobrenje koristi postojeći `sov_approve_trip_announcement` RPC i stvara pravi red u `sov_trips`.
- Stranica je dostupna samo arhivaru, adminu i webmasteru.
- Nema novih SQL promjena; postojeći backend v6.1.40b/v6.1.41 ostaje izvor istine.
