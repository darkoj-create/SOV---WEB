# SOV v6.1.41 — Gmail auto-import zapisnika

## Što radi

Apps Script čita Gmail labelu `SOV/Zapisnici za obradu`, nalazi DOCX attachment, izvuče tekst iz DOCX-a, spremi original u Google Drive folder i pošalje tekst/metapodatke u Supabase RPC:

`public.sov_ingest_meeting_minutes_from_gmail(...)`

Nakon uspjeha mail prebacuje u labelu `SOV/Zapisnici obrađeno`. Ako padne, stavlja `SOV/Zapisnici greška`.

## Setup

1. Otvori Google Apps Script.
2. Napravi novi projekt, npr. `SOV Gmail zapisnici`.
3. Zalijepi sadržaj filea `SOV_GMAIL_ZAPISNICI_APPS_SCRIPT_v6_1_41.gs`.
4. Run `installSovGmailZapisniciTrigger()`.
5. Dopusti Gmail, Drive i UrlFetch permissions.
6. U Gmailu stavi labelu `SOV/Zapisnici za obradu` na mail sa zapisnikom.
7. Run `processSovGmailZapisnici()` za prvi test.

## Sigurnost

Default ingest key je:

`SOV_GMAIL_ZAPISNICI_2026_CHANGE_ME`

To je namjerno za prvi setup. Kasnije ga treba promijeniti:

- u tablici `sov_gmail_ingest_keys`
- u Apps Script configu

## Web

U `zapisnici-najave.html` dodan je Gmail status/log box. Izvučene najave i dalje idu kroz isti staging/dedupe/approve flow.
