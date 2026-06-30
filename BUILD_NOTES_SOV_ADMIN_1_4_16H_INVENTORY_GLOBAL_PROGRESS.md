# SOV Admin 1.4.16h — Inventory global progress

## Promijenjeno
- Inventura više ne resetira prebrojane stavke pri promjeni filtera.
- Globalni progress, manjak i višak računaju se kroz sve prebrojane stavke.
- Header prikazuje ukupan broj stavki iz cijelog inventara.
- Submit šalje samo sve prebrojane stavke, bez obzira na trenutačni filter.
- Zaključivanje je dostupno čim postoji barem jedna prebrojana stavka.
- Dodan gumb Potvrdi sve u kategoriji.
- Dodan gumb Resetiraj inventuru.

## Verzija
- versionCode: 900084
- versionName: 1.4.16h-inventory-global-progress

## Dodatni nužni popravci
- Filter kategorije `Sve` sada stvarno prikazuje sve stavke inventure.
- Globalni submit više ne označava payload imenom trenutačno odabranog filtera, nego kao sve prebrojane stavke.

## Provjera
- Pokušaj Gradle compilea pokrenut, ali nije dovršen jer okruženje nema pristup `services.gradle.org` za Gradle 8.7 download.
