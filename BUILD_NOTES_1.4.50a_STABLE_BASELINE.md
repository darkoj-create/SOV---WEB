# SOV Admin v1.4.50a — STABLE BASELINE

## Status

Ovo je stabilizacijski baseline nakon serije malih sigurnosnih, UX i stabilizacijskih koraka.

## Baza

- Prethodni APK source: `v1.4.49a-final-checklist`
- Nova verzija: `v1.4.50a-stable-baseline`
- `versionCode`: `900148`
- `versionName`: `1.4.50a-stable-baseline`

## Opseg

Nema novih featurea i nema novih produkcijskih promjena izvan označavanja stabilne verzije.

Promijenjeno je samo:

- `app/build.gradle.kts` — bump verzije
- dodani finalni baseline dokumenti

## Namjerno NIJE dirano

- Supabase / RLS / RPC
- self-update sustav
- Apps Script endpoint logika
- network guard
- manifest intent-filteri
- UI layout
- business logika
- Field Hub / Laptop Hub komunikacija
- Oružarstvo / Arhivar / Izleti / Karta logika

## Lokalni build

Pokrenuti:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

## Release pravilo

Ako ovaj build prođe i regression checklist je zelen, ovo je nova sigurna baza za iduće funkcionalne promjene.
