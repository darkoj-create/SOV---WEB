# SOV web v6.1.44b — WordPress WXR text-only import

Baseline: `sov-web-build-v6.1.43e-smart-location-future-trips.zip`
Source: `sovelebit.wordpress.com.2026-06-24.000.xml`

## Što je napravljeno

- Uvezeno je 198 objavljenih WordPress postova kao text-only stranice u `novosti/`.
- Uvezeno je 14 objavljenih WordPress stranica kao text-only stranice u `legacy-wordpress/stranice/`.
- Generiran je novi `vijesti.html` s pretragom, filterom kategorije i filterom godine.
- Generiran je novi `index.html` s najnovijim objavama iz XML exporta.
- Generiran je `data/news.json` bez image URL-ova.
- Generiran je `data/legacy-wordpress-content.json` kao strukturirani indeks.
- Generiran je `data/legacy-wordpress-media-manifest.json` s 3440 media referenci za kasniju fazu.

## Što NIJE napravljeno

- Nisu preuzete slike, galerije, PDF-ovi ni ostali media fajlovi.
- Nema ubacivanja media foldera u Git.
- Nema SQL promjena.
- Nisu dirani Izleti, Oružarstvo, Arhivar, Karta, role, login ni APK kompatibilnost.

## Zašto ovako

Prethodni full media import je proizveo preko 2 GiB Git pack i tisuće binarnih fajlova. Ovaj build namjerno prenosi samo tekst i strukturu, a media ostavlja za fazu storage/servera.

## Kasnija media faza

Koristiti `data/legacy-wordpress-media-manifest.json` kao source-of-truth za skidanje, optimizaciju i upload slika na SOV server / R2 / drugi storage.
