# SOV Admin APK v1.4.21 — Inventura search

Baseline: v1.4.20-role-view-check-fix

## Changed

### Oružarstvo / Inventura
- Dodana pretraga opreme direktno u APK inventuri.
- Pretraga radi preko cijelog raw inventara, ne samo trenutno odabrane kategorije/lokacije.
- Pretraga koristi postojeću SOV tolerantnu logiku za opremu:
  - bez kvačica
  - aliasi za česte pojmove: croll/krol, karabiner/karbiner, uže/uzad, bušilica/baterija itd.
  - po nazivu, šifri, kategoriji, podkategoriji, lokaciji, napomeni i searchText polju
- Dodan dodatni fuzzy token match za tipfelere.
- Kad je pretraga aktivna, filteri kategorija/lokacija se sakriju da oružar odmah vidi rezultate.
- Dodan gumb “Očisti”.
- Rezultati su ograničeni na prvih 120 radi stabilnog UI-a.
- “Potvrdi sve u kategoriji” mijenja copy u “Potvrdi sve prikazane rezultate” kad je pretraga aktivna.

## Not changed
- Nema SQL promjena.
- Nema web promjena.
- Nema promjena u Supabase repositoryjima.
- Nema promjena u save/delete/export logici.
- Nema promjena u trips/tracking/broadcast logici.
- Firebase/Crashlytics setup iz prethodne verzije ostaje.

## Validation
- id i data tokovi inventure su netaknuti.
- EquipmentSupabaseRepository nije mijenjan.
- Zagrade/parenteze u HomeAndToolsScreens.kt su balansirane.
- ZIP integrity test prolazi.
