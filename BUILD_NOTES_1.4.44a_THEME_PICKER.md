# SOV Admin v1.4.44a — Theme picker

Bazirano na: `v1.4.43a-accessibility-labels`

## Opseg

FAZA 3 / 3.2 — izbor teme aplikacije.

## Promjene

- Dodan `AppThemeMode` s opcijama:
  - `SYSTEM` — prati sistemsku temu uređaja
  - `LIGHT` — svijetla tema
  - `DARK` — tamna tema
- Default je `DARK`, da postojeći korisnici nakon nadogradnje ne vide neočekivanu promjenu.
- `AppSessionStore` sprema i učitava odabir teme u postojeći `session_json`.
- `MainActivity` više ne koristi hardkodirani `SpeleoTheme(darkTheme = true)`, nego odabir korisnika + `isSystemInDarkTheme()`.
- Settings ekran ima novu karticu "Tema aplikacije / App theme".

## Namjerno nije dirano

- Nema masovnog refactora boja po ekranima.
- Nema promjena u Supabaseu, self-updateu, Apps Scriptu ni mrežnom sloju.
- `CaveScreenBackground` ostaje vizualno isti da build ne promijeni identitet aplikacije preko noći.

## Test

1. Pokreni app — default mora ostati tamna tema.
2. Settings → Tema aplikacije → Svijetla.
3. Zatvori i ponovno otvori app — izbor mora ostati spremljen.
4. Settings → Sustav i promijeni sistemsku temu uređaja.
5. Provjeri čitljivost: Home, Settings, Search, Map top controls, Oružarstvo osnovne kartice.
