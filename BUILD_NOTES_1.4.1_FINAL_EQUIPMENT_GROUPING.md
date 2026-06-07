# SOV Admin APK 1.4.1 — Final Equipment Grouping Rules

- Katalog/browse/search sada čita `sov_equipment_app_catalog_grouped`.
- Inventura ostaje na raw `sov_equipment_app_catalog` stavkama, da oružar broji stvarne retke/lokacije.
- Search više ne koristi preširoke globalne alias tagove po kategoriji.
- Centralne grupe: Descenderi, Croll/prsni blokeri, Ručni blokeri, Pojasevi/sjedalice, Karabineri, Spitovi/sidrišta, Bušilice, Baterije, Punjači, Svrdla, Mjerenje/Disto/TopoDroid, Crtaći pribor, Kacige, lampe itd.
- User i dalje ne vidi lager brojke; Admin/Oružar vidi količine.
- Potreban SQL: `SUPABASE_ORUZARSTVO_APP_CATALOG_v5_44_FINAL_GROUPING_RULES.sql`.
