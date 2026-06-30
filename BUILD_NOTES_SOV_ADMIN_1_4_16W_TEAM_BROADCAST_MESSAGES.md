# SOV Admin v1.4.16w — Team Broadcast Messages

Baseline: v1.4.16v Teams Flow.

## Added
- APK-only lightweight team/trip broadcast messages.
- Messages appear inside the active trip Team/Tracking card.
- UX: compact `💬 Poruke ekipe` entry, modal panel, target chips `Moj team` / `Svi`.
- Local pending queue for weak/no signal; pending messages show `čeka`.
- Poll/refresh while the messages panel is open.

## Fixed
- Compile error in `FieldPackageFeature.kt`: nullable `pkg.weatherCity` now uses `orEmpty()`.

## SQL required
Run `SUPABASE_SOV_TRIP_MESSAGES_v6_1_25.sql`.

## Not changed
- No web UI for messages.
- Existing teams flow, trips flow, armory, documents, and web are not changed.
