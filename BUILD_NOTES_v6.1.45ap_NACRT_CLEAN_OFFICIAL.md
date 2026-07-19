# SOV Web v6.1.45ap — Nacrt Clean Official

## Aktivni renderer
- `nacrt-core.js`
- `nacrt-tdr-fix.js`
- `nacrt-v2.js`
- `nacrt-branding.js`

Teški v3/v4 slojevi za automatsko farbanje nisu aktivni.

## Nacrti
- nema generičke smeđe ispune jame ili terena
- materijali se prikazuju crno-bijelim speleološkim uzorcima
- crvena poligonala i stanice ostaju zbog čitljivosti mjerenja
- službeni SOV i PDS Velebit logotipi koriste uploadane službene slike
- logotipi se pri učitavanju pretvaraju u data URI kako bi ostali u spremljenom SVG/PNG izlazu
- uklonjen je generator/debug footer

## Cloud pristup
- `nacrt.html` traži odobrenu prijavu
- neprijavljeni korisnik preusmjerava se na login
- kartica „Nacrt Generator” automatski se dodaje na glavni SOV Cloud dashboard svim odobrenim ulogama

## Stabilnost
Nema Canvas petlji ni per-pixel obrade. Sloj koristi samo SVG/string post-processing i ne može ponoviti v4 browser freeze.
