# SOV Web v5.57.5 — Arhivar pločica + puni SQL detail

Popravci:
- `plocica` / `pločica` iz `field_tasks` i `workflow_raw` sada se tretira kao eksplicitna falinka, čak i kad objekt ima neki katastarski broj.
- broj pločice se izvlači iz više mogućih SQL/raw ključeva: `plate_number`, `plocica`, `pločica`, `broj_plocice`, `katastarski_broj`, `kbr`, `oznaka` itd.
- klik na objekt i dalje koristi brzi one-object RPC, ali sada vraća puno više: opis, pristup, istraživanje/povijest, autori/ekipa, hidrologija, geologija/morfologija, opasnosti/zaštita, napomene i sva raw SQL polja.
- worklist ostaje light/fast da se ne vrati statement timeout.

SQL za Supabase:
- pokrenuti `SUPABASE_SOV_ARHIVAR_WORKFLOW_v5_57_5_PLOCICA_FULL_SQL_DETAIL_FIX.sql`
- ili cijeli aktualni `SUPABASE_SOV_ARHIVAR_WORKFLOW_v5_57_ALL_IN_ONE.sql`
