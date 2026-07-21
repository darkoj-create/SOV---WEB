# SOV Admin 1.4.30a — Speleo Runner performance + graphics pass

Base: v1.4.29j linked SOV folder + trip-card UI reorg source.

## Scope
File changed: `app/src/main/java/com/darko/speleov1/SpeleoRunnerScreen.kt`.

## Applied changes

1. **Reduced per-frame allocation in obstacles/tokens**
   - `RunnerObstacle` and `RunnerToken` changed from immutable `data class` copies to mutable classes for runtime x-position updates.
   - Horizontal and lake movement now mutates `x` in-place and reuses existing buffers instead of creating `copy(x = ...)` objects every frame.

2. **Scanlines optimized**
   - `drawPaperGrain()` now draws scanlines with one cached `BitmapShader`/native `drawRect` instead of a `drawLine` loop across the full screen.
   - Shimmer particles reduced from 28 to 10 per frame.

3. **Bitmap cave backgrounds activated**
   - `speleo_runner_horizontal_bg_01..05.webp` and `speleo_runner_vertical_bg_01..05.webp` are loaded in `SpeleoRunnerScreen()`.
   - `RunnerCanvas()` passes the active biome bitmap into horizontal and vertical draw functions.
   - `drawAiCaveHorizontalBackground()` and `drawAiCaveVerticalBackground()` are now used when bitmaps are available; procedural fallback remains.

4. **Input responsiveness**
   - Added jump buffer (`0.12s`) and coyote timer (`0.08s`) for horizontal jumping.
   - Existing vertical/lake/transition controls remain unchanged.

5. **Delta-time handling**
   - Removed `_smoothedRunnerDt` usage and the previous hard `0.008..0.028` dt clamp that could make the game feel like slow-motion under load.
   - Spike guard remains through `coerceIn(0.001f, 0.05f)`.

## Not changed

- Supabase leaderboard client and prefs keys are untouched.
- Game phases, scoring constants, leaderboard submit/refresh, high score prefs and public UI strings are preserved.
- No new dependencies.

## Build note

`./gradlew :app:compileDebugKotlin --no-daemon` could not run in this sandbox because Gradle wrapper download requires `services.gradle.org`, which is blocked by offline/DNS restrictions here. Build locally with Android Studio/Gradle cache available.
