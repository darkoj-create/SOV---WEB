# SOV Web v6.1.39j — User loan packages flow

Datum: 2026-06-14
Baseline: v6.1.39i real XLS workbook + v6.1.39h user catalog fix

## Problem
User dio Oružarstva imao je nejasne "Brze pakete" koji su bili doslovno kategorije i mogli su previše/random puniti zahtjev ili odmah slati zahtjev.

## Promjena
- Preimenovano i preoblikovano u **Predloženi paketi**.
- Paketi sada imaju jasne opise i dodaju samo razumne predstavnike iz grupa opreme.
- Paket NE šalje zahtjev automatski.
- Paket samo napuni drawer **Moj zahtjev**.
- Korisnik prije slanja može:
  - maknuti stavke,
  - promijeniti količine,
  - upisati izlet/razlog,
  - dopisati napomenu.
- Individualni gumb **Zatraži** također sada dodaje u košaricu/drawer umjesto automatskog slanja pojedinačnog zahtjeva.

## Paketi
1. Osobna oprema
   - kaciga/pojas
   - Blokeri i Croll
   - Pupci
   - Descender
   - Centralni/pomoćni karabineri
2. Sidrenje i opremanje
   - karabineri
   - sidrišne pločice
   - Fix / Spit
   - zaštita užeta
   - alat za opremanje
3. Mjerenje i crtanje
   - mjerenje
   - crtanje
   - kompas/busola
   - dokumentacija
4. Rasvjeta i elektronika
   - rasvjeta
   - baterije/punjači
   - komunikacija
   - elektronika

## Nije dirano
- SQL
- artikli
- količine
- lokacije
- posudbe backend
- auth
- XLS export

## Cache bust
- `assets/oruzarstvo-boot-v615.js?v=6.1.39j-user-loan-packages`
