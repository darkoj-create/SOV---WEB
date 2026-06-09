# SOV Web v6.1.29 — Oružar: jednostavnost i inventura

Baseline: sov-web-build-v6.1.28-oruzar-arhivar-ux-fixes.zip

## Changed
- dashboard.html: dodan oružar panel s direktnim linkovima na Posudbe, Inventar, Inventura
- oruzar-master.html: hero copy → actionable, brand link → oruzar-master.html
- oruzar-master-inventura.html: kompletna obnova statičnog HTML-a iznad inventoryRoot
  - dodana uputa s 3 koraka
  - labelani inputi (datum, napomena)
  - čišći copy na gumbima
  - fallback loading state u inventoryRoot
  - cm-kpis display:none uklonjen + :empty fallback
- oruzar-master-inventura.html: hero copy promijenjen
- oruzar-master-posudbe.html: hero copy, brand link, fallback za loansRoot
- oruzar-master-inventar.html: hero copy, brand link, export gumb copy

## Not changed
- Nema SQL promjena
- Nema APK promjena
- Nema promjena u vanjskim JS fileovima (oruzar-master-clean.js, oruzarstvo-supabase.js)
- Nema promjena u onclick handlerima

## Out of scope
- Oružar redirect na master.html kao home — zahtijeva auth.js promjenu
- Tri ulaza za inventuru — arhitekturalni problem
- Prazni KPI panel dok JS ne učita — renderMaster() optimizacija
