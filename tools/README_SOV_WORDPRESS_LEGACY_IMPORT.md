# SOV legacy WordPress import

Ovaj build više ne hotlinka slike sa starog WordPressa u javnom webu. Postojeće slike su prepisane na lokalne putanje `assets/legacy-wordpress/...`, a izvorni URL-ovi su ostavljeni samo u `data/legacy-wordpress-media-manifest.json` da ih importer/downloader može povući.

## 1) Lokaliziraj postojeći build

```bash
python tools/sov_wordpress_legacy_importer.py --root . --rewrite-existing
```

## 2) Skini stvarne slike umjesto placeholdera

Pokrenuti u okruženju koje ima internet:

```bash
python tools/sov_wordpress_legacy_importer.py --root . --download-assets --overwrite-media
```

## 3) Povuci dodatne stare objave/stranice kroz WordPress REST API

```bash
python tools/sov_wordpress_legacy_importer.py --root . --import-rest --download-assets
```

Importer koristi WordPress REST API (`/wp-json/wp/v2/posts` i `/wp-json/wp/v2/pages`) i piše nove objave u `novosti/`, a statične stare stranice u `legacy-wordpress/` da ne pregazi postojeće SOV module.

## Sigurnosno ponašanje

- Ne pregazi postojeće HTML objave bez `--overwrite`.
- Ne pregazi skinute slike bez `--overwrite-media`.
- Slike se spremaju lokalno pod `assets/legacy-wordpress/`.
- Stari URL-ovi ostaju samo u manifestima, ne u prikazu stranica.
- Izleti, arhivar, oružarstvo, korisnici i APK-kompatibilni dijelovi se ne diraju.
