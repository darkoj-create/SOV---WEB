SOV Admin → Darko OS track export

Sto je dodano:
- u Offline > Trackovi svaki track ima novi button: Darko OS
- button salje sazetak tracka u Darko OS Google Sheet
- GPX se sprema u Google Drive folder: Darko OS - SOV GPX Track Exports
- u Sheet se upisuje link na GPX + statistika tracka

Setup:
1. Otvori Darko OS Google Sheet:
   https://docs.google.com/spreadsheets/d/1GLc4W-3DPEsX_XYLEHok9-Q83XT3Y8xIZ2Y85En3Dxo/edit
2. Extensions > Apps Script
3. Zalijepi sadrzaj filea DARKO_OS_TRACK_EXPORT_WEBAPP.gs
4. Save
5. Deploy > New deployment > Web app
   Execute as: Me
   Who has access: Anyone with the link / Anyone
6. Kopiraj /exec URL
7. U Android projektu otvori:
   app/src/main/java/com/darko/speleov1/DarkoOsTrackSyncClient.kt
8. Zamijeni:
   PASTE_DARKO_OS_TRACK_EXPORT_WEBAPP_EXEC_URL_HERE
   sa svojim /exec URL-om
9. Buildaj SOV Admin APK

Napomena:
Bez tog /exec URL-a app ce prikazati toast da Darko OS endpoint nije postavljen.


INTEGRATED BUILD admin.2:
Darko OS endpoint is already integrated:
https://script.google.com/macros/s/AKfycbxVrStMsScP6UWVESnC6hcQV7MM2XRpzEQYZYblOgjHt1gzIhiinUfpQewjAY3ZnR2w/exec
Version: 1.1.1-admin.2-darko-os / versionCode 900002
