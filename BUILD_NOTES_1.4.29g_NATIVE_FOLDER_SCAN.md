# SOV Admin 1.4.29g — Native folder scan hard fix

Popravljeno:
- app na startupu skenira `Download/SOV/Offline` i vraća datoteke u UI
- Offline ekran ponovno skenira foldere pri otvaranju
- GPX/KML/KMZ/GeoJSON/CSV/GPKG idu u importane slojeve
- KML/CSV se dodatno vraćaju u Moja baza ako sadrže točke
- MBTiles/PMTiles iz `mbtiles` i `maps` idu u offline/custom karte
- parent state se reload-a nakon scan-a, pa se stavke odmah vide bez restarta

Ne dirano:
- SQL
- Runner SQL scores
- Cloud login gate
- Laptop hub u Cloud gridu
