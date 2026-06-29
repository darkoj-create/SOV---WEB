# SOV Web v6.1.45q — Public front UX polish

Public-facing UX cleanup for header/navigation, logo contrast, about subnav, expedition banner, 404, thumbnails, mobile and accessibility.

## Done
- Added `assets/sov-public-final-v6145q.css`
- Added `assets/sov-public-header-v6145q.js`
- Injected shared public header into key public pages.
- Replaced inconsistent public headers at runtime with one canonical accessible navigation.
- Fixed active nav state and mobile hamburger.
- Fixed bad `/o-nama` route via `vercel.json` rewrite and link normalization.
- Added styled `404.html`.
- Styled `o-drustvu.html` subnav as pill subnav.
- Fixed expedition banner CTA overlap by moving CTA into its own banner row.
- Added branded thumbnail fallback for missing/bad news images.
- Added focus-visible states, alt fallback and card aria labels.

## SQL
No SQL changes.
