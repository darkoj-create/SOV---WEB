# SOV Admin APK 1.3.1 Stable Rebuild

Ovo je konzervativni rebuild nakon što je prethodni 1.3.1 audit-device patch bio problematičan.

Baza: zadnji poznati dobri `sov-admin-v1.3.0-identity-sync-layer-source.zip`.

Promjene:
- `versionCode = 900011`
- `versionName = 1.3.1-stable-rebuild`
- vraćena stabilna 1.3.0 APK logika za login/role/sync foundation
- maknuti APK-side best-effort audit/device RPC pozivi iz permission sync flowa jer mogu rušiti/kočiti app ovisno o runtimeu i mreži

Web/Supabase dio v5.35 može ostati, ali APK device/audit logging ćemo ponovno dodati kasnije nakon što potvrdimo stabilan runtime i RPC response shape.

Build:
- otvoriti u Android Studio
- Gradle sync
- Generate Signed APK
- potpisati istim admin keystoreom
