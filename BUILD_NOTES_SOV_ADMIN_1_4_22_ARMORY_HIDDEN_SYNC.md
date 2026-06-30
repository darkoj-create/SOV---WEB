# SOV Admin APK v1.4.22 — Armory hidden sync

Baseline: v1.4.21b-imports-layer-persistence

## Razlog
Web v6.1.39c uveo je soft-hide za posudbe/zahtjeve opreme
(`armory_hidden` na `equipment_requests` / `equipment_loans`).
Oružar na webu radi "Makni iz viewa" → red se arhivira, ne briše.
APK je i dalje učitavao SVE zahtjeve, pa su se skriveni zahtjevi
i dalje pojavljivali u APK posudbama. Web i APK su se raziđli.

## Promjena (1 linija)
`EquipmentSupabaseRepository.fetchRequests()`:
- equipment_requests upit sada ima `&armory_hidden=not.is.true`.
- `not.is.true` = vrati redove gdje hidden NIJE true → uključuje
  `false` I `null`. Novi zahtjevi iz APK-a (bez flaga) ne nestaju,
  bez obzira na default kolone u bazi.

## Nije dirano
- Nema SQL promjena (kolone već postoje iz weba v6.1.39c).
- Nema promjena lokacija (APK ih dinamički čita iz artikala → već 2 kanonske).
- Nema snapshot podrške u APK (live inventura ostaje, namjerno).
- equipment_loans APK i dalje ne čita.
- Nema promjena u trips/tracking/arhivar/import logici.
- EquipmentSupabaseRepository write/inventura tokovi netaknuti.

## Validacija
- Jedina izmjena je URL filter u fetchRequests.
- Zagrade balansirane, ostatak repo-a bit-identičan baselineu.
