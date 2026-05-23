# Build notes

## v1.5 rule
Ne pregaziti javni landing, SOV Cloud ni SOV Karta prilikom uređivanja pojedinačnih javnih stranica. Ovaj build mijenja samo povijest i dokumentaciju verzije.

# BUILD NOTES — SOV WEB v1.4

Ovaj build je recovery build. Ne smije se pregaziti javni portal samo dashboardom.

Mora uvijek sadržavati:
- OG cinematic landing `index.html`
- javne stranice: `o-drustvu.html`, `povijest.html`, `velebitaski-duh.html`, `vijesti.html`, `speleoskola.html`
- članski dio: `login.html`, `dashboard.html`, `admin-users.html`
- SOV karta: `baza.html` + `data/sov-baza.json`
- original WordPress arhivu u `archive-original-wordpress/`

Pravilo: svaka nova verzija mora biti FULL SITE build, ne parcijalni overwrite.
