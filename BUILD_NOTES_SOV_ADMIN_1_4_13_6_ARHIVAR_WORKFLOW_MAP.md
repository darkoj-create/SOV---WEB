# SOV Admin v1.4.13.6 — Arhivar workflow checkovi + TK25 otvaranje

Ovaj build usklađuje Android admin APK s web buildom v5.57.8.

## Promjene

- Arhivar detail ostaje normalan, ljudski prikaz objekta bez SQL/raw dumpa.
- Checklist sada podržava iste workflow faktore kao web:
  - Pločica
  - Koordinate
  - Nacrt
  - Zapisnik
  - Fotka
  - Nacrt ne treba ponoviti / ponoviti nacrt
- Dodan unos za broj pločice / katastarski broj.
- Spremanje statusa koristi novi RPC:
  - `sov_archive_update_object_status_v2`
- Ako objekt ima koordinate, u detailu se prikazuje gumb:
  - `Otvori na TK25 karti`
- Otvaranje na karti pali TK25/WMS način rada, centrira se na objekt i koristi privremeni arhivarski marker.

## SQL preduvjet

Potrebno je da je u Supabaseu već pokrenut web SQL v5.57.8:

`SUPABASE_SOV_ARHIVAR_WORKFLOW_v5_57_8_WORKFLOW_CHECKS_TK25.sql`

## Verzija

- `versionName = 1.4.13.6-arhivar-workflow-map`
- `versionCode = 900052`
