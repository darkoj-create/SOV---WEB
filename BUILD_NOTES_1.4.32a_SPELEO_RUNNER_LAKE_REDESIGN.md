# SOV Admin 1.4.32a — Speleo Runner LAKE redesign

Base: 1.4.31a maps-wms-perf source, including previous trip-card, Runner perf/graphics and Maps/WMS patches.

Scope: `app/src/main/java/com/darko/speleov1/SpeleoRunnerScreen.kt` only. No leaderboard, Supabase, score submission, high-score prefs or non-LAKE phase logic was intentionally changed.

## Implemented

- LAKE paddling no longer uses identical mash buttons.
  - Left button: `⟵ VESLAJ`, right button: `VESLAJ ⟶`.
  - Alternating left/right strokes build rhythm and stronger boost.
  - Repeating the same side loses rhythm and steers the boat to the opposite lane.
- Added 3 LAKE lanes with smoothed lane interpolation.
- LAKE duration raised to 14 seconds.
- Re-enabled lane rocks as non-lethal obstacles.
  - Rock hit resets paddle boost/rhythm, pulls the cave monster closer, shakes screen and removes the rock.
  - Game over remains only from the cave monster.
- Reworked cave-monster chase/escape into chase + ambush loop.
  - Escape no longer removes danger forever.
  - Ambush has a 1-second lane warning.
  - Ambush is disabled near the final beaching window.
- Golden shells remain active during the whole LAKE phase.
- Added first-time LAKE tutorial hint through prefs key `lake_tutorial_seen`.
- Replaced shark visual with a giant Proteus/čovječja ribica style using Sierra palette-derived colors.
- Reworked lake water into palette-driven Sierra/VGA bands, pixel sparkles, ink outlines and palette HUD.
- Added NEMAN gap meter and danger tint.
- Added final beaching/exit light during last 2 seconds.

## Files changed

- `app/src/main/java/com/darko/speleov1/SpeleoRunnerScreen.kt`
- `app/build.gradle.kts` version bump to `1.4.32a-lake-redesign`.

## Notes

The current source still uses the existing Compose state style for Runner simulation, not the full `RunnerSimState` refactor. New LAKE variables were therefore added in the same state style to avoid mixing a partial sim-state refactor with gameplay changes.

No new dependencies were added.

## Build/test

Build could not be verified in this sandbox if Gradle wrapper download is unavailable. Test locally:

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:assembleDebug
```

Manual test matrix:

- HORIZONTAL → TRANSITION → VERTICAL → EXIT_VERTICAL → LAKE → HORIZONTAL
- LAKE alternating strokes vs repeated same-side strokes
- LAKE rock hit penalty
- LAKE chase game over
- LAKE ambush warning and avoid-by-lane-change
- Pause/restart during LAKE
- High score and leaderboard after game over
