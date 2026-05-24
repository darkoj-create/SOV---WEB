# v4.29 Oružarstvo date import fix

- Import sada normalizira djelomične datume iz inventure prije slanja u Supabase date kolone.
- `09/2022` i `06/2022` se spremaju kao prvi dan mjeseca.
- Nepotpuni datumi bez godine ostaju `null`, da import ne puca.
