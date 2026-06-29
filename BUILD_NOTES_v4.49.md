# SOV web v4.49 — hard fix za duplicate category import

Popravak nakon greške:
`duplicate key value violates unique constraint "equipment_categories_name_key"`

Što je promijenjeno:
- kategorije se više NE upisuju kroz obični bulk upsert koji može puknuti na postojećim imenima
- import sada radi safe flow:
  1. dedupe kategorije po imenu
  2. provjeri postoji li kategorija s tim imenom
  3. ako postoji, ažurira je po ID-u
  4. ako ne postoji, ubaci novu
  5. ako u međuvremenu nastane duplicate, ponovno pronađe postojeću i ažurira
- za kategorije se više ne oslanjamo na legacy_id kao identitet

SQL migraciju nije potrebno ponovno vrtjeti ako je v4.48 SQL već pokrenut.
