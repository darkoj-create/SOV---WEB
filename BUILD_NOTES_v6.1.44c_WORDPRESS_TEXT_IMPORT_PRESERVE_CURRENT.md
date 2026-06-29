# SOV web v6.1.44c — WordPress text import, current public site preserved

Baseline: `sov-web-build-v6.1.43e-smart-location-future-trips.zip`  
Source: `sovelebit.wordpress.com.2026-06-24.000.xml`

## Što je napravljeno

- Zadržana je postojeća naslovnica i postojeće aktualne objave sa slikama.
- Zadržane su postojeće javne stranice: `o-drustvu.html`, `povijest.html`, `speleoskola.html`, `velebitaski-duh.html`, `procelnistvo.html`, `velebiten.html`, `pridruzi-nam-se.html`.
- Iz WordPress XML-a dodani su samo postovi koji nisu već imali modernu `novosti/*.html` stranicu.
- Generirano novih text-only post stranica: 187.
- Sačuvano postojećih modernih post stranica: 11.
- `vijesti.html` je sada normalna stranica “Sve objave” s pretragom/filterima, ne tehnička arhiva.
- Media/slike nisu bulk-importane; samo su evidentirane u `data/legacy-wordpress-media-manifest.json` za kasniju fazu.

## Što nije dirano

- Nisu pregažene stranice “O nama”, “Povijest”, “Speleoškola”, “Velebitaški duh”, “Pročelništvo”, “Velebiten”.
- Nije dodan `assets/legacy-wordpress/` media folder.
- Nije diran SQL, Supabase, Izleti, Oružarstvo, Arhivar, Karta, login, role ni APK kompatibilnost.

## Namjera

Ovo nije “stari web / tehnička arhiva” build. Ovo je normalan SOV web kojem su dodane starije objave kao tekstualne stranice, dok aktualni vizualni dio ostaje kakav je bio.
