# SOV Admin APK v1.4.36a — Cleartext Local Guard

Base: v1.4.35a-log-redaction-hardening.

## Promjene
- Dodan `SovNetworkSecurity` kao centralni app-side cleartext gate.
- HTTP je dopušten samo za lokalne/private adrese: localhost, 127.0.0.1, ::1, 10.x.x.x, 172.16-31.x.x, 192.168.x.x, 169.254.x.x, 100.64-127.x.x i `.local`.
- HTTPS ostaje dopušten svugdje.
- Centralni Supabase/REST, WMS/tile, WMS capabilities, offline map download, update download, shared layers, Drive drawings, Field Hub, weather i trip asset direct URLConnection pozivi sada prolaze kroz isti guard.
- Version bump: `versionCode 900137`, `versionName 1.4.36a-cleartext-local-guard`.

## Važna napomena
Android Network Security Config ne podržava CIDR/private range allowlist poput `192.168.0.0/16` ili `172.16.0.0/12`. Zbog SOV Field Huba i hotspot/local WMS-a ne smijemo ugasiti manifest-level cleartext na slijepo, jer bi arbitrary lokalni IP-jevi pukli. Zato je ovo siguran kompatibilni korak: app sam blokira vanjski HTTP, ali zadržava teren/laptop/hub kompatibilnost.

## Test
- Login/logout i Supabase sync.
- Field Hub na `http://192.168.x.x:port`.
- Custom lokalni WMS na `http://192.168.x.x/...`.
- Vanjski custom WMS preko `http://` treba biti odbijen; preko `https://` treba raditi.
- Offline map download preko HTTPS.
