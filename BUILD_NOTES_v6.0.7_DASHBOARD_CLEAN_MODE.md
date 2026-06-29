# SOV web v6.0.7 — Dashboard clean mode

Base: `sov-web-build-v6.0.6-oruzarstvo-single-boot.zip`

## Cilj
Dashboard više ne smije izgledati kao dev build. Korisnik prvo vidi jednostavan app ulaz, role vide svoje radne alate, a Webmaster tehniku tek kad otvori skriveni sustavski blok.

## Promjene
- `dashboard.html` reorganiziran u 3 zone:
  - **Moje stvari** — karta, predaja nove jame, izleti, tracking, oprema, članak.
  - **Rad po ulozi** — oružar, arhivar, urednik, admin operativa.
  - **Sustav** — Webmaster-only details blok za sync, role, audit i SQL.
- Dodan `assets/sov-dashboard-clean-v607.css`.
- Adminu su ostavljeni operativni alati, bez SQL/sync/audit šuma.
- Webmaster alati više nisu odmah vidljivi kao glavna kartica na dashboardu.
- `update.json`, `VERSION.txt`, `BUILD_VERSION.txt`, `sync-status.html` usklađeni na v6.0.7.

## SQL
Nema SQL promjena.

## Rollback
Vrati ZIP v6.0.6 ako dashboard layout napravi problem. Baza se ne dira.
