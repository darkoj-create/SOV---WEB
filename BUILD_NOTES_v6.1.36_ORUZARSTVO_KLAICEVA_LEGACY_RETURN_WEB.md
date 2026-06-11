# SOV Web v6.1.36 — Oružarstvo Klaićeva legacy_return web patch

Bazirano na: `sov-web-build-v6.1.35-oruzar-inventura-search.zip`.

## Što je promijenjeno

- Dodan web workflow **Povrat stare opreme / bez otvorene posudbe** u Oružar Master.
- Na karticama inventara dodan gumb **Povrat stare** za brzo zaprimanje postojećeg artikla.
- U Posudbe dodan poseban panel za zaprimanje stare opreme.
- Frontend zove RPC:
  - `sov_armory_record_legacy_return` za postojeći artikl
  - `sov_armory_add_item_and_legacy_return` za novu/nejasnu opremu
- Ne uvodi široki direct-write RLS iz browsera.
- Ubaceni SQL/runbook fajlovi za Build 1 + Build 2 u ZIP.

## Redoslijed deploya

1. Pokrenuti `SUPABASE_ORUZARSTVO_V2_1_BUILD1_KLAICEVA_OPENING_BALANCE.sql`.
2. Pokrenuti `SUPABASE_ORUZARSTVO_V2_1_BUILD2_LEGACY_RETURN_RPC_RLS.sql`.
3. Deployati ovaj web build.
4. Testirati: Oružar Master → Posudbe → Povrat stare opreme.

## Namjerno nije rađeno

- Nije dodan legacy claim sustav.
- Nije rekonstruirana stara povijest posudbi.
- Nije dodan QR/barcode workflow.
- Nije mijenjan APK.
