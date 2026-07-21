# SOV Admin v1.4.54a — DEM Track Profile

## Sažetak
Korak 3 DEM nadogradnje: visinski profil tracka i DEM provjera visine ulaza objekta.

## Verzija
- versionCode: 900153
- versionName: 1.4.54a-dem-track-profile

## Promijenjeno
- Dodan `TrackElevationProfileFeature.kt`.
- U Offline tabu dodana akcija `Profil` za spremljene trackove.
- U Import KML/GPX karticama dodana akcija `Visinski profil` kada import sadrži track.
- Profil tracka uzorkuje najviše 200 točaka.
- Ako track ima GPS `<ele>`/altitude vrijednosti, korisnik može birati `GPS visine` ili `DEM visine`.
- DEM profil koristi postojeći `ElevationRepository`, uključujući offline DEM cache iz v1.4.53a.
- Graf je nacrtan čistim Compose Canvasom, bez vanjskih chart biblioteka.
- U detalju objekta dodana usporedba `Visina (baza)` i `Visina (DEM)`.
- Ako se baza i DEM razlikuju više od 30 m, prikazuje se diskretno žuto upozorenje.

## Namjerno nije mijenjano
- Nema SQL migracije.
- Nema promjene `SovHttpClient`, `SovPermissionsStore`, `SovNetworkSecurity`.
- Nema promjene WMS/hillshade/offline slojeva i njihovih postavki.
- DEM se ne crta kao vizualni sloj na karti.
- Baza se ne mijenja automatski; DEM usporedba je samo vizualna provjera.

## Test na uređaju
1. Otvori Offline > Tracks.
2. Na spremljenom tracku tapni `Profil`.
3. Ako track ima GPS visine, prebaci `GPS visine / DEM visine`.
4. Skini offline DEM paket, uključi airplane mode i ponovi DEM profil.
5. Otvori detalj objekta s koordinatama i provjeri red `Visina (baza) · Visina (DEM)`.
