# v4.48 — Oružarstvo recovery build

Popravljeno nakon v4.47 regresije:
- katalog ponovno ima sadržaj iz `data/oruzarstvo-data.json`
- guest/član vidi kategorije i artikle bez skladišnih količina
- Oružar master je ponovno eksplicitno vidljiv adminu/oružaru
- import kategorija više ne puca na `equipment_categories_name_key` jer se kategorije upsertaju po `name`, ne po `legacy_id`
- zadržan pojednostavljeni model: samo užad ima individualni kod, ostalo je količinski artikl
