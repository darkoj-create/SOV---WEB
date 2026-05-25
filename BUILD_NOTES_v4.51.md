# v4.51 Null-data import/catalog hard fix

- Import page no longer crashes if data object is null or malformed.
- Oružarstvo main page normalizes data/oruzarstvo-data.json before rendering.
- Supabase import function normalizes data before reading categories/items/ropes.
- Keeps simplified model: individual codes only for ropes, quantity articles for other equipment.
