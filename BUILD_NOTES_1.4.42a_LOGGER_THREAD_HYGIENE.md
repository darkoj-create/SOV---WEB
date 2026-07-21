# SOV Admin v1.4.42a — SovClientLogger thread hygiene

Bazirano na: `v1.4.41b-manifest-intent-filter-cleanup`

## Opseg

Plan korak 2.3: `SovClientLogger` više ne pokreće novi `Thread {}` za svaki log.

## Promjene

- `util/SovClientLogger.kt`:
  - dodan jedan `Executors.newSingleThreadExecutor()`
  - executor koristi daemon thread `SOV-ClientLogger`
  - `logHandledError()` sada radi fire-and-forget preko tog executora
  - zadržana je postojeća deduplikacija od 15 sekundi
  - zadržano je postojeće `isSending` ponašanje da se ne rade paralelni remote logovi
  - nije mijenjan payload, redaction, Crashlytics ni Supabase RPC endpoint

- `app/build.gradle.kts`:
  - `versionCode = 900140`
  - `versionName = "1.4.42a-logger-thread-hygiene"`

## Namjerno nije dirano

- self-update sustav
- Supabase RPC / RLS
- UI / UX
- Apps Script auth
- network security guard

## Test

Lokalno pokrenuti:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

Ručni smoke test:

1. login/logout
2. izazvati jednu handled grešku ili mrežni fail
3. provjeriti da app ne crasha
4. provjeriti da se log i dalje šalje u Supabase/Crashlytics kada postoji session
5. brzo ponoviti istu grešku i provjeriti da deduplikacija ostaje aktivna
