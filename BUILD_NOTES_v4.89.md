# v4.89 — Arhivar Role + Speleo Baza Edit

Dodano:
- nova rola `arhivar`
- admin panel može dodijeliti rolu Arhivar
- Speleo Baza detalj objekta prikazuje `Uredi objekt` za admin/arhivar
- izmjene se spremaju u Supabase tablicu `speleo_object_overrides`
- audit log ide u `speleo_object_edits`
- obični user ne vidi edit gumb
- oružar ne dobiva Speleo Baza edit po defaultu

SQL:
- pokrenuti jednom `SUPABASE_SPELEO_BAZA_ARHIVAR_v4_89.sql`
