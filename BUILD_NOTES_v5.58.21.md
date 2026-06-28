# SOV web v5.58.21 — Arhivar full object edit + Karta clean detail

## Arhivar
- `arhivar.html` → Edit objekt sada nije samo naziv/pločica/koordinate.
- Dodan puni edit za:
  - naziv, pločica, tip, mjesto, županija, općina, lat/lon
  - opis objekta / tehnički opis
  - pristup
  - istraživanje / povijest
  - autori / ekipa
  - hidrologija
  - geologija / morfologija
  - opasnosti / zaštita
  - javna napomena
  - katastarski status, status zapisa, zadaci/falinke, workflow
  - digitalni nacrt, bibliografija/zapisnik, GPS tracklog, georef zapis
- Spremanje ide kroz novi RPC `sov_arhivar_update_object_full(...)`.
- Detail dohvat prvo koristi `sov_arhivar_get_object_detail_v2(...)`, uz fallback na stari RPC.

## Karta
- `karta.html` više ne prikazuje dev tekst `Izvor: ...`.
- Detail objekta na karti koristi isti v2 full detail RPC kao Arhivar, pa nakon klika prikazuje puni normalni info objekta.

## SQL
- Novi SQL: `SUPABASE_SOV_ARHIVAR_FULL_OBJECT_EDIT_v5_58_21.sql`.
- Ne briše raw import; koristi `speleo_object_overrides` kao siguran arhivarski override sloj.
- Audit promjena ide u `speleo_object_edits`.

## Sync status
- `sync-status.html` usklađen na v5.58.21.
