# SOV web build 6.1.45ai

Build: `sov-web-build-v6.1.45ai-member-toolbar-unification`

## Što je novo

- Ujednačen je gornji članski toolbar na radnim modulima.
- Toolbar sada ima samo tri akcije: **Javni sajt**, **Dashboard**, **Odjava**.
- Logo lijevo vodi na `dashboard.html`.
- Odjava koristi postojeći `data-logout` / `SOVAuth.logout()` mehanizam iz `assets/auth.js`.
- Nema promjena poslovne logike, API/RPC poziva ni SQL-a.

Detaljan prije/poslije pregled izmijenjenih headera: `SOV_HEADER_NAV_REPORT_v6_1_45ai.md`.
