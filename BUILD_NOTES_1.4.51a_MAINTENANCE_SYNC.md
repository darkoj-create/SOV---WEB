# SOV Admin v1.4.51a — maintenance sync

## Verzija

```kotlin
versionCode = 900149
versionName = "1.4.51a-maintenance-sync"
```

## Napravljeno

1. **Update pipeline**
   - `update.json` je poravnat na aktualni build.
   - Dodan `RELEASE_CHECKLIST.md` da release asseti više ne zaostaju.
   - `AppUpdateManager.kt` sada tretira stariji `update.json` na GitHub releaseu kao `UpToDate`, uz `Log.i`, bez errora prema korisniku.

2. **Snackbar za terenske greške**
   - Dodan `SovMessenger` i spojen na postojeći `SnackbarHostState` kroz `SpeleoAppRoot.kt`.
   - Migrirane su samo odabrane greške iz Toastova u Snackbar:
     - import/brisanje/prijava/prijevoz u `FieldPackageFeature.kt`
     - nacrti/download/open errors u `RecordDetailFeature.kt`
   - Potvrdni Toastovi poput kopiranja i dalje ostaju Toast.

3. **Oružarstvo offline asset audit**
   - Uspoređen `oruzarstvo-xls-canonical-v6.1.5.json` s `SUPABASE_ORUZARSTVO_CATEGORY_PRIORITY_ALIGN_v6_1_45ai.sql`.
   - Razlike u kategorijama/prioritetima nisu pronađene, JSON nije mijenjan.
   - Detalji su u `ARMORY_CATEGORY_AUDIT_1.4.51a.md`.

4. **Empty states**
   - Dodan `SovEmptyState` composable.
   - Primijenjeno na:
     - search no-results
     - arhivar predaje
     - offline karte/korisničke slojeve kada je sve prazno

5. **ImportParser testovi**
   - Dodani fixturei u `app/src/test/resources/import/` za KML/KMZ/GPX/GeoJSON/CSV/XLSX i korumpirane ulaze.
   - Dodan `ImportParserTest.kt`.
   - `ImportParser.kt` koristi `XmlPullParserFactory` umjesto `android.util.Xml` radi JVM unit testova.

6. **Web sitnica**
   - Dodan `bump-version.py` helper za statički web build.
   - Skripta ažurira query parametre za `sov-version.js` i `sov-client-logger.js` u HTML datotekama te fallback verziju u `assets/sov-version.js`.

## Namjerno nije dirano

- `SovHttpClient`
- `SovPermissionsStore`
- `SovNetworkSecurity`
- Supabase SQL/RLS
- self-update download/install logika osim stale-manifest fallbacka
- armory sync logika

## Build status

Build nije potvrđen u sandboxu jer Gradle wrapper ne može dohvatiti distribuciju:

```text
UnknownHostException: services.gradle.org
```

Lokalno pokrenuti:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```
