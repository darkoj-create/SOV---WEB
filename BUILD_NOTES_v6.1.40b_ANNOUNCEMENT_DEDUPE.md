# SOV web v6.1.40b — Zapisnici najave dedupe

Baseline: uploaded `sov-web-build-v6.1.39k-xls-per-snapshot.zip`, after v6.1.40 + v6.1.40a parser fix.

## Added

- Dedupe metadata on `trip_announcements_staging`:
  - `dedupe_key`
  - `normalized_title`
  - `matched_announcement_id`
  - `matched_trip_id`
  - `duplicate_score`
  - `duplicate_reason`
  - `canonical_announcement_id`
  - `dedupe_status`
- New table `trip_announcement_links` for linking multiple minutes announcements to one canonical announcement.
- RPC helpers:
  - `sov_recompute_trip_announcement_dedupe(uuid)`
  - `sov_mark_trip_announcement_duplicate(uuid, uuid, text)`
  - `sov_force_trip_announcement_new(uuid)`

## Web changes

- `zapisnici-najave.html` cache-bust bumped to `6.1.40b-announcement-dedupe`.
- After importing DOCX and inserting staging announcements, the web calls `sov_recompute_trip_announcement_dedupe` for each new announcement.
- Announcement cards show a warning panel when there is a possible duplicate.
- Added filters:
  - `Mogući duplikati`
  - `Duplikati`
- Added actions:
  - `Spoji / označi duplikat`
  - `Ipak novi izlet`

## Not changed

- Gmail automation not added.
- Existing trips are not modified automatically.
- Approved announcements are not deleted.
- Oružarstvo, auth and XLS export untouched.
