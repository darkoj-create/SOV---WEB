# SOV Admin APK 1.4.12 — Trips signups + transport

Companion APK build for Web 5.56.

Changes:
- Trip signup dialog now matches Supabase `sov_trip_signup` RPC.
- Signup fields: attendance (idem / možda / ne idem), transport mode (trebam prijevoz / imam auto / snalazim se), seats, departure place, note.
- Trip cards now open an in-app **Prijave i prijevoz** dialog instead of the legacy Google Sheet car schedule.
- Transport summary reads from `sov_trip_members_transport_view`.
- Google Sheets / Apps Script remain out of the active trips backend.

Requires SQL:
- `SUPABASE_SOV_TRIPS_CLOUD_v5_49_ALL_IN_ONE.sql`
- `SUPABASE_SOV_TRIPS_SIGNUPS_TRANSPORT_v5_56.sql`
