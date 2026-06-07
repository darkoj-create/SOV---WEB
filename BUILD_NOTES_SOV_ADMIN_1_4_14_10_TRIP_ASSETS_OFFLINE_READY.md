# SOV Admin 1.4.14.10 — Trip Assets offline-ready

- Trip Assets više nisu samo cloud download: app sprema pakete u lokalni `filesDir/sov_trip_assets`.
- Prije downloadanja provjerava postoji li lokalni file s istom veličinom/checksumom.
- Ako je paket već preuzet, app ga ne skida ponovno i pokazuje `Offline spremno`.
- Download status ostaje nakon updatea aplikacije jer je file u app persistent storageu.
- Dodan poziv `sov_trip_asset_mark_downloaded` kad se paket preuzme.
