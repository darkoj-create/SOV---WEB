# SOV Admin 1.4.29 — Cloud Login Gate + Oružarstvo Sync

**versionCode:** 900117 · **versionName:** 1.4.29-cloud-login-armory-sync · **datum:** 2026-06-30

## Novo u ovom buildu

### 1. Cloud login gate (s povratkom)
- Ako **nisi prijavljen** i otvoriš **Cloud** (donja navigacija ili prečac), app te **prvo vodi na prijavu**.
- Nakon uspješne prijave **vraća te točno u Cloud** — ne na Home.
- Implementacija: `SpeleoAppRoot.kt`
  - `navigateTo(AppTab.CLOUD)` provjerava `SovPermissionsStore.loadSession(context).isLoggedIn`; ako nisi prijavljen, sprema `pendingPostLoginRoute = CLOUD` i ide na `LOGIN`.
  - `SovCloudLoginScreen.onLoggedIn` čita `pendingPostLoginRoute` i navigira natrag na spremljenu rutu (`popUpTo(HOME){saveState}` da login nestane iz back-stacka).
  - `pendingPostLoginRoute` je `rememberSaveable` (preživi rotaciju/proces).
  - **Svi Cloud moduli gated, ne samo Cloud tab:** Oružarstvo, Arhiva i Kalendar otvaraju se kroz `openCloudRoute(route)` koji traži prijavu i vraća te na taj isti modul nakon logina — bez obzira otvaraš li ih iz Cloud ekrana ili iz Tools prečaca. (Izleti/Trips su već zaštićeni provjerom role.)

### 2. Jasan login gumb na Home
- `HomeAndToolsScreens.kt` (HomeScreen): stari mali shield-ikona zamijenjen **pill gumbom s tekstom**:
  - odjavljen → amber **„Prijavi se"** (shield ikona),
  - prijavljen → zelena **„Prijavljen"** (check ikona).
- Klik → otvara `SovCloudLoginScreen` (ista ruta kao prije).

### 3. Oružarstvo — kategorije/podkategorije 1:1 s webom
(uključeno iz prethodnog zadatka, sad u istom buildu)
- `EquipmentSupabaseRepository.kt`: dodano polje `priority`, prikaz kategorije kroz `displayCatName` (isti remap kao web: „Osobni SRT komplet" → „Osobna oprema"), sortiranje po server `priority` (`effectiveCategoryPriority`/`categoryPriority`), kanonski nazivi u offline/legacy putevima. Uklonjen stari `normalizeCategoryForSort`.
- `assets/oruzarstvo-xls-canonical-v6.1.5.json`: regeneriran iz žive baze (546 stavki) → offline ima iste podkategorije.
- SQL `SUPABASE_ORUZARSTVO_CATEGORY_PRIORITY_ALIGN_v6_1_45ai.sql` (priložen) — **već primijenjen na produkciju**.

## SQL
Nema novih obaveznih migracija za ovaj APK build osim već primijenjene priority funkcije.

## Test checklist
- [ ] Odjavljen: tap Cloud → otvara se Login. Nakon prijave → odmah Cloud.
- [ ] Prijavljen: tap Cloud → direktno Cloud (bez login koraka).
- [ ] Home gumb prikazuje točan status (Prijavi se / Prijavljen).
- [ ] Oružarstvo: kategorije i redoslijed identični webu (oruzarstvo.html).
- [ ] Offline (airplane mode, fresh install): kategorije/podkategorije i dalje ispravne.

## Izmijenjene datoteke
- app/src/main/java/com/darko/speleov1/SpeleoAppRoot.kt
- app/src/main/java/com/darko/speleov1/HomeAndToolsScreens.kt
- app/src/main/java/com/darko/speleov1/util/EquipmentSupabaseRepository.kt
- app/src/main/assets/oruzarstvo-xls-canonical-v6.1.5.json
- app/build.gradle.kts (versionCode/Name)
- update.json
