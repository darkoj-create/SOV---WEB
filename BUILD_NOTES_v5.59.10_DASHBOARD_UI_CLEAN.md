# SOV Web v5.59.10 — Dashboard UI cleanup

Bez SQL/backend promjena. Build nastavlja na v5.59.9 kategorije oružarstva i v5.59.8 user approval sync fix.

## Promjene

- Dashboard header više ne koristi role-preview gumbe unutar glavnog nav bara.
- Role preview je premješten u statički toolbar ispod headera, samo za Webmastera.
- Desktop browser više nema sticky/floating gornji shell. Header je normalan dio stranice.
- Mobile browser/touch i dalje ima sticky/floating header i hamburger drawer.
- Desktop browser s manjom širinom ili zoomom više ne dobije automatski mobile/floating ponašanje.
- Dashboard kartice su očišćene: footer više nije absolute overlay pa tekst ne ulazi u “Otvori/Predaj”.
- Grid koristi auto-fit/min širinu umjesto forsirana 4 stupca, pa kartice ne budu prenatrpane.
- Hero sekcija i status kartica su kompaktizirane.
- Web build label na dashboardu podignut je na 5.59.10.

## Tehnički

- Dodan `assets/sov-dashboard-clean-v55910.css`.
- Patchani su shared shell/polish CSS overridei za desktop vs mobile browser behavior.
- Nema novih SQL datoteka.
