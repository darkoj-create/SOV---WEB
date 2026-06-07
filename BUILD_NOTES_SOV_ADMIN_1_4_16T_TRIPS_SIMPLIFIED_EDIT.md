# SOV Admin 1.4.16t — Trips simplified edit

Baseline: `sov-admin-v1_4_16s-calendar-date-compile-fix-source.zip`.

## Changes

- Removed category filter chips from the APK Izleti monthly dashboard.
- Monthly Izleti list now shows all trip/event categories in the selected month.
- Simplified trip edit dialog:
  - removed separate trip name field
  - removed weather city field from edit UI
  - keeps only date, leader, location, category/goal and description
  - title is regenerated from location + category/goal
- Simplified new trip dialog copy:
  - removed wizard/helper text blocks
  - removed optional add-ons block from the basic create form
  - new trip form stays focused on date, leader, location, weather city, category and description

## Version

- versionCode: 900096
- versionName: 1.4.16t-trips-simplified-edit

## Not changed

- Web was not changed.
- SQL was not changed.
- Trip delete hard fix, session refresh fixes, calendar date fixes and weather fixes are preserved from previous builds.
