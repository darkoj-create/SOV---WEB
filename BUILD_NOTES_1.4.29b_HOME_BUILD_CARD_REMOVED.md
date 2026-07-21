# SOV Admin v1.4.29b — Home build card removed

## Promjena

- Uklonjena je vidljiva kartica/pill s imenom builda s glavnog ekrana aplikacije.
- Build/verzija i dalje ostaju dostupni u ekranu **O aplikaciji**, nisu više na main screenu.
- Cloud login gate iz v1.4.29a ostaje bez promjena.
- Oružarstvo sync iz v1.4.29 ostaje bez promjena.

## Tehnički

- Datoteka: `app/src/main/java/com/darko/speleov1/HomeAndToolsScreens.kt`
- Uklonjen samo Compose `Text(text = SOV_DISPLAY_VERSION, ...)` blok u `HomeScreen`.
- Nisu dirane rute, login logika, Supabase/API pozivi ni poslovna logika.

## SQL

- Nema SQL promjena.
