# SOV Web v5.48.1 — Armory speed hotfix

- Oružarstvo više ne blokira UI manifest/full syncom prije prikaza ako postoji lokalni katalog cache.
- Dodan JS cache-first wrapper za `SOVArmoryDB.loadAllData()`.
- Oružar Master se otvara iz static/cache kataloga odmah, a Supabase refresh ide u pozadini.
- Dodan SQL v5.48.1 koji mijenja spori manifest view u malu cache tablicu `sov_equipment_catalog_manifest`.
- Stari katalog/read layer se ne briše.
