# SOV Admin v1.4.14.2 — Field Tracking team map viewer

Baseline: `sov-admin-v1.4.14.1-field-tracking-terrain-modes-source.zip`

## Cilj
Dodati smislen APK viewer za Field Tracking na samoj karti:

- korisnik na karti klikne **Team**
- odabere teren/team
- app povuče zadnje poznate pozicije članova i trailove
- karta prikaže markere ljudi i linije traga

## Promjene

### MapFeature.kt
- dodan mini toolbar gumb **Team** na desnom map toolbaru
- dodan bottom sheet **Članovi teama na karti**
- sheet dohvaća dostupne terene preko `sov_tracking_get_my_field_events`
- odabrani teren dohvaća:
  - latest pozicije: `sov_tracking_get_latest_positions`
  - trail: `sov_tracking_get_trip_points_v2` fallback `sov_tracking_get_trip_points`
- na karti se crtaju:
  - marker za svaku osobu
  - status boja: online / zadnja poznata / offline / SOS
  - trail linija po članu za zadnjih 6 sati
- refresh ide ručno i automatski svakih 30 s dok je team odabran

### FieldTrackingLiteStore.kt
- dodani DTO modeli:
  - `FieldTrackingFieldEvent`
  - `FieldTrackingLatestPosition`
  - `FieldTrackingRemotePoint`
- dodani API helperi:
  - `getMyFieldEvents`
  - `getLatestPositions`
  - `getTripTrackPoints`

## Verzija
- `versionCode = 900059`
- `versionName = 1.4.14.2-field-tracking-team-map-viewer`
- expected APK: `SOV-ADMIN-1.4.14.2.apk`

## SQL
Nema novog SQL-a. Koristi postojeći Field Tracking SQL v5.59.2.

## Napomena
Gradle build nije mogao biti pokrenut u sandboxu jer wrapper pokušava skinuti Gradle 9.0.0 s `services.gradle.org`, a sandbox nema internet.
