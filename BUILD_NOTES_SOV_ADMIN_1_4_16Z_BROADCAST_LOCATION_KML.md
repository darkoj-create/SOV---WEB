# SOV Admin 1.4.16z — Broadcast location KML

- Added location sharing inside map/team broadcast sheet.
- Button: `Pošalji moju lokaciju kao KML`.
- Location messages are stored as compact text payloads and rendered as location cards.
- Each location card has `Podijeli KML` action.
- No SQL change required; uses existing `sov_trip_messages.message_text`.
- Fixed duplicate `val sent` in pending message flush from previous source.

VersionCode: 900102
VersionName: 1.4.16z-broadcast-location-kml
