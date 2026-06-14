# Zapisnici 1960–2026 — practical import plan

## Decision

Use SQL for metadata and search, not for file storage.

- SQL: title, date, year, type, tags, file size, OCR summary, storage path.
- Storage: the actual DOCX/PDF/JPG/PNG files.
- Web build: only UI and small manifests.

## How to avoid massive uploads

1. Do not put historical files inside the web ZIP.
2. Convert scans to OCR PDF before upload.
3. Use 150–200 dpi grayscale or black/white for text documents.
4. Avoid TIFF/RAW; keep originals offline if needed, but publish optimized PDF.
5. Upload year-by-year, not one 10 GB package.
6. Use folders: `zapisnici/1960/`, `zapisnici/1961/`, etc.
7. Keep a CSV/JSON manifest for every batch.
8. Add OCR/full-text later only as SQL text/index, not by re-uploading files.

## Import order

1. Run SQL patch.
2. Upload one small test year.
3. Check `zapisnici-cijela-arhiva.html`.
4. Continue with 5-year batches.
5. Only after all files are stable, add OCR text/full-text search.

