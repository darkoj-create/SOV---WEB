# SOV web v6.1.40a — Zapisnici najave parser fix

Baseline: v6.1.40, created from uploaded v6.1.39k baseline.

## Fixed
- Improved parsing of NAJAVE rows from meeting-minutes DOCX files.
- Better date parsing for:
  - `31.05., Lokacija – opis`
  - `06.06. – 07.06., Lokacija – opis`
  - `4.-7.6., Lokacija – opis`
  - `20. – 21. 6. 2026. Lokacija: opis`
  - `10.-12.-7. Bunovac - opis`
- Better title/location detection for real SOV examples:
  - Homoljačko polje kod Plitvica
  - Žumberačko gorje
  - Ponorac
  - Duman / Duman i Burinka
  - Munižaba
  - Burinka
  - Žica
  - Krasno
  - Ratkovo
  - Bijele sige na Medvednici
  - Sjeverna Makedonija (BCC)
  - Sjeverni Velebit
- Fixed Unicode word-boundary issue for Croatian names with Ž/Č/Ć/Š/Đ.
- Re-importing the same DOCX now refreshes non-approved staging rows with better parsed rows.

## Not changed
- No SQL migration.
- No changes to Oružarstvo.
- No changes to trips/calendar storage.
- No Gmail automation yet.

## Cache bust
- `assets/zapisnici-najave.js?v=6.1.40a-announcement-parser`
