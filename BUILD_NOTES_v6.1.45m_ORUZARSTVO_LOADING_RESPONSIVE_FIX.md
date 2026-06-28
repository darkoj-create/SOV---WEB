# SOV web v6.1.45m — Oružarstvo loading + responsive fix

- Popravljen beskonačni loading kataloga u `oruzarstvo.html`.
- `assets/oruzarstvo-boot-v615.js` sada prvo koristi cache/local DATA, zatim live Supabase, a ako live zapne vraća se na statički XLS canonical JSON.
- Dodan responsive hotfix CSS: `assets/oruzarstvo-responsive-hotfix-v6145m.css`.
- Očuvan merge Osobna oprema + SRT iz 45l.
- Nema SQL promjena.
