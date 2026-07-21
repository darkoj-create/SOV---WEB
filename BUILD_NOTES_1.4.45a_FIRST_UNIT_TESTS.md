# SOV Admin v1.4.45a — first unit tests

Bazirano na: `v1.4.44a-theme-picker`

## Cilj

Prvi mali testni sloj prije većih refactora. Ovo je FAZA 4 / 4.1 iz plana, ali bez masovnog diranja aplikacije.

## Promjene

- `versionCode = 900143`
- `versionName = "1.4.45a-first-unit-tests"`
- Dodani test dependencyji:
  - `junit:junit:4.13.2`
  - `kotlinx-coroutines-test:1.8.1`
- Izvučena čista verzijska logika u `util/AppVersionComparator.kt`.
- Izvučena čista search normalizacija u `util/SovSearchNormalizer.kt`.
- `AppUpdateManager` sada koristi `compareAppVersionNames(...)` bez promjene ponašanja self-updatea.
- `MainViewModel` sada delegira postojeću search normalizaciju na `normalizeSovSearchText(...)`, bez promjene izlaza.

## Testovi

Dodano ukupno 20 unit testova:

- `CoordinateConverterTest`
  - HTRS96/TM central meridian sanity check
  - Zagreb/Split stabilne referentne vrijednosti
  - round-trip WGS84 → HTRS96/TM → WGS84 za Zagreb/Rijeku/Pulu/Dubrovnik
  - finite check za rubni input izvan Hrvatske
- `AppVersionComparatorTest`
  - numerička usporedba verzija
  - `v` prefix
  - sufiksi tipa `1.4.33a-armory`
  - dodatni build suffix brojevi
  - blank/non-numeric fallback
- `SovSearchNormalizerTest`
  - dijakritike
  - đ/Đ
  - whitespace/punctuation
  - compact token
  - null/blank

## Nije dirano

- Supabase
- self-update ponašanje / hash provjera nije vraćena
- UI
- tema
- manifest
- Apps Script
- network/security guard

## Lokalna provjera

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

Napomena: u sandboxu Gradle wrapper i dalje pada na DNS/download (`services.gradle.org`), pa lokalni Android Studio build ostaje autoritativan.
