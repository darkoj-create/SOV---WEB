## v1.1 manual/readme + teal shared waypoint polish
- Build package: `sov-v1.1-PUBLIC-RELEASE-teal-shared-readme-manual.zip`.
- Shared waypoints changed from dark/navy blue to teal marker color so they no longer conflict with My Base dark-blue points.
- Shared track overlay color aligned to teal.
- README.md replaced with a user-facing visual README/manual.
- Bundled visual manual assets under `docs/manual/images/` and DOCX manual under `docs/manual/`.

# v1.1 PUBLIC RELEASE

- versionName 1.1 / versionCode 464.
- Public-release baseline created from the v1.0.166 field patch line.
- Updated Gradle version metadata, `build.json`, `update.json`, About screen copy and documentation.
- Baseline includes WMS/Geo flicker fixes, hillshade redraw/cache optimization, My Base KML/CSV import-export, GPS spike filtering, simplified battery optimization prompt, English UI cleanup, Drive drawings URL cleanup and red-object field-task placement improvement.
- Added `docs/RELEASE_NOTES_1.1_PUBLIC.md` and `docs/UI_NAVIGATION_PEN_SIMPLIFICATION.md`.

# v1.0.143 - Search map focus + cluster reveal
- versionName 1.0.143 / versionCode 440.
- Search -> Map now filters invalid focus coordinates before opening the map and uses a safer center+zoom focus path for multi-result jumps.
- Search result sets on the map now keep viewport-aware clusters while the visible result count stays above the marker cap, instead of sampling down to 240 singles.

# v1.0.139 - Search cold render throttle
- versionName 1.0.139 / versionCode 436.
- Prevents first-search ANR when a place-name query returns many objects by rendering search result cards in small progressive batches.
- Keeps SourceFilter behavior from v1.7.117.

# SOV Android v1.0.134 — offline multi-select section cards

- versionName 1.0.134 / versionCode 431.
- Karte i slojevi now uses six compact multi-select cards in two rows: Maps, MBTiles, Imports, Waypoints, Tracks and Shared.
- The old horizontal section slider / Sve selector is removed from this screen logic.
- Multiple categories can be active at the same time, so the screen can show combined sections without switching away from the others.

# SOV Android v1.0.133 — shared layer visibility toggle

- versionName 1.0.133 / versionCode 430.
- Shared waypoints now use a distinct navy-blue diamond marker with a cyan rim.
- Shared tracks now use the matching navy-blue line color.
- No Sheet/API/schema/search/offline logic changed.

# SOV Android v1.0.89 — shared trip WeatherCity forecast field

- versionName 1.0.89 / versionCode 390.
- Added optional WeatherCity shared Sheet column for forecast geocoding.
- Added WeatherCity field to create/edit field trip forms.
- Shared trip weather now uses WeatherCity first, then trip location, then coordinate fallback.

# SOV Android v1.0.87 — shared trip RasporedUrl sync

- versionName 1.0.87 / versionCode 388.
- Added Sheet column M `RasporedUrl` through the updated Apps Script.
- Added `updateRasporedUrl` Apps Script action so Android can save the created car schedule URL back to the shared Sheet.
- Added Android client method `updateRasporedUrlOnSheet(...)`.
- Sheet trip cards now save newly created schedule URLs locally and back to Sheet, so other users receive the same `rasporedUrl` from `listTrips` instead of creating a new empty tab.
- Replace the existing trips Apps Script with `docs/scripts/FIELD_TRIPS_SHEET_WEBAPP_v1_0_87_RASPORED_URL.gs` and deploy a new version of the same web app URL.

## SOV Android v1.0.86 — offline-area weather coordinate center

- versionName 1.0.86 / versionCode 387.
- Shared trip coordinates now come from the downloaded offline map area.
- If caves/objects exist inside the offline bbox, Sheet centerLat/centerLon uses their average coordinates.
- If no objects exist inside the bbox, Sheet centerLat/centerLon falls back to the geometric bbox center.
- Existing v1.0.84 GS remains compatible because field names are unchanged.

## SOV v1.0.84 — Shared trip coordinates for weather
- Shared Izleti Sheet sync now sends centerLat/centerLon and min/max bbox coordinates when creating a trip.
- Sheet trip JSON now accepts coordinate fields so other users can fetch weather by coordinates, not only by text location/offline map metadata.
- SheetTrip weather now prefers Sheet coordinates, then local package coordinates, then text location geocoding fallback.
- Added Apps Script file `docs/scripts/FIELD_TRIPS_SHEET_WEBAPP_v1_0_84_SHARED_TRIP_COORDS.gs`.
- versionName 1.0.84 / versionCode 385.


## SOV v1.0.83 — Manual waypoint coordinates
- Added manual WGS84 / HTRS96-TM coordinate entry for creating map waypoints.
- Long-press waypoint dialog can now switch to manual coordinate correction.
- Confirmed manual coordinates create a waypoint and focus the map on the entered point.
- versionName 1.0.83 / versionCode 384.


## SOV v1.0.76
- Test-only update metadata bump: versionName 1.0.76 / versionCode 377.
- No functional code changes from v1.0.75.

# SOV v1.0.46 FULL — Speleo Runner character polish

- Version: 1.0.46 / versionCode 347.
- Added flat Sierra VGA headlamp beam before the player body.
- Improved player readability with stronger body outline/contact shadow.
- Walk bob is more visible and arms move opposite the legs.
- Crawl mode now adds small dust puffs in front/behind the speleologist.
- No gameplay, state, collision, map, field package, or tracking logic changed.

---

# SOV v1.0.16 FULL

Latest baseline: Speleo Runner bat reachability and crawl/rockfall obstacle readability patch. VersionCode 317.

# SOV 1.0.10 FULL

## Speleo Runner v1.0.8 — Cave morphology and random horizontal duration
- Added horizontal cave floor/ceiling morphology state.
- Horizontal phases now last randomly between 45 and 150 seconds.
- Horizontal cave drawing now uses visibly sloped ceiling and floor paths.
- HUD horizontal timer now shows remaining / total phase duration.
- Approach cue to vertical appears relative to the randomized phase duration.
- Version: 1.0.8 / versionCode 309.



## SOV 1.0.10 FULL — Speleo Runner BATS HUD visibility fix

- Added animated high-contrast `🦇 BATS X` pill on the second HUD row.
- Kept SCORE meters large and animated.
- Preserved WALK/DEPTH and gameplay counting logic.
## SOV 1.0.10 FULL - Speleo Runner HUD score visibility verification
- VersionCode 306 / versionName 1.0.7.
- Verified HUD score patch: SCORE is larger and animated, WALK/DEPTH are split, and HUD contrast is strengthened.
- Gameplay meter calculation was intentionally left unchanged.

## v1.7.112 — Trip create search normal mode

- Base: v1.7.111
- Changed MapFeature recordsToShow so trip creation / area-selection starts clean instead of rendering all database markers.
- APK target: SOV1.7.apk

## v1.7.112 — Search result-set marker fix

- Builds on v1.7.110.
- Search/filter result markers now render through a forced search-result marker plan independent of stale viewport/cache state.
- Search -> Map now keeps database records available even for “Prikaži sve” / no active-filter focus.
- About/version/update metadata bumped to 1.7.112 / versionCode 288.

## v1.7.109 — Search map marker repair

- Builds on v1.7.108.
- Fixes Search results on map: marker overlay now follows search/filter results instead of stale viewport cache.

## v1.7.109 — Search/database filter repair

- New master candidate after v1.7.107.
- Search/filter logic repaired and search cache bumped to v4.
- Root `update.json` added for GitHub release metadata.

## v1.7.82.4 Emergency map removal cleanup

Emergency Field Map / "Osnovna terenska karta Hrvatske" has been removed from the app UI and map selection flow. The MapLibre vector renderer dependency and emergency vector background files were removed. Existing offline maps, MBTiles imports, KML/GPX overlays, tracks, SOV logic, Izleti + Sheet sync and .sovpkg support remain unchanged.

# BUILD HISTORY — SOV App

## Active baseline
v1.7.90 — Overlay layer management cleanup

v1.7.89 — Field Package / Izleti refactor pass 1

## Recent build line
- v1.7.59 — Dangers/MSP/Opasnosti removed
- v1.7.60 — Map UX overhaul
- v1.7.61 — Search UX overhaul
- v1.7.62 — Premium search polish
- v1.7.63 — Premium search visual polish
- v1.7.64 — Clear ON/OFF filter states
- v1.7.65 — Search + Pokaži jame moved to top
- v1.7.66 — Baza first, removed intro + Uključeno sada
- v1.7.67 — Search and results merged
- v1.7.68 — Search result count shown only once
- v1.7.69 — Izleti premium UX overhaul
- v1.7.70 — New Trip card/wizard premium polish
- v1.7.71 — Sheet delete sync for Izleti
- v1.7.72 — Izleti category separation
- v1.7.73 — Delete own trips from Sheet
- v1.7.74 — Overlay grand overhaul
- v1.7.75 — Offline layers grand overhaul
- v1.7.76 — Offline icons compile fix
- v1.7.77 — Offline clean categories
- v1.7.88 — Offline/Overlays refactor pass 1
- v1.7.89 — Field Package / Izleti refactor pass 1

## Permanent rule
Dangers/MSP/Opasnosti features remain removed unless explicitly reintroduced by Darko.


## SOV 1.0.10 FULL - Speleo Runner HUD readability hotfix
- VersionCode 305 / versionName 1.0.4.
- Enlarged animated SCORE meter in HUD.
- Stronger HUD contrast and gold border.
- WALK and DEPTH split into separate animated metrics.


## SOV v1.0.79 — Release network/Gson safety fix
- Disabled release minify/shrink to keep signed APK behavior aligned with debug.
- Added extra Gson DTO keep rules for Sheet/weather/network parsing if minify is re-enabled later.
- update.json points to SOV1.0.79.apk / versionCode 380.

## v1.0.91
- Settings screen now has GPS, Kompas and Signal tiles.
- Added GPS Status screen with coordinates, accuracy, satellites and last fix.
- Added Compass screen with bearing, cardinal direction, smoothed needle and sensor accuracy.
- Added Signal i pokrivenost screen with live network info and local signal history map.

## v1.0.133 — Shared layer visibility toggle

- Added show/hide control for shared points and shared tracks in Zajednički slojevi.
- Multiple shared layers can remain visible on the map at the same time.
- The Open action still focuses the selected shared layer on the map.
- Preserves the v1.0.132 navy-blue diamond shared waypoint marker and matching shared track color.


## v1.0.147 - Transport table button fix
- Shared trip cards now always show the Raspored auta / transport table button.
- If the link is missing from Sheet/list response, the app uses local cached links and can create the transport tab on demand.
- Weather and past-trip archive behavior unchanged.

## v1.0.148 - Past shared trips archive cutoff fix
- Removed the extra 24-hour grace period from shared-trip past-date detection.
- Past shared trips now move out of the main list and into the collapsed Prošli izleti archive as soon as their end date/time is past.


## v1.0.150
- Added lightweight in-app PDF drawing viewer for downloaded Drive nacrti.
- Local PDF pages render as preview thumbnails and fullscreen zoomable images.
- Added pinch zoom/pan, page navigation, and external PDF fallback.

## v1.0.154 - PDF drawing metadata matching
- Based on v1.0.153 master.
- Added stronger Drive drawing index script and app-side match scoring.
- No changes to search, map centering, shared trips, weather or transport-table flow.
