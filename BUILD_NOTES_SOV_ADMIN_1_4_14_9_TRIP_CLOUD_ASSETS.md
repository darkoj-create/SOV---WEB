# SOV Admin 1.4.14.9 — Trip Cloud Assets

## Cilj
Dodaje neinvazivni cloud paket za aktivne izlete: offline karta + GPX/KML/trackovi + TopoDroid privitci mogu se objaviti kao `.sovpkg` paket vezan uz cloud izlet. Članovi izleta ga mogu otvoriti/preuzeti iz appa.

## UX
- U kartici izleta dodana je sekcija **Paketi za teren**.
- Oružar/Arhivar/Admin/User na izletu vidi zajedničke pakete ako je prijavljen.
- Voditelj/Admin može kliknuti **Objavi paket**.
- Član klikne **Skini i otvori** i paket se lokalno importira.

## Tehnika
- App exporta postojeći SOV field package `.sovpkg` preko postojećeg `FieldPackageManager.exportPackage()`.
- Paket može sadržavati offline segment karte, spremljene GPX/KML/trackove i TopoDroid privitke, ovisno što je lokalno uključeno u paket.
- Upload ide u Supabase Storage bucket `sov-trip-assets`.
- Metadata ide u tablicu `sov_trip_assets`.
- SQL v5.59.6 dodaje cleanup za pakete nakon završetka izleta.

## Verzija
- versionCode: 900066
- versionName: 1.4.14.9-trip-cloud-assets
- expected APK: SOV-ADMIN-1.4.14.9.apk

## Potreban SQL
Pokrenuti `SUPABASE_SOV_TRIP_ASSETS_v5_59_6.sql` prije korištenja.
