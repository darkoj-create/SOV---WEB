# SOV Admin APK 1.4.3 — Armory Screens + Return Flow

Scope: APK-only continuation after Web 5.45 / APK 1.4.2 core cleanup.

## Changes
- Keeps Oružarstvo as four clear modes: Katalog, Moji zahtjevi, Oružar red, Inventura.
- Oružar red no longer uses the dead approval step; requests go directly to Izdano.
- Adds per-item return flow in APK:
  - each request item shows returned / borrowed quantity,
  - destination chips: Oružarstvo, U jami, Kod nekoga, Rashod,
  - full return -> Vraćeno, partial return -> Djelomično vraćeno.
- Request cards now display multi-item lines from `equipment_request_items`.
- Return details are saved into request notes and request item notes where possible, without requiring extra SQL columns.
- Inventory remains raw/offline-first and is not affected by grouped catalog browsing.

## Version
- versionCode: 900034
- versionName: 1.4.3-armory-screens-return-flow

## Notes
No new Supabase SQL is required for this APK patch. It uses the existing v5.45 request tables and status model.
