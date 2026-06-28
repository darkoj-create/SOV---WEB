# v4.53 — Simplified Oružar Inventura

- Inventura je pojednostavljena na: prebroji, upiši/povećaj/smanji broj, potvrdi stanje.
- Nema QR/SKU obaveze za količinske artikle.
- Novi artikl ide kroz wizard: naziv → kategorija → podkategorija → broj komada → opcionalni kod → lokacija/minimum/napomena.
- Spremanje novog artikla koristi Supabase createEquipmentItem ako je baza aktivna, inače lokalni prikaz za test.
- User katalog ostaje jednostavan: kategorija → podkategorija → artikl → Zatraži.
