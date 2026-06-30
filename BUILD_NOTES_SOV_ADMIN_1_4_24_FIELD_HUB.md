# SOV Admin APK v1.4.24 — Field laptop hub

Baseline: v1.4.23-sync-contracts

## Cilj

APK je prilagođen lokalnom laptop hubu za teren bez signala.
Postojeći SOV Cloud tok ostaje isti; laptop hub je dodatni lokalni kanal.

## Dodano

- Novi `SovFieldHubClient.kt`
  - sprema adresu laptop huba i PIN
  - `GET /ping` za test veze
  - `GET /roster` za preuzimanje izleta/ekipa s laptopa
  - `POST /upload` za slanje `.sovpkg` paketa s mobitela na laptop
  - koristi `X-SOV-PIN`

- `Izleti` ekran
  - nova kartica `Laptop hub`
  - polja: adresa laptop huba i PIN
  - gumbi: Spremi, Test, Povuci ekipe s laptopa
  - prikaz roster cachea

- Lokalni izleti
  - nova akcija `Na laptop`
  - eksportira postojeći `.sovpkg` i šalje ga na laptop hub

- Cloud izleti
  - nova kartica `Laptop hub sync`
  - može povući ekipe s laptopa
  - prikazuje lokalno povučene ekipe za taj izlet
  - može poslati pripadajući `.sovpkg` na laptop hub

## Nije dirano

- Inventura i oružarstvo RPC logika iz 1.4.23
- Arhivar `_v2` detalj objekta
- Posudbe read-RPC i fallback
- Supabase schema/RLS
- Import layer persistence iz 1.4.21b
- Existing `.sovpkg` import/share tok

## SQL

Nema novog SQL-a za field hub.
Ako 1.4.23 SQL `SUPABASE_SOV_ARMORY_ACTIVE_REQUESTS_RPC_v1_4_23.sql`
već postoji u bazi, ne treba ponovno pokretati ništa za ovaj build.

## Validacija u ovom okruženju

- Ručna statička provjera zagrada/parenteza prolazi za promijenjene Kotlin fileove.
- `./gradlew assembleDebug` nije mogao krenuti jer Gradle wrapper download
  `https://services.gradle.org/distributions/gradle-8.7-bin.zip` vraća HTTP 403
  u ovom okruženju. To je infrastrukturni download problem prije same kompilacije.
