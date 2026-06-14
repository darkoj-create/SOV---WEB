# SOV Web v6.1.40 — Zapisnici archive + najave parser foundation

Baseline: `sov-web-build-v6.1.39k-xls-per-snapshot.zip`

## Scope

Adds the first safe foundation for importing SOV meeting minutes and extracting trip announcements.

## SQL

Migration file:

`SUPABASE_SOV_v6_1_40_MEETING_MINUTES_ANNOUNCEMENTS.sql`

Applied to Supabase project `SOV-web`.

Creates:

- `public.meeting_minutes`
  - stores one parsed meeting-minutes document
  - references existing `public.sov_document_archive`
  - stores meeting date, title, original filename, storage path, SHA-256 checksum, plain text, extracted NAJAVE text, meeting leader, minutes taker
  - has checksum deduplication

- `public.trip_announcements_staging`
  - stores extracted announcements from the NAJAVE section
  - status flow: `novo`, `treba_provjeru`, `odobreno`, `odbijeno`, `duplikat`
  - stores title, dates, leader, location, category, description, raw text, confidence

- `public.sov_approve_trip_announcement(p_announcement_id uuid)`
  - creates a real `public.sov_trips` row from a staged announcement
  - marks the announcement as approved
  - keeps source metadata in `sov_trips.meta`

RLS:

- uses existing `public.sov_documents_is_staff()` for meeting minutes and staging management
- no public auto-publishing

## Web

New page:

- `zapisnici-najave.html`

New script:

- `assets/zapisnici-najave.js`

Updated:

- `dokumenti.html`
  - adds card: “Zapisnici → najave izleta”
- `assets/auth.js`
  - registers `zapisnici-najave.html`
  - protects it with archive role access

## User flow

1. Go to Dokumenti → Zapisnici → najave izleta.
2. Upload DOCX zapisnik.
3. Browser parses DOCX text using JSZip.
4. Meeting date is detected from title/filename.
5. Original DOCX is uploaded to `sov-documents` bucket.
6. Metadata/text is saved into `sov_document_archive` + `meeting_minutes`.
7. Section `NAJAVE` is extracted until `RAZNO`.
8. Each announcement is saved to `trip_announcements_staging`.
9. Staff can edit/review/reject/mark duplicate.
10. Staff can approve a row and create a real `sov_trips` item.

## Parser behavior

Supports the current SOV minutes pattern:

- `NAJAVE` section
- stops at `RAZNO`
- parses date patterns such as:
  - `31.05.`
  - `06.06. – 07.06.`
  - `4.-7.6.`
  - `15. 6. 2026.`
  - `20. – 21. 6. 2026.`
- categorizes roughly as:
  - `izlet`
  - `vježba`
  - `seminar`
  - `ekspedicija`
  - `predavanje`
  - `akcija`

## Not changed

- Oružarstvo
- user loan packages
- XLS export
- auth/password reset
- existing trips list/calendar logic
- existing document archive pages except one new link in `dokumenti.html`
- Gmail automation not implemented yet

## Next recommended build

v6.1.41:

- Gmail label watcher / manual Gmail attachment ingest
- duplicate detection by Gmail message id + checksum
- automatic import to `meeting_minutes`
- optional notification to Admin/Arhivar when new announcements are found
