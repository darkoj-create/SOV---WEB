# SOV web v5.58.10 — Cloud User UI oprema + članski članci

## Promjene
- Dashboard User view više nije previše ogoljen: dodani su Oprema i Napiši članak.
- Oprema je vidljiva članu i vodi na postojeću člansku pretragu oružarstva / zahtjeve.
- Novi `napisi-clanak.html` je user-facing forma za predaju članka uredniku.
- Članski članak se sprema kao `sov_news.published = false` i `source = user-submission-v5.58.10`.
- Urednik/Admin ga kasnije vidi u `news-editor.html`, doradi i objavi.
- KML/GPX upload i Dokumenti su označeni kao sljedeći user moduli, bez polovične implementacije.
- Maknut je `assets/sov-inbox.js` s dashboarda zbog donjeg notification artefakta.
- `sync-status.html` je usklađen s v5.58.10.

## SQL
Pokrenuti: `SUPABASE_SOV_NEWS_USER_ARTICLES_v5_58_10.sql`

## APK
Nema APK promjene — ovo je Cloud web/UI + Supabase RPC.
