# SOV web v4.85 — equipment_locations RLS import fix

Fix za import gresku:
`new row violates row-level security policy for table "equipment_locations"`

Uzrok: full open preview SQL je otvorio glavne tablice, ali nije otvorio staru tablicu `equipment_locations` koju import jos koristi za lokacije.

Pokrenuti jednom:
`SUPABASE_ORUZARSTVO_v4_85_OPEN_PREVIEW_EQUIPMENT_LOCATIONS_RLS_FIX.sql`

Nema brisanja podataka.
