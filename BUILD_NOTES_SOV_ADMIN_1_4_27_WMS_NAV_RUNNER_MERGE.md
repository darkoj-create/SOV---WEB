# SOV Admin 1.4.27 — WMS + Nav Runner Merge

**Baseline:** `sov-admin-v1_4_26-wms-fast-source.zip`  
**Integrated patch source:** `files.zip` / `BUILD_NOTES_SOV_ADMIN_1_4_25_NAV_RUNNER_FIX.md`

## Što je integrirano

1. `SpeleoAppRoot.kt`
   - kod navigacije do cilja sada gasi GPS autocentar,
   - postavlja `importedFocusPoint`,
   - postavlja razuman zoom `15.0`,
   - okida `importedFocusNonce++`,
   - zatim otvara MAP tab.

2. `SpeleoRunnerScreen.kt`
   - pre-allocation `Path` pool (`_pathF`–`_pathK`),
   - zamijenjeni višestruki `Path()` alokasi po frameu,
   - cilj: manje GC/jank trzanja u SpeleoRunneru.

## Što je namjerno sačuvano iz 1.4.26

- WMS fast/stale-connection fix u `SovTileHttp.kt` ostaje netaknut.
- `WmsTilesOverlay`, `WmsBaseTilesOverlay`, cache i ostali WMS slojevi nisu dirani.
- Field Hub, Izleti, Oružarstvo, Arhivar, role/login, Supabase repositoryji nisu dirani.
- Nema SQL promjena.

## Što NIJE preuzeto iz files.zip

- Nije preuzet patch `build.gradle.kts` jer bi vratio verziju na `1.4.25`.
- Nisu preuzeti patch `update.json` i `build.json` jer bi pregazili `1.4.26` WMS metadata.

## Nova verzija

- `versionCode`: `900115`
- `versionName`: `1.4.27-wms-nav-runner`
- očekivani APK: `SOV-ADMIN-1.4.27.apk`

## Build napomena

Ovaj paket je source build za Android Studio. U sandboxu nije dostupan Android SDK pa ovdje nije moguće lokalno izgraditi potpisani APK. U Android Studio: Gradle Sync → Build APK.
