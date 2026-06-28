# SOV web v6.1.45w — Login route restore

Fix za nestalu/zaobiđenu login stranicu nakon public/dashboard polish buildova.

## Root cause
- `assets/sov-public-header-v6145q.js` je linkove na `login.html` preusmjeravao na `dashboard.html`.
- `vercel.json` je clean URL `/prijava` rewriteao na `/dashboard.html`.
- `login.html` je postojao, ali su ulazi prema njemu bili pokvareni.

## Fix
- `login.html` opet ostaje stvarna prijava.
- public header `Članska zona/Prijava` vodi na `login.html`.
- `/prijava`, `/login` i `/clanska-zona` vode na `login.html`.
- dodani fizički fallback folderi: `login/index.html`, `prijava/index.html`, `clanska-zona/index.html`.

## SQL
Nema SQL promjena.
