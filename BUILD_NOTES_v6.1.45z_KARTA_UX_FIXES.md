# SOV Web v6.1.45z — Karta UX fixes

Promjene samo na `karta.html` + routing fallback. Nema SQL promjena.

- automatski `fitBounds()` na sve valjane markere nakon učitavanja baze
- default view na Hrvatsku ako nema valjanih koordinata
- `0,0`, prazne, `null`, `NaN` i out-of-range koordinate više se ne renderiraju kao marker
- brojač “s koordinatama” koristi isti validator kao marker renderer
- aktivni bazni sloj sada dinamički prikazuje `TK25/DOF/HOK/OSM aktivan`
- custom pillovi i Leaflet layer control sinkroniziraju aktivno stanje
- toolbar/zoom kontrole razmaknute da TK25 pill ne bude odrezan
- rezultati u listi sada prvo prikazuju ime objekta, status je manji ispod
- mobile layout: karta puna širina, panel ispod mape
- dodan fallback/redirect za `Karta.html` -> `karta.html`
