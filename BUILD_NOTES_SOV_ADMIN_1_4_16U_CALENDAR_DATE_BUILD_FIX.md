# SOV Admin 1.4.16u — Calendar date build fix

Base: 1.4.16t trips simplified edit.

Fixed:
- `HomeAndToolsScreens.kt`: removed ambiguous inline `Date(...)` expressions in calendar event save flow.
- Parses start/end date into explicit `Long` values before creating `java.util.Date`.
- Prevents Kotlin overload ambiguity on `Date(Long/String)`.

Version:
- versionCode 900097
- versionName 1.4.16u-calendar-date-build-fix
