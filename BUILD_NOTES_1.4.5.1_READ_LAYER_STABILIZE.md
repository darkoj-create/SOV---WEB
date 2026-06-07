# SOV Admin APK 1.4.5.1 — read layer stabilize

- Ne mijenja UI flow.
- Forsira novi lokalni cache key za Oružarstvo da app ne prikazuje stari kaotični cache iz 1.4.5.
- APK i dalje čita `sov_equipment_app_catalog_grouped` za katalog i `sov_equipment_app_catalog` za inventuru.
- Nakon SQL `v5_47_3_READ_LAYER_STABILIZE`, ti viewovi opet čitaju stabilne legacy tablice dok `equipment_assets` ostaje mirror.
