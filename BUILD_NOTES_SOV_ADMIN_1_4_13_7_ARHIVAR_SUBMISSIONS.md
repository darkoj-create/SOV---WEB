# SOV Admin 1.4.13.7 — Arhivar predane jame

Usklađeno s web buildom v5.58.0.

## Dodano u APK source
- Novi Supabase repository `ArchiveSubmissionsRepository`.
- Novi ekran `ArchiveSubmissionsScreen`.
- Iz postojećeg Arhivar ekrana dodan ulaz `Predane jame`.
- Predane jame prikazuju:
  - osnovne podatke speleo zapisnika,
  - opis/pristup/istraživanje,
  - privitke kao metadata,
  - status: čeka review / fali nešto / odobreno / odbijeno.
- Arhivar može:
  - `Approve u bazu` preko `sov_approve_speleo_submission`,
  - označiti što fali preko `sov_mark_speleo_submission_needs_changes`,
  - odbiti predaju.

## SQL dependency
Pokrenuti web SQL:
`SUPABASE_SOV_ARHIVAR_SUBMISSIONS_v5_58_0.sql`

## Napomena
File upload novih jama je primarno web/Baza flow u v5.58.0. APK u ovoj iteraciji pokriva arhivarski review dio, da ne ostane web-only.
