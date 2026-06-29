# v5.24 — Trip calendar Apps Script architecture hardfix

- Kalendar izleta sada forsira ispravan Apps Script deployment endpoint.
- Resetira stari/bad lokalno spremljeni endpoint pri prvom učitavanju ove verzije.
- Refresh više ne koristi zamrznuti `TRIPS_WEBAPP_URL`, nego svaki put čita aktivni endpoint.
- Učitavanje pokušava više podržanih actiona: `listTrips`, `getTrips`, `trips`, `list`, `read`, i fallback bez actiona.
- Podržava više oblika odgovora iz Apps Script-a: `trips`, `data`, `rows`, `items`, `values`, ili direktan JSON array.
- Normalizira Sheet polja: datum/voditelj/lokacija/opis/cilj + bbox/raspored/prijavljeni.
- Uklonjen demo fallback koji je skrivao pravi problem.
- Kreiranje izleta (`izleti.html`) koristi isti ispravni endpoint.

Endpoint:
https://script.google.com/macros/s/AKfycbybGi7p6_ImXAXEErJ6P9K0GYHy8lHW850K9cQe2py8yUV2oJO6UW1DJi00quorVTHOGQ/exec
