# SOV Admin 1.4.16r — Calendar build fix

Baseline: `sov-admin-v1_4_16q-calendar-multiday-date-fix-source.zip`

## Fixed
- `FieldPackageFeature.kt`: added missing Compose imports used by month controls:
  - `Icons.Default.ArrowBack`
  - `Icons.Default.ArrowUpward`
  - `Modifier.rotate(...)`
  - `TextAlign.Center`
- `HomeAndToolsScreens.kt`: fixed invalid Kotlin regex strings in calendar date parser.
- `HomeAndToolsScreens.kt`: custom event dialog now validates dates through `sovCalendarParseDateMillis(...)`, so `dd/mm/yyyy` is accepted as intended.

## Version
- versionCode: 900094
- versionName: 1.4.16r-calendar-build-fix
