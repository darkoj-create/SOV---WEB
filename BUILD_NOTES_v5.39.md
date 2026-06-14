# SOV Web v5.39 — Premium UI/UX consolidation

Ovaj build ne mijenja SQL shemu i ne dira bazu objekata.

## Fokus
- Novi role-aware SOV Cloud dashboard.
- Jasnije grupiranje modula: operativni moduli, interni sustavi, stanje sustava.
- Premium UI stil preko `assets/sov-premium-ui.css`.
- Dashboard helper `assets/sov-dashboard-premium.js` koji pokušava read-only učitati osnovne KPI brojeve iz Supabasea.
- SQL sandbox / Go Live linkovi ostaju samo Admin + SQL permission.
- Preview role switcher ostaje opt-in kroz postojeći `?preview=1` flow.

## Nema novog SQL-a
Ako si već pokrenuo v5.36.3, v5.37 i v5.38 SQL, za ovaj build nema dodatnog SQL-a.
