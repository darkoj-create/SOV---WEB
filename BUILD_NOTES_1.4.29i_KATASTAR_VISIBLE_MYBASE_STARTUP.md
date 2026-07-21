# SOV Admin 1.4.29i — Katastar vidljiv registriranima + startup fix

## Fix 1 — Katastar nije bio vidljiv
Katastar dataset je bio u assetima, ali gate je tražio poseban `canViewKatastar` flag.
Po zahtjevu, Katastar sada mogu vidjeti svi prijavljeni i odobreni korisnici.

Promjena:
- `SovPermissionsStore.canUseKatastarNow()` sada vraća `true` za `session.isLoggedIn && permissions.isApproved`.
- Ne traži više poseban `canViewKatastar` flag.

Katastar asset:
- `app/src/main/assets/katastar_crospeleo_2026_android_v1.json.gz`
- 6317 objekata iz `CroSpeleo - objekti.xlsx`

## Fix 2 — freeze na 49% kod velike Moje baze
Problem: app je u `onCreate()` i startup loadu pokušavao odmah skenirati/parsati velike GPX/KML/CSV/MBTiles foldere.

Promjena:
- maknut sinkroni `SovNativeOfflineFolders.scanAndRestore()` iz `MainActivity.onCreate()`
- `Moja baza` se više ne parsira u glavnom startup loadu
- prvo se prikažu SOV baza + Katastar
- Moja baza se dodaje u pozadini
- restore iz native foldera više ne parsira velike KML/CSV datoteke odmah, nego ih samo vraća u radni folder

## Nije dirano
- poslovna logika pretrage
- SOV baza
- Oružarstvo
- Runner SQL
- Cloud login gate
- Supabase schema osim ranije napravljene katastar tablice/gatea

## Napomena
APK nije buildan u sandboxu jer nema interneta za Gradle wrapper.
