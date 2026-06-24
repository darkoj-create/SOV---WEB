# SOV Web v6.1.28 — Oružar i Arhivar UX fixes

Baseline: sov-web-build-v6.1.27-ui-fixes.zip

## Changed

### Oružar
- oruzarstvo.html: "Brzi paketi" sakriveni za role-oruzar i role-admin (CSS)
- oruzarstvo.html: gumb "Otvori oružarski dio" → "Upravljaj opremom"
- oruzarstvo.html: command card copy ažuriran
- oruzarstvo.html: role-card description za oružara via CSS ::before
- oruzar-master.html + sub-stranice: dodani linkovi "Izleti" i "Karta" u cm-nav
- oruzar-master.html + sub-stranice: uklonjen cm-kpis display:none; prazna KPI sekcija se skriva samo ako je stvarno prazna
- oruzar-master-posudbe.html: fallback poruka u loansRoot
- oruzarstvo.html: ažuriran title tag

### Arhivar
- arhivar-dashboard.html: "Logika sustava" zamijenjeno čitkim opisom toka
- arhivar-dashboard.html: "Testiraj predaju" → "Otvori kartu"
- arhivar-predane-jame.html: "Predaj testnu jamu" → "Predaj jamu"
- arhivar-predane-jame.html: ažuriran title tag
- predaj-novu-jamu.html: "Skini draft JSON" → "Spremi skicu lokalno"

## Not changed
- Nema SQL promjena
- Nema APK promjena
- Nema promjena u vanjskim JS fileovima
- Nema promjena u Supabase RPC pozivima
- Nema promjena u onclick handlerima

## Out of scope (needs JS or architecture changes)
- Dupli sustav oruzarstvo.html tabovi vs oruzar-master.html — arhitekturalni problem
- Role flash "Član"→"Oružar" — zahtijeva promjenu applyRole() funkcije
- KPIs skriveni na oruzarstvo.html line 210 — istražiti intentional vs bug
