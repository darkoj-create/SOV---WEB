# SOV Admin 1.4.29a — Cloud login human icon gate

## Što je promijenjeno

- Cloud tab sada ima dodatni hard-gate i na samoj `cloud` ruti, ne samo na kliku taba.
- Ako korisnik nije prijavljen i klikne Cloud, aplikacija odmah otvara login screen.
- Nakon uspješne prijave aplikacija automatski vraća korisnika na Cloud.
- Login ekran sada koristi veliku ikonu korisnika / čovječuljka (`Icons.Default.AccountCircle`) umjesto SOV logotipa.
- Tekst login ekrana je jasniji: “Prijava za SOV Cloud” i “Prijavi me i otvori Cloud”.
- Gumb za već prijavljenog korisnika sada kaže “Nastavi u Cloud”.

## Što nije dirano

- Supabase sign-in poziv nije mijenjan.
- Session spremanje nije mijenjano.
- Role/permissions sync nije mijenjan.
- Oružarstvo sync iz 1.4.29 ostaje.
- Nema SQL promjena.

## Datoteke

- `app/src/main/java/com/darko/speleov1/SovCloudLoginScreen.kt`
- `app/src/main/java/com/darko/speleov1/SpeleoAppRoot.kt`
- `app/build.gradle.kts`
- `update.json`
