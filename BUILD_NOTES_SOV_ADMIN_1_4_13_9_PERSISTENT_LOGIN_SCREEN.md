# SOV Admin 1.4.13.9 — Persistent login screen

## Što je novo
- Home screen login ikonica sada otvara poseban, lijepi SOV Cloud login screen.
- Login više nije shortcut na Settings.
- Session i permission cache ostaju spremljeni u SharedPreferences i preživljavaju update aplikacije.
- App na startupu automatski pokušava obnoviti Supabase session/permissione preko refresh tokena.
- Settings više ne prikazuje ručni SOV login/sync blok.
- Login screen prikazuje status prijave i rolu, bez potrebe za ručnim syncanjem.

## Verzija
- versionCode: 900055
- versionName: 1.4.13.9-persistent-login-screen
- APK name: SOV-ADMIN-1.4.13.9.apk

## SQL
Nije potreban novi SQL.

## Napomena
APK nije buildan u sandboxu jer Gradle wrapper zahtijeva mrežni download Gradlea. Source je spreman za Android Studio build.
