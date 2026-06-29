# SOV web v6.1.45ad — Arhivar simple workbench

## Scope
Frontend-only UX simplification for Arhivar screens. No SQL changes.

## Changes
- `arhivar-dashboard.html` rebuilt as a single workbench with 3 tabs: Predaje, Arhiva, Izvoz.
- Existing functional pages are loaded as embedded modules so current Supabase logic stays intact.
- Consistent Arhivar header/nav injected on Arhivar pages.
- Decorative emoji/icon noise hidden.
- Arhivar archive stats collapsed to key metrics first, with `Prikaži više` toggle.
- Status pipeline chips hidden in inbox; dropdown remains source of truth.
- Export moved visually to one central module with ŠTO + FORMAT selectors.
- Archive page duplicate export box replaced with pointer to central export.
- Detail panels made sticky with clear scrollbar on desktop and stacked on mobile.
- Filter/detail sync guards added to archive and inbox lists.
- Confirm dialogs added/strengthened for approve/reject/mark-needs actions.
- Global links normalized to `karta.html` and label `Karta objekata`.
- Includes previous fixes: simple submission flow, karta redirect hard-fix, member auth restore.

## Notes
- `Zahvati` and `Nacrti` remain reachable as secondary tools, not primary tab flow.
- No DOC/DOCX files are included intentionally.
