# SOV web build v6.1.44d — WordPress featured media import

Baseline: `sov-web-build-v6.1.44c-wordpress-text-import-preserve-current-public.zip`

## Što je napravljeno

- Dodan je optimizirani media paket iz WordPress TAR exporta.
- Za stare WordPress objave koje su u `44c` bile text-only dodana je jedna naslovna fotografija po objavi gdje je dostupna.
- `vijesti.html` sada prikazuje thumbnailove za stare objave umjesto praznih/no-photo kartica.
- Generirane stare `novosti/*.html` stranice sada imaju veliku optimiziranu naslovnu fotografiju u članku.
- Dodano je 6 manjih dokumenata iz WordPress media exporta u `documents/legacy-wordpress/`.
- Dodani su map/audit fajlovi u `data/` radi kasnijeg punog media importa.

## Što NIJE mijenjano

- Naslovnica i postojeće moderne objave ostaju sa slikama kako su bile.
- Nisu uvezene pune galerije.
- Originalni 2.4 GB TAR nije dodan u build.
- Nije dodan `assets/legacy-wordpress/` folder s originalima.
- Nisu dirani Izleti, Oružarstvo, Arhivar, Karta, Gmail sync, SQL, role/login ni APK kompatibilnost.

## Brojevi iz media paketa

```text
SOV featured-only media package
================================
TAR: D:\SOV_MEDIA\media-export-125847801-from-0-to-6853.tar
Selected posts: 187
Featured images created: 181
Missing featured images: 6
Errors: 0
Documents copied: 6 / 12.3 MB

Original source size for selected images: 181.3 MB
Optimized article images: 32.5 MB
Optimized thumbnails: 6.8 MB
Compression ratio article: 17.9% of original
Compression ratio thumbnail: 3.8% of original

Generated:
  assets/legacy-wordpress-featured/
  assets/legacy-wordpress-thumbs/
  documents/
  data/legacy-featured-media-map.json
  featured_images.csv
  missing_featured_images.csv
  documents.csv

ZIP this output folder and upload it to ChatGPT.
```

## Lokalni Git/Vercel napomena

Ovaj build nosi samo optimizirane WebP slike i male dokumente. Originalni TAR i dalje mora ostati izvan repozitorija.

U repo ne smije ići:

```text
media-export-125847801-from-0-to-6853.tar
assets/legacy-wordpress/
```

Preporučeni `.gitignore` dodaci:

```text
*.tar
*.tar.gz
assets/legacy-wordpress/
```
