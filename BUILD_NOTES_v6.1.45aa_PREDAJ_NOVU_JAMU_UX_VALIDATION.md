# SOV web v6.1.45aa — Predaj novu jamu UX/validation hardening

## Scope
Patch only `predaj-novu-jamu.html` and new form UX assets. Submit happy-path / Supabase insert/upload flow is preserved.

## Changed
- Added `assets/sov-submission-page-v6145aa.js` replacing the old form runtime for this page.
- Added `assets/sov-submission-ux-v6145aa.css`.
- `predaj-novu-jamu.html` now uses `novalidate` and custom visible validation.

## Fixes
- Success message is shown directly in the submit section next to `Predaj Arhivaru`, with submission ID and inbox explanation.
- Removed practical cause of invisible native required validation by replacing it with custom validation.
- Missing required fields scroll to first invalid field and highlight it.
- Consent failure shows clear reason: `Nedostaje: potvrda prije predaje`.
- WGS84 decimal comma is accepted; Croatia-range warnings added without blocking submit.
- File inputs render selected file lists with size and per-file remove buttons.
- File inputs validate category extensions and warn about large files.
- Sidebar section links are all real anchors and get active scroll-spy highlighting.
- Draft buttons now use consistent `skica` naming and show saved time.
- `Obriši skicu` asks for confirmation before local draft deletion.

## SQL
No SQL changes.
