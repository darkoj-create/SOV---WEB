# SOV WEB build v0.8

Što je dodano:
- `data/sov-baza.json` kao statična JSON baza.
- `baza.html` kao SOV Karta web app.
- TK25 WMS je glavni sloj, OSM je fallback.
- Search i filteri po bazi.
- Export trenutno filtriranih/pronađenih objekata u KML.
- Označavanje područja na karti i generiranje MBTiles paketa u browseru.

Napomena za MBTiles:
Browser generira MBTiles pomoću sql.js i dohvaća TK25 tileove iz WMS-a. Ako Geoportal blokira CORS ili ako označiš preveliko područje/zoom, download može pasti. Za veće pakete kasnije je pametnije napraviti server-side generator.
