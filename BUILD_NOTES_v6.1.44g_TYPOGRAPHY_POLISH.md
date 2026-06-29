# SOV Web v6.1.44g — Typography polish

Baseline: `sov-web-build-v6.1.44f-documents-cleanup.zip`

## Changed

- Added `assets/sov-typography-v6144g.css`.
- Injected it into active public/admin/news/minutes HTML pages.
- Standardized the UI typography to a conservative system stack:
  - Aptos / Segoe UI Variable / Segoe UI / Roboto / Helvetica Neue / Arial / system-ui.
- Removed the visual clash caused by mixed decorative/serif page text in active SOV pages.
- Improved heading spacing, line-height and long-form readability.
- No remote Google Fonts.
- No font files bundled in the repo.

## Not changed

- Supabase / SQL.
- Gmail sync.
- Zapisnici import logic.
- Izleti.
- Oružarstvo.
- Arhivar.
- Karta.
- APK.
- WordPress content/media.

## Reason

Feedback was that the site font looked a bit rough. This build uses a calmer, boring-in-a-good-way font stack that should not annoy design perfectionists while remaining stable and dependency-free.
