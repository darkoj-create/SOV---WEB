# SOV APK v1.4.17 — Oružar i Arhivar UX fixes

Baseline: v1.4.16z-broadcast-location-kml

## Changed

### Arhivar (ArchiveSubmissionsFeature.kt)
- Detail kartica predaje premještena iz inline LazyColumn u full-screen Dialog koji se otvara tapom na predaju.
- Arhivar vidi cijeli detail bez scrollanja nazad na vrh liste.
- Dodan confirm dialog prije "Odobri".
- "Approve u bazu" promijenjeno u "Odobri".
- Icons.Default.Delete zamijenjen s Icons.Default.Close na gumbu "Odbij predaju".
- Opis ekrana više ne koristi "SQL bazu" developer termin.
- Nakon approve / needs changes / reject akcije detail dialog se zatvara i lista se osvježava.
- Učitavanje liste više ne otvara automatski prvi detail dialog.

### Oružarstvo (HomeAndToolsScreens.kt)
- Loading karta: uklonjen tekst o "XLS fallback" i "cache".
- Tab "Oružar red" promijenjen u "Zahtjevi članova".
- Section title unutar Oružar taba ažuriran.
- Inventura zapis: "Razlika APK" zamijenjeno s ljudski čitljivim zapisom: "Prebrojano: X · Evidentirano: Y".

## Not changed
- Nema SQL promjena.
- Nema web promjena.
- Repozitoriji netaknuti: ArchiveSubmissionsRepository, EquipmentSupabaseRepository.
- Navigacija netaknuta: SpeleoAppRoot.kt, MainActivity.kt.
- Trips, tracking i broadcast netaknuti.
- Supabase pozivi nisu mijenjani.

## Validation
- selected inline detail blok uklonjen iz LazyColumn.
- Dialog i DialogProperties importi dodani.
- Delete import uklonjen, Close import dodan.
- showApproveConfirm koristi confirm dialog prije approve poziva.
- ArchiveSubmissionsRepository.* pozivi ostali netaknuti.
- Kotlin zagrade i parenteze u promijenjenim fajlovima su balansirane.
- Gradle compile nije završen u sandboxu jer wrapper pokušava skinuti Gradle 8.7 sa services.gradle.org, a okruženje nema internet.
