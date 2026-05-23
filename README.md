# SOV WEB build v0.9 full-site-fixed

Ovaj ZIP je full site build, ne samo registered portal.

Sadrži:
- javni portal: index.html
- o društvu: o-drustvu.html
- povijest: povijest.html
- Velebitaški duh: velebitaski-duh.html
- login + dashboard
- SOV Karta: baza.html
- JSON baza: data/sov-baza.json

Popravci:
- SOV Karta koristi DGU TK WMS endpoint kao glavni sloj:
  https://geoportal.dgu.hr/services/tk/wms
- OSM je samo fallback i jasno piše kad je aktivan.
- Statusi objekata:
  zelena = u katastru
  crvena = nije u katastru / za unos
  siva = na provjeri / nije objekt / nejasno
- Export filtriranih objekata u KML.
- Označavanje offline područja generira manifest za MBTiles backend/script.

Napomena:
Browser sam ne može pouzdano generirati pravi raster MBTiles iz DGU WMS-a bez backend procesa.
Zato `baza.html` generira bbox manifest koji backend/script može pretvoriti u .mbtiles.
