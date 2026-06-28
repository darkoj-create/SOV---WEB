# SOV Web v6.1.10 — Documents Full Archive Ready

Build: `sov-web-build-v6.1.10-documents-full-archive-ready`

## What changed

- Added `zapisnici-cijela-arhiva.html` for the complete meeting-minutes archive target `1960–2026`.
- Added `zapisnici-import.html` protected import page for Arhivar/Admin/Webmaster.
- Added `SUPABASE_SOV_DOCUMENTS_ARCHIVE_v6_1_10.sql` for SQL metadata + private Supabase Storage bucket.
- Added `data/zapisnici-full-archive-index.json` to document the hybrid migration state.
- Updated `dokumenti.html` with a new premium card: `Cijela arhiva zapisnika (1960–2026)`.
- Added static fallback: until SQL is populated, full archive shows existing static packages `2017–2022` and `2026`.

## Intended architecture

```text
SQL table public.sov_document_archive = metadata / filters / search
Supabase Storage bucket sov-documents = actual PDF/DOCX/JPG/PNG files
Web ZIP = UI only, not the complete archive files
```

## Why

The already integrated static archive is ~62 MB for 2017–2022 + 2026. A complete archive 1960–2026 would quickly become too large for clean deploys if packed into the web ZIP. This build prepares the UI and database/storage layer so old files can be uploaded year-by-year without inflating every deploy.

## Recommended upload layout

```text
sov-documents/
  zapisnici/
    1960/
    1961/
    ...
    2026/
```

## SQL

Run `SUPABASE_SOV_DOCUMENTS_ARCHIVE_v6_1_10.sql` once before using the import page.

