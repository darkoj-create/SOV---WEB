# v5.20 Trip Sheet Sync Real Restore

Popravlja kalendar izleta tako da opet čita Google Apps Script/Sheet deployment robusno:
- pokušava više action varijanti: listTrips, getTrips, trips, readTrips, list i direktni endpoint
- podržava odgovore {trips}, {rows}, {data}, {items} i direktan array
- normalizira HR/EN nazive kolona: datum/date, voditelj/leader, lokacija/location, opis/description, cilj/goal, bbox, rasporedUrl
- ne prikazuje demo izlete kad Sheet padne, nego jasno pokazuje grešku
- live vijest iz v5.19 ostaje
