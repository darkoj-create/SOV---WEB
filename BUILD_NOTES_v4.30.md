# v4.30 — Oružarstvo date format polish

- User-facing Oružarstvo dates now display as dd/mm/yyyy.
- Static Oružarstvo JSON date values converted from ISO yyyy-mm-dd to dd/mm/yyyy.
- Supabase importer still converts dd/mm/yyyy back to ISO date for database columns.
- Added parser support for dd/mm/yyyy and dd-mm-yyyy in assets/oruzarstvo-supabase.js.
