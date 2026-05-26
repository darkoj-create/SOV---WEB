# v4.98 — Nacrti performance fix

- Baza/pregled baze više ne radi fuzzy matching svakog nacrta protiv svakog objekta pri svakom renderu.
- Syncani nacrti se indeksiraju po `object_id` i dohvaćaju instantno.
- Thumbnailovi se učitavaju samo u detalju odabranog objekta, ne za cijelu listu.
- `Otvori nacrt` i dalje vodi na inline nacrte u objektu, bez Documents redirecta.
- Nema SQL promjene.
