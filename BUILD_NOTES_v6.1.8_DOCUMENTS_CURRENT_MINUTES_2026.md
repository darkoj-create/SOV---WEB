# SOV Web v6.1.8 — Documents / Aktualni zapisnici 2026

Base: `sov-web-build-v6.1.7-cloud-documents-hub.zip`

## Added

- New page: `zapisnici-aktualni-2026.html`
- New folder/card inside `dokumenti.html` → **Zapisnici sastanaka**:
  - **Aktualni zapisnici (2026)**
- Integrated uploaded `Zapisnici 2026.zip` as individual DOCX files under:
  - `assets/documents/zapisnici/2026/`
- Added metadata manifest:
  - `data/zapisnici-2026.json`

## Imported documents

- Count: 17 DOCX files
- Total size: 5.8 MB
- Date range: 2026-01-07 → 2026-05-06

## SQL

No SQL required for this build. This is currently static document delivery.

Recommended future architecture for 1960–2026 archive:

- Store binary files in object storage/Supabase Storage/Drive, not directly inside SQL rows.
- Store metadata in SQL: year, meeting date, category, title, storage path, file type, file size, OCR/full text.
- Use SQL for search/filtering and Storage for downloading/opening files.
