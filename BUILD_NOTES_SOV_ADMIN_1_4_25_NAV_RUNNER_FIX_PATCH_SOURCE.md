# SOV Admin 1.4.25 — Nav + Runner Fix

**Datum:** 2026-06-26
**Baza:** 1.4.24-field-hub (900112)
**versionCode:** 900113

---

## Bug 1: Navigacija do importiranih jama / waypointa

### Simptom
"Go to" na importiranoj točki ili waypoint-u nije centrirao kameru na cilj.
Ako je bio aktivan live tracking, GPS autocentar je konstantno vukao kameru natrag na korisnikovu lokaciju.

### Uzrok
`setNavigationTarget()` u `SpeleoAppRoot.kt` nije:
- gasio `autoCenterOnUserEnabled`
- postavljao `importedFocusPoint` / `importedFocusNonce` za okidanje kamerine animacije

Nasuprot tome, `focusRecordOnMap(navigation=true)` ispravno radi sve četiri operacije.

### Fix — `SpeleoAppRoot.kt`
```kotlin
fun setNavigationTarget(target: NavigationTarget?) {
    navigationTarget = target
    if (target != null) {
        autoCenterOnUserEnabled = false   // GPS praćenje mora ustupiti navigaciji
        importedFocusPoint = target.point // osiguraj da kamera skoči na cilj
        importedFocusZoom = 15.0          // razuman zoom za navigaciju
        importedFocusNonce++              // okida animateTo u MapView
        navigateTo(AppTab.MAP)
    }
}
```

**Datoteke:** `SpeleoAppRoot.kt`
**Rizik:** nizak — kirurški, ne dira MapFeature ni kameralogiku

---

## Bug 2: SpeleoRunner jank na slabijim telefonima

### Simptom
Igra trzala na većini telefona, posebno na starijem hardveru.

### Uzrok
87 `Path()` instanciranja unutar draw funkcija pozivanih 60× u sekundi.
Najgori offender: `drawSierraShape` sam po sebi alocira 4 `Path()` po pozivu
(shadow offset, mid offset, highlight, detail). Svaki `drawInkPath` → `drawSierraShape`
→ 4 alloca. U jednom frameu: ~200+ Path alloca → GC pauze → jank.

### Fix — `SpeleoRunnerScreen.kt`

**1. Proširen pool pre-alociranih pathova:**
- `_pathF`, `_pathG`, `_pathH`, `_pathI` — za `drawSierraShape` internals
- `_pathJ`, `_pathK` — rezervni za kompleksne frame-ove

**2. `drawSierraShape` refactor (4 alloca → 0):**
Svi `Path().apply { addPath(path, offset) }` zamijenjeni s `_pathF.reset(); _pathF.addPath(path, offset)` itd.

**3. Hot-path draw funkcije — zamjena `val x = Path().apply {...}` s `_pathA.reset(); _pathA.moveTo(...)` itd.:**
- `drawStableVerticalBackground` → repeat(9) × 2 paths = 18 alloca eliminirana
- `drawStableHorizontalBackground` → repeat(6) + repeat(8) + floorPath + repeat(8) + biome 6/8/9 = 23+ eliminirana
- `drawBiomeAtmosphere` → biome 0 beam + biome 2 repeat(16) = 17 eliminirana
- `drawHorizontalCave` → ceilingMass + repeat(14) + floor + repeat(8) = 24+ eliminirana

**Ukupno eliminirano iz hot-patha:** ~200+ Path alloca/frame → ~5 (samo povremene, ne u per-frame loopovima)

**Datoteke:** `SpeleoRunnerScreen.kt`
**Rizik:** nizak — svi `_pathA`–`_pathK` globali su file-level private val, thread-safe (Compose Canvas = single UI thread), nema promjene vizualnog outputa

---

## Nije mijenjano
- Nema SQL izmjena
- Nema promjena u MapFeature.kt, OfflineFeature.kt, FieldPackageFeature.kt
- Nema promjena u tile/WMS loaderima
