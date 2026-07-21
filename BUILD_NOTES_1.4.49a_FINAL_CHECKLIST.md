# SOV Admin v1.4.49a — Final checklist / release-candidate notes

## Tip promjene

Dokumentacijski i release-candidate korak. Nema nove aplikacijske logike, nema novih Supabase migracija i nema UX promjena.

## Verzija

- `versionCode = 900147`
- `versionName = "1.4.49a-final-checklist"`

## Baza

Bazirano na `v1.4.48a-l10n-field-status`.

## Svrha

Ovaj build služi kao zadnja kontrolna točka prije `v1.4.50a STABLE BASELINE`.
Dodani su checklist i release-candidate notes kako se ne bi u finalni baseline ušlo naslijepo.

## Dirano

- `app/build.gradle.kts` — samo version bump
- `BUILD_NOTES_1.4.49a_FINAL_CHECKLIST.md`
- `SOV_REGRESSION_CHECKLIST_1.4.49a.md`
- `SOV_RELEASE_CANDIDATE_NOTES_1.4.49a.md`

## Namjerno nije dirano

- Supabase / RLS / RPC
- self-update
- Apps Script endpointi
- networking
- Field Hub logika
- karta/WMS logika
- Oružarstvo logika
- Izleti logika
- UI layout

## Lokalni build

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

## Lokalni testovi

```bash
./gradlew :app:testDebugUnitTest
```

Ako build i smoke test prođu, sljedeći korak je `v1.4.50a-stable-baseline`.
