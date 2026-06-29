# SOV web v6.1.45ai — jedinstveni članski toolbar

## Cilj

Gornji toolbar na članskim i radnim modulima sveden je na isti minimalni oblik:

1. **Javni sajt** → `index.html`
2. **Dashboard** → `dashboard.html`
3. **Odjava** → postojeći `data-logout` / `SOVAuth.logout()` flow

Logo lijevo vodi na `dashboard.html`.

## Tehnički opseg

- Dodan je zajednički stil: `assets/sov-member-header-v6145ai.css`.
- U radnim HTML stranicama zamijenjen je samo gornji header/nav blok.
- `assets/arhivar-simplify-v6145ad.js` više ne ubacuje stari veliki arhivarski nav ako nova zajednička traka već postoji.
- `sov-version.js`, `VERSION.txt`, `BUILD_VERSION.txt` i `update.json` su usklađeni na `6.1.45ai`.

## Bez promjene

- Nema SQL-a.
- Nema izmjena API/RPC poziva.
- Nema promjene poslovne logike modula.
