# SOV web v5.58.5 — News CMS hard profiles RLS reset

Fix za Supabase error: `infinite recursion detected in policy for relation "profiles"`.

- SQL agresivno brise sve postojece RLS policyje na `public.profiles` i vraca samo nerekurzivne self/JWT policyje.
- `sov_news` SELECT vise ne zove role/profile helper, pa editor moze ucitati listu bez profiles recursion.
- Edit/upload i dalje idu kroz `sov_can_edit_news()`.
- Public vijesti koriste RPC-eve `sov_news_public_list` i `sov_news_public_detail`.

Pokreni: `SUPABASE_SOV_NEWS_CMS_v5_58_5_HARD_PROFILE_RLS_RESET.sql`
