# v4.27 bootstrap admin login fix

- Dodan bootstrap admin fallback za darko.jeras@gmail.com nakon uspješnog Supabase auth logina.
- Ako profil iz RLS/ID sync razloga dođe kao pending/null, web ga tretira kao admin/approved i pokušava uskladiti profiles red.
- Ne dira CSS, dashboard ni oružarstvo podatke.
