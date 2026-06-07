# SOV 1.1 PUBLIC RELEASE

Release date: 19 May 2026  
Version: 1.1  
VersionCode: 464

## What this release is

SOV 1.1 is the first public-release baseline for the Android speleo field app. It is meant to be a stable field build rather than another experimental patch build.

## Main capabilities

- Search SOV cave/speleological object data.
- Open object details with coordinates, status, descriptions and field tasks.
- Use online WMS base maps, hillshade and Geological Units overlay.
- Work with offline maps, MBTiles, GPX/KML imports and saved field data.
- Record tracks with GPS spike filtering.
- Create and manage trips / field packages.
- Import a personal database through My Base using KML or CSV.
- Export My Base as CSV or KML.
- Use Drive drawings index with a built-in URL and a simple refresh action.
- Use Croatian by default, with saved English language preference when changed.

## Important technical notes

- Build metadata is updated in `app/build.gradle.kts`, `build.json` and `update.json`.
- Expected release APK filename after local build/signing: `SOV1.1.1.apk`.
- The included source ZIP may still contain older APK artifacts if not rebuilt locally; always rebuild/sign before publishing.
- `versionCode` is now 464.

## Suggested next UX simplification

The next cleanup target should be the drawing pen and map navigation flow: one obvious pen entry point, a small floating active-pen toolbar, auto-disable on Save/Cancel, and fewer always-visible controls on the map. See `docs/UI_NAVIGATION_PEN_SIMPLIFICATION.md`.
