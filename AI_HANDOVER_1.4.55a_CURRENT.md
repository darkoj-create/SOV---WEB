# AI HANDOVER — SOV Admin 1.4.55a current source

**Pripremljeno:** 2026-07-19  
**Otvoriti kao Android Studio projekt:** folder koji sadrži `settings.gradle.kts`  
**Bazna stabilna verzija:** `1.4.54a-dem-track-profile`, versionCode `900153`  
**Aktualna source verzija:** `1.4.55a-offline-nacrti-v3`, versionCode `900154`

## Važno o stanju builda

Claudeov zadnji postojeći `app-debug.apk` napravljen je 2026-07-19 oko 11:07 i sadrži 1091 nacrt, ali se pogrešno predstavlja kao 1.4.54a. Nakon tog APK-a source je dodatno mijenjan oko 15:52–16:43. Ovaj source paket uključuje te kasnije izmjene i još 5 nacrta, ukupno 1096.

Ovaj aktualni source nije ponovno kompajliran u ovom radnom okruženju jer ovdje nema Android SDK/Gradle dependency cachea ni mrežnog pristupa za Gradle wrapper. Prvi korak u Android Studiju je Gradle Sync + Clean + Build APK(s).

## Trenutačni sadržaj nacrta

- `app/src/main/assets/nacrti_bundled/`
- **1096 fizičkih nacrta** + `index.json`
- **886 logičnih drawing grupa**
- matcher v3: **782 assigned**, **41 review**, **63 unmatched**
- index i fizičke datoteke su provjereni: nema missing/extra datoteka
- 5 nacrta dodanih nakon zadnjeg APK-a:
  - `05-219 MR_skenirani nacrt.webp`
  - `MR_poligonski vlak_profil.pdf`
  - `Jama u šumi na pločama 1_nacrt.pdf`
  - `Jama u šumi na pločama 1_skenirani nacrt.pdf`
  - `Patkov gušt_nacrt.webp`

## Glavne izmjene prema 1.4.54a

### Offline nacrti i matching

- `DriveDrawingsRepository.kt`: bundled-first nacrti, Drive merge/fallback, cache indexa i matching.
- `RecordDetailFeature.kt`: thumbnailovi, viewer, OOM-safe subsampling i badge Nacrt/Bez nacrta.
- `MainViewModel.kt`, `SearchFeature.kt`, `SearchFilterController.kt`, `Models.kt`: searchable `onlyWithDrawing` filter i cached `hasBundledDrawing` flag.
- `MATCHING_REPORT_v3.md`, `match_nacrti_v3.py`, `match-overrides.json`: aktualni matcher workflow.

### Native Nacrt Generator

Novi paket `app/src/main/java/com/darko/speleov1/nacrt/`:

- `NacrtZipParser.kt`: otvara TopoDroid export ZIP i traži `manifest`, `survey.sql` i `.tdr` datoteke.
- `SurveySqlParser.kt`: parsira mjerenja, noge/splayeve, računa koordinate, duljinu, horizontalu i dubinu.
- `TdrParser.kt`: parsira TopoDroid TDR binarni format za plan/profil, linije, područja, simbole i stanice.
- `NacrtRenderer.kt`: render nacrta.
- `NacrtScreen.kt`: UI za import, plan/profil, statistiku, PNG spremanje i dijeljenje.
- `HomeAndToolsScreens.kt` i `SpeleoAppRoot.kt`: ulaz u Nacrt Generator i viewer nacrta objekta.

### DEM tracking

- `TrackPoint` sada ima `demAltitudeM`.
- `ElevationRepository.elevationAtBlocking()` daje DEM visinu tracking servisu.
- `TrackingForegroundService`, `TrackingRuntime`, `FieldTrackingLiteStore` i `UserContentStore` spremaju GPS i DEM visinu.
- `TrackElevationProfileFeature.kt` koristi prošireni profil.

### Baza

- `baza_velebit_2026_android_v2.json.gz`: ažurirana baza s 1750 zapisa.
- `update_sov_baza.py`: ETL za Sjeverni Velebit 2026.
- Supabase staging SQL iz starog handovera još nije potvrđeno izvršen.

## Prvi testovi u Android Studiju

1. Gradle Sync mora završiti bez greške.
2. Clean Project i Build APK(s).
3. Airplane mode → objekt s nacrtom → thumbnail i fullscreen.
4. Test PDF nacrta: Vražja jama.
5. Test više stranica: Lubuška jama.
6. Search → filter samo objekti s nacrtom.
7. Cloud → Nacrt Generator → import oba poznata TopoDroid ZIP uzorka.
8. Track → provjeri GPS i DEM profil te spremanje nakon restarta aplikacije.
9. Provjeri migraciju SQLite tracking tablice i slanje `altitude_dem_m`.

## Poznati rizici / otvoreno

- Zadnje source izmjene nakon 11:07 nisu još potvrđene kompilacijom.
- TDR format ima više verzija; oba korisnikova ZIP uzorka obavezno testirati.
- Bundled nacrti čine oko 247 MiB sourcea i APK će ostati oko 270–280 MiB.
- Kod trenutačno može kopirati bundled nacrt u `filesDir`; kasnije optimizirati direktno čitanje WebP asseta i privremeni PDF cache.
- Postoje kandidati za identične duplikate među nacrtima; ne brisati bez SHA-256 deduplikacijskog prolaza i korekcije indexa.
- Matcher još ima 41 grupu za ručni pregled i 63 unmatched.
- Supabase staging/promotion za novu SOV bazu nije označen kao izvršen.

## Datoteke za nastavak

- `AI_HANDOVER_1.4.55a_CURRENT.md` — ovaj dokument, aktualan.
- `AI_HANDOVER_1.4.55a_OFFLINE_NACRTI_BAZA_UPDATE.md` — Claudeov prethodni kontekst.
- `MATCHING_REPORT_v3.md`
- `match_nacrti_v3.py`
- `match-overrides.json`
- `convert_nacrti.py`
- `UBACI_NACRTE.bat`
- `update_sov_baza.py`
