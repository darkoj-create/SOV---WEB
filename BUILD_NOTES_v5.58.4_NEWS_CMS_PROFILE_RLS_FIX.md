# SOV Web v5.58.4 — News CMS profile RLS recursion fix

Popravak za Supabase grešku:

`infinite recursion detected in policy for relation "profiles"`

Što je promijenjeno:
- dodan SQL patch `SUPABASE_SOV_NEWS_CMS_v5_58_4_PROFILE_RLS_RECURSION_FIX.sql`
- role/helper funkcije sada su SECURITY DEFINER + row_security=off
- poznate rekurzivne `profiles` politike iz role-control buildova se zamjenjuju sigurnim verzijama
- `sov_news` i `sov-news` Storage politike koriste novi non-recursive helper
- javni news RPC-jevi imaju `row_security=off` da public feed/detail ne zapinju na RLS-u

Deploy weba nije strogo potreban ako je v5.58.3 već gore; najbitnije je pokrenuti SQL patch.
