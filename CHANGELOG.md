# Changelog

## v1.2 — login + connected full site (2026-05-23)
- Povezan javni portal: Naslovnica, O društvu, Povijest, Velebitaški duh, Prijava.
- Članska zona više ne otvara dashboard direktno nego ide kroz `login.html`.
- Dodan pravi frontend login/registracija flow preko `assets/auth.js`.
- Registracija kreira korisnika sa statusom `pending`.
- Dodan `admin-users.html` za approval/reject korisnika.
- Zaštićene članske stranice: dashboard, SOV karta/baza, izleti, oružarstvo, dokumentacija, pregled baze.
- Sačuvane originalne WordPress arhive u `archive-original-wordpress/` da se više ne izgube puni tekstovi.

## Pravilo builda
Nikad više ne slati parcijalni build koji pregazi javni portal. Svaki ZIP mora sadržavati javni dio + SOV Cloud + dokumentaciju verzije.

