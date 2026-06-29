# SOV Web v6.1.9 — Documents archive 2017–2022

## Added
- Added `zapisnici-arhiva-2017-2022.html` as a premium archive page under Documents → Zapisnici sastanaka.
- Integrated uploaded `2017.zip` package, which actually contains years 2017, 2018, 2019, 2020, 2021 and 2022.
- Added 271 archived documents to `assets/documents/zapisnici/archive-2017-2022/`.
- Added manifest `data/zapisnici-2017-2022.json` for future SQL/storage migration.
- Added Documents dashboard card: “Arhiva zapisnika (2017–2022)”.

## Upload package audit
- Total documents: 271
- Total unpacked size: 49.2 MB
- Formats: DOCX 241, DOC 12, ODT 12, XLSX 3, PDF 2, JPEG 1
- Regular meeting minutes: 262
- 2017: 46 docs, ~1.0 MB
- 2018: 44 docs, 13.3 MB
- 2019: 48 docs, 19.5 MB
- 2020: 41 docs, 639 KB
- 2021: 47 docs, 751 KB
- 2022: 45 docs, 14.0 MB

## UX
- Search across title, excerpt, year, format and type.
- Filter by year and document type.
- Grouped by year and month.
- Each document has Open and Download actions.
- Large files are marked in metadata chips.

## Database recommendation
No SQL is required for this build. Static integration is acceptable for this 2017–2022 batch, but the full 1960–2026 archive should move to:

- Supabase Storage or other object storage for actual files.
- SQL table for metadata, tags, file paths, sizes, dates and OCR/full-text search.
- Web should load metadata only and download files on click.

