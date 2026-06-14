# v4.68

- Hard fix: user “Zatraži” now creates a pending loan request immediately, instead of only adding to a local cart/drawer.
- Oružar Master → Posudbe reloads latest Supabase requests before rendering.
- Added refresh button for loan requests.
- If Supabase/auth fails, request falls back to local storage with a clear warning.
