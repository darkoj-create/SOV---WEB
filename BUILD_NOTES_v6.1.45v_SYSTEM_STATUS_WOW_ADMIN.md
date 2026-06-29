# SOV Web v6.1.45v — System Status WOW Admin

Promjena: stara statička `system-status` stranica je zamijenjena kompletnim admin health dashboardom iz nule.

## Što radi live
- provjerava Supabase konfiguraciju i auth session
- provjerava Admin/Webmaster pristup
- učitava lokalni ecosystem manifest
- učitava live `sov_ecosystem_manifest_current`
- učitava live `sov_equipment_catalog_manifest`
- broji live katalog opreme (`sov_equipment_app_catalog_grouped`)
- broji `equipment_items`, `equipment_requests`, pending zahtjeve, `equipment_loans`, `equipment_loan_items`
- broji profile i security hardening log gdje browser ima dozvolu
- prikazuje OK / Warning / Error, latency i raw JSON snapshot

## Rute
- `/system-status`
- `/system-status.html`
- `/sov-system-status`
- `/sov-system-status.html`
- `/status`
- `/status.html`

## Bitno
- Stranica više nije statički placeholder.
- Dizajnirana je kao admin/dev dashboard za svakodnevnu provjeru.
- Nema SQL promjena u ovom ZIP-u.
- Ako RLS audit stavke prikazuju `—`, browser nema direktan DB katalog pristup bez dodatnog security-definer RPC-a; ostale provjere rade live kroz postojeći Supabase client.
