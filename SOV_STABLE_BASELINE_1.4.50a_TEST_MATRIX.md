# SOV v1.4.50a — Minimal Test Matrix

## Build

- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:compileDebugKotlin`
- [ ] `./gradlew :app:assembleDebug`

## Login/session

- [ ] cold start bez logina ne crasha
- [ ] login radi
- [ ] logout radi
- [ ] restart appa zadrži session
- [ ] corrupt/expired session vraća na login bez crasha

## Core app

- [ ] Home se otvara
- [ ] Settings se otvara
- [ ] tema Sustav/Svijetla/Tamna radi
- [ ] bottom navigation radi
- [ ] ikone ne mijenjaju layout

## Moja baza/import

- [ ] GPX/KML iz appa
- [ ] GPX/KML iz file managera
- [ ] KMZ/SOVPKG iz WhatsApp/Drive ako postoji test file
- [ ] MBTiles/GPKG/TIFF ako postoji test file
- [ ] random TXT/JSON se ne nudi ili se ignorira bez crasha

## Karta/Baza

- [ ] bez logina traži login ili faila uredno
- [ ] s loginom učita bazu
- [ ] WMS preko HTTPS radi
- [ ] lokalni WMS / Field Hub HTTP preko 192.168.x.x radi
- [ ] vanjski HTTP WMS se blokira/odbije uredno

## Izleti

- [ ] feed nakon logina radi
- [ ] detalj izleta radi
- [ ] datoteke izleta rade
- [ ] tracking ekran se otvara ako postoji aktivni izlet

## Oružarstvo

- [ ] katalog radi
- [ ] search radi
- [ ] zahtjev člana prikazuje requester oružaru
- [ ] inventura pojedinačno spremanje radi
- [ ] inventura bulk confirm traži potvrdu
- [ ] undo bulk confirma radi

## Web/public Supabase

- [ ] public news list/detail radi bez logina
- [ ] Runner leaderboard radi bez logina
- [ ] Runner score submit radi bez logina ako ga i dalje želimo javno
- [ ] karta/izleti bez logina ne dobivaju privatne podatke

## Terenski status

- [ ] GPS status ekran
- [ ] Kompas ekran
- [ ] Signal/pokrivenost ekran

## Final

- [ ] nema crasha u prvih 5 minuta normalnog korištenja
- [ ] nema vidljivih tokena/emailova u logovima
- [ ] nema regresije u update sustavu
