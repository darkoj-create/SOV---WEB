# SOV Admin v1.4.32b — Speleo Runner LAKE build fix

Base: v1.4.32a Speleo Runner LAKE redesign.

## Fix
- `SpeleoRunnerScreen.kt`: fixed LAKE draw compile errors around the boat/paddle section.
- Added missing `lakePaddleTimer` parameter forwarding into `drawLakeCave(...)`.
- Converted new trigonometry expressions in the LAKE draw code to explicit `Float` values (`sin(...toDouble()).toFloat()`, `cos(...toDouble()).toFloat()`) so Compose `Offset`, `Size`, `drawLine`, `drawOval`, and `Color.copy(alpha)` receive `Float`, not `Double`.

## Functional intent
- No gameplay/server/leaderboard/prefs changes.
- This is only a compile/build fix for the LAKE redesign.

## Test
- Run `./gradlew :app:compileDebugKotlin` and `./gradlew :app:assembleDebug`.
- Manual test: LAKE rhythm, steering, rock hit, ambush, game over, restart, leaderboard.
