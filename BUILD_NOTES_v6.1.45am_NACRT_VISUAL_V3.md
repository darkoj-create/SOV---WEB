# SOV Web v6.1.45am — Nacrt Visual v3

## Opseg

Vizualni sloj za `nacrt.html`, izgrađen preko stabilnog v2 parsera, geometrije i rasporeda.

## Promjene

- Automatska tonska ispuna špiljskog prostora i kada TopoDroid crtež nema `area` zapise.
- Posebne palete za profil, tlocrt i presjek.
- Dubinsko sjenčanje profila: svjetliji ulaz, tamniji dublji dijelovi.
- Deterministička zrnata tekstura bez vanjskih slika ili mrežnih resursa.
- Diskretni SVG relief i sjene koje ostaju čitljive u SVG i PNG izvozu.
- Bogatiji prikaz blokova, sipara/debrisa, stupova i ulaza.
- Presjeci s `pit` obrisom također dobivaju ispunu.
- Footer označen kao `SOV Nacrt Generator v3`.

## Sigurnost promjene

- `nacrt-core.js`, `nacrt-tdr-fix.js` i `nacrt-v2.js` nisu mijenjani.
- V3 samo omata `NacrtRenderer.render` nakon v2.
- Ako nema Canvas podrške ili nema zidnih linija, v2 izlaz ostaje funkcionalan.
- Nema SQL promjena.

## Provjera

Testirano na `Jama_u_koritima_1-1s(2).zip`:

- profil, tlocrt i presjek se parsiraju;
- automatske ispune prate zidne obrise;
- blokovi, debris i stupovi se prikazuju;
- generirani SVG nema JavaScript sintaksnih pogrešaka;
- SVG i PNG ostaju samostalni, bez vanjskih tekstura.
