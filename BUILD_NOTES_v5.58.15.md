# SOV web v5.58.15

- `baza.html` preimenovan u `karta.html`; `baza.html` je redirect.
- Karta UI očišćen za user-friendly desktop i mobile browser.
- Uklonjeni SQL/dev linkovi, offline MBTiles dev alat, veliki counteri i inbox artefakt.
- Dodan `assets/sov-map-db.js` i SQL RPC `sov_map_objects()` za čitanje aktualne Supabase baze objekata.
- `sync-status.html` usklađen na v5.58.15 i dodana provjera Karte.

Prvo pokrenuti: `SUPABASE_SOV_MAP_v5_58_15_KARTA_REAL_DB.sql`, zatim deploy web ZIP.
