# SOV Admin v1.4.16s — Calendar date compile fix

Hotfix based on v1.4.16r.

## Fixed
- `HomeAndToolsScreens.kt`: fixed calendar custom-event date fallback.
- Replaced invalid `selectedDayMillis` reference inside `SovCalendarAddEventDialog` with `initialDateMillis`.
- This removes Kotlin overload ambiguity for `java.util.Date(...)` and unresolved `selectedDayMillis` compile errors.

## Version
- versionCode: 900095
- versionName: 1.4.16s-calendar-date-compile-fix
