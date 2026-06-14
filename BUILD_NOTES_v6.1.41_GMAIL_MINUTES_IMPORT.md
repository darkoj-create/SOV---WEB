# SOV Web v6.1.41 — Gmail zapisnici auto-import

Bazirano na v6.1.40b.

## Dodano

- Gmail/Apps Script auto-import sloj za zapisnike sastanaka.
- SQL backend:
  - `sov_gmail_ingest_keys`
  - `sov_gmail_minutes_import_log`
  - `sov_ingest_meeting_minutes_from_gmail(...)`
  - helper funkcije za datum i sekciju NAJAVE
- Apps Script:
  - `SOV_GMAIL_ZAPISNICI_APPS_SCRIPT_v6_1_41.gs`
  - čita Gmail labelu `SOV/Zapisnici za obradu`
  - sprema DOCX u Google Drive folder
  - šalje plain text i metadata u Supabase
  - prebacuje mail u `SOV/Zapisnici obrađeno` ili `SOV/Zapisnici greška`
- Web:
  - `zapisnici-najave.html` prikazuje Gmail ingest log
  - cache-bust dignut na `6.1.41-gmail-minutes`

## Nije dirano

- Oružarstvo
- XLS export
- Auth/password reset
- User posudbe/paketi
- Postojeći izleti i staging dedupe logika

## Napomena

Original DOCX iz Gmaila se sprema u Google Drive folder preko Apps Scripta. U SOV bazu ide dokumentni metadata + plain text + staging najave.
