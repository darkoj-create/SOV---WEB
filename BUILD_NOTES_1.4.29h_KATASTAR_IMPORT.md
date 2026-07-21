# SOV Admin 1.4.29h — Katastar import

## Baseline
- Based on `1.4.29g-native-folder-scan`.

## Added
- Imported `CroSpeleo - objekti.xlsx` into app asset:
  - `app/src/main/assets/katastar_crospeleo_2026_android_v1.json.gz`
  - 6317 objects
  - 6317 valid HTRS96/TM → WGS84 coordinates
- Added Katastar as separate record source:
  - `source = "katastar"`
  - `source_labels = ["katastar"]`
- Search source filter now supports:
  - SOV
  - Katastar
  - Moja baza
  - Sve

## Access rule
- Katastar is loaded into UI only when:
  - user has an app session, and
  - permissions allow `can_view_katastar`.
- If user taps Katastar without login, app opens login screen.
- SOV base and Moja baza remain as before.

## Supabase
- Created table `public.sov_crospeleo_katastar_objects`.
- RLS enabled.
- `anon` access revoked.
- `authenticated` has read access.

## Kept
- Native folder scan from 1.4.29g.
- Laptop hub in Cloud.
- Runner SQL scores.
- Cloud login gate.
- Oružarstvo sync.

## Not changed
- SOV local base is not overwritten.
- My Base import logic is not changed.
- No existing user files are removed.
