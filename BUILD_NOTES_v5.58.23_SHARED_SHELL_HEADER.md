# SOV Web v5.58.23 — Shared shell/header convergence

Ovaj build nastavlja UX foundation cleanup iz v5.58.22.

## Promjene

- Dodan `assets/sov-shell-v55823.css` i `assets/sov-shell-v55823.js`.
- App/admin/module stranice sada dobivaju zajednički shell sloj za header, aktivni link i mobile drawer.
- Stari header sustavi (`sov-top`, `aw-top`, `as-top`, `cm/topbar`, oružarski headeri) vizualno se normaliziraju u isti SOV Cloud stil.
- Na mobitelu se header više ne oslanja samo na horizontalni scroll: dodan je hamburger/drawer s role-aware linkovima.
- Dodan skip-link za pristupačnost na operativnim stranicama.
- Webmaster/SQL linkovi se dodatno skrivaju na UI razini za role koje nisu Webmaster. Ovo je UX guard; prava i dalje moraju ostati u Supabase RLS/auth sloju.
- `auth.js` registered pages lista proširena je na Oružar Master i audit stranice.
- `sync-status.html` dignut je na v5.58.23.

## SQL

Nema novog SQL-a.

## APK

Nema APK promjena.
