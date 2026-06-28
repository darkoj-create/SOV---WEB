# SOV Web v6.1.32 — User portal header cleanup

Baseline: sov-web-build-v6.1.31-client-error-logs.zip

## Changed
- dashboard.html: gornji user portal header očišćen
- dashboard.html: uklonjeni su top nav linkovi Novosti, Karta, Izleti, Oprema, Dokumenti, Arhivar i profil chip iz glavne trake
- dashboard.html: ostavljene su samo dvije akcije: Home i Odjava
- dashboard.html: title tag ažuriran na v6.1.32

## Not changed
- Nema SQL promjena
- Nema APK promjena
- Nema promjena u Supabase/RPC pozivima
- Nema promjena u auth.js logout logici
- Nema promjena u izleti/oružar/arhivar business logici

## Note
- Ovo je svjesno UI-only čišćenje dashboard headera prema zahtjevu: user portal gore treba imati samo povratak na home i logout.
