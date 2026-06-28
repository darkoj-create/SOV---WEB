# SOV Web v6.1.44f — Documents page cleanup

Baseline: v6.1.44e Gmail native zapisnici.

## Changed
- Rebuilt `dokumenti.html` into a simple, clean document hub.
- Main visible entries are now:
  - `zapisnici-native.html` — native archive of meeting minutes
  - `zapisnici-najave.html` — announcements extracted from minutes
  - `zapisnici-skupstine.html` — assemblies/decisions
  - simple help/tutorial links
- Removed duplicate/legacy clutter from the main Documents page:
  - Aktualni zapisnici 2026
  - Arhiva zapisnika 2017–2022
  - Cijela arhiva zapisnika
  - Pregled zapisnika
  - Novi zapisnik

## Not changed
- No SQL changes.
- No APK changes.
- No Izleti/Oružarstvo/Arhivar/Karta changes.
- Old pages are still present in the build for compatibility, just no longer promoted on `dokumenti.html`.
