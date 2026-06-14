# SOV web v6.0.9 — Predane jame workflow hardening

Base: v6.0.8 public site polish.

## Cilj
Predaja nove jame i Arhivarski inbox trebaju biti workflow, ne samo formular. Korisnik mora jasno vidjeti što šalje, Arhivar mora jasno vidjeti status, falinke i export.

## Promjene
- `predaj-novu-jamu.html` dobio završeni step-by-step UX, autosave, lokalni draft info i upload progress.
- Dodan novi `assets/sov-submission-page-v609.js`.
- Dodan novi `assets/sov-form-v609.css`.
- Popravljen stari problem gdje je forma mogla referencirati nepostojeći `sov-submission-page-v605.js`.
- `arhivar-predane-jame.html` dobio status countere i brzi pipeline filter.
- `assets/arhivar-submissions-review.js` sada ima v6.0.9 status countere i safe fallback review update.
- Dodan `assets/arhivar-submissions-v609.css` za čistiji inbox.
- XLSX/CSV/XML/ZIP export ostaje.
- `update.json`, `VERSION.txt`, `BUILD_VERSION.txt`, `sync-status.html` dignuti su na v6.0.9.

## SQL
Optional SQL: `SUPABASE_SOV_PREDANE_JAME_WORKFLOW_HARDENING_v6_0_9.sql`.

Patch je idempotentan i aditivan: dodaje review/status kolone ako fale, indekse i safe RPC `sov_update_speleo_submission_review()`.

## Rollback
Ako UI zapne, vrati v6.0.8 ZIP. Ako je SQL pokrenut, rollback SQL-a nije potreban jer su dodane samo sigurne kolone/funkcije.
