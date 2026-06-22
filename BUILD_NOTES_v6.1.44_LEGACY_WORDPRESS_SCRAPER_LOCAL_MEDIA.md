# SOV web v6.1.44 — Legacy WordPress scraper + lokalne slike

Baseline: `sov-web-build-v6.1.43e-smart-location-future-trips`.

## Što je dodano

- Dodan `tools/sov_wordpress_legacy_importer.py`:
  - `--rewrite-existing` prepisuje stare WordPress/PDS upload URL-ove na lokalne putanje.
  - `--download-assets` skida stvarne slike/datoteke u `assets/legacy-wordpress/` kad postoji internet.
  - `--import-rest` povlači javne WordPress postove/stranice kroz REST API i renderira ih u SOV stilu.
- Dodan `tools/README_SOV_WORDPRESS_LEGACY_IMPORT.md` s uputama.
- Dodan `data/legacy-wordpress-media-manifest.json` s mapiranjem starih URL-ova na lokalne datoteke.
- Dodan lokalni folder `assets/legacy-wordpress/` s placeholderima za sve trenutno referencirane legacy slike/datoteke.

## Što je promijenjeno

- Svi javni HTML/JS/CSS/JSON/SQL asset linkovi koji su hotlinkali `sovelebit.wordpress.com/wp-content/uploads/...`, `i0.wp.com/sovelebit...` ili `www.pdsvelebit.hr/wp-content/uploads/...` prepisani su na lokalni `assets/legacy-wordpress/...`.
- U javnim člancima uklonjen je nepotreban stari WordPress permalink iz `data-carousel-extra` atributa.
- Dodan mali CSS helper `.legacy-source-note` za buduće automatski renderirane legacy članke.

## Namjerno nije dirano

- Izleti / kalendar / Gmail sync.
- Arhivar / karta / predane jame.
- Oružarstvo i user borrowing.
- Role sustav, dashboard i APK-kompatibilni feedovi.

## Važno

Ovaj build više ne prikazuje slike direktno sa starog WordPressa. Trenutno su unutra neutralni placeholderi zato što ovaj runtime nema direktan internet/DNS pristup za masovno skidanje slika. Na deployment računalu/serveru pokrenuti:

```bash
python tools/sov_wordpress_legacy_importer.py --root . --download-assets --overwrite-media
```

Nakon toga placeholder datoteke će biti zamijenjene stvarnim slikama.
