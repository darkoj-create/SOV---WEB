# SOV Admin v1.4.29j — SheetTripCard UI reorganizacija

Promjena je UI-only u `FieldPackageFeature.kt`.

## Što je promijenjeno
- `SheetTripCard` prošireni prikaz reorganiziran je u naslovljene sekcije:
  - Sažetak
  - Info i prijave
  - Teren
  - Materijali
  - Upravljanje
- Prognoza je pomaknuta odmah ispod opisa.
- Jedan CTA `Prijave i prijevoz` zamjenjuje dva nejasna CTA-a.
- Sudionici i vozači više se ne dupliraju u proširenom dijelu; ostaju u sažetku.
- `TripAssetsCloudCard` ostaje interni expand/collapse, ali je vizualno odvojen u sekciji Materijali.
- `FieldTripMessagesEntry` je izdvojen kao jasan ulaz u poruke ekipe unutar sekcije Teren.
- Dodan `animateContentSize()` za glađi expand/collapse.
- Uklonjen mrtvi `if (false && ...)` LaunchedEffect blok.

## Što nije dirano
- Nema promjena u Supabase/sync/network sloju:
  - `FieldPackageSheetSyncClient.*`
  - `TripAssetCloudRepository.*`
  - `FieldTrackingLiteApi.*`
  - `SovFieldHubClient.*`
- Potpis `SheetTripCard(...)` ostaje kompatibilan s postojećim pozivima.
- Dialog logika nije mijenjana: `FieldPackageSignupDialog`, `FieldPackageTransportDialog`, `FieldTripMessagesDialog` ostaju isti.

## Build status u ovom sandboxu
`./gradlew :app:compileDebugKotlin --no-daemon` nije mogao završiti jer sandbox nema internet i Gradle wrapper ne može skinuti `https://services.gradle.org/distributions/gradle-8.7-bin.zip`.

Izvor je patchan i spreman za Android Studio / lokalni Gradle build.
