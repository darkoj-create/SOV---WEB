# UI / Navigation / Drawing Pen simplification plan

This is a product note for the next UX cleanup after SOV 1.1 Public Release.

## Current UX issue

The map can feel busy because tracking, ruler, drawing pen, overlay controls, GPS/follow actions and imported layers compete for attention. Drawing pen is useful, but it should feel like a temporary mode, not like the app has changed permanently.

## Recommended direction

### 1. One clear entry point

Keep drawing pen inside Map tools only:

- `Map tools -> Drawing pen`
- Do not expose multiple pen toggles in separate places.
- When pen starts, show a small temporary floating toolbar.

### 2. Active-pen toolbar

When drawing is active, show only:

- `Undo`
- `Save`
- `Cancel`

No brush thickness, no advanced options in the first public UX.

### 3. Strong mode indication

When drawing is active:

- Dim or hide non-essential map controls.
- Show a short label: `Drawing mode` / `Način crtanja`.
- Prevent map panning from fighting with finger drawing.

### 4. Safe exit behavior

- `Save` saves and turns drawing mode off.
- `Cancel` discards and turns drawing mode off.
- Android Back exits drawing mode first, not the whole map.

### 5. Cleaner navigation

- Keep left toolbar slightly higher and collapsible.
- Keep Field Control actions grouped: Import, Folder, Hide all.
- Move rarely used technical actions into Map tools, not on the main map canvas.

## Why this matters

For field use, the app should never make the user wonder which mode is active. Drawing must feel temporary, obvious and reversible.
