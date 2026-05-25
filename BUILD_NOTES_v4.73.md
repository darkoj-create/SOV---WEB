# SOV web v4.73 — integer quantity cleanup

Fix za čudne decimalne brojače u Oružar Master → Inventar.

- UI brojači sada uvijek prikazuju cijele komade.
- Import više ne prihvaća decimalne/datumske vrijednosti kao količinu.
- SQL cleanup radi backup i zatim očisti decimalne količine u cijele brojeve.
- Ne dira nazive artikala, kategorije, lokacije ni user katalog.

Pokrenuti jednom:
`SUPABASE_ORUZARSTVO_v4_73_INTEGER_QUANTITY_CLEANUP.sql`
