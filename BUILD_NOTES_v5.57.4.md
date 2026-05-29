# SOV Web v5.57.4 — Arhivar timeout fix

- Arhivar worklist je splitan na brzi list view i puni detail RPC.
- Web lista više ne vuče raw tekst, nacrte i zapisnike za svih 1500 objekata odjednom.
- Klik na objekt dohvaća puni tekst samo za taj objekt.
- Popravlja Supabase error: `canceling statement due to statement timeout`.
- RLS ostaje uključen.
- Pokrenuti SQL: `SUPABASE_SOV_ARHIVAR_WORKFLOW_v5_57_4_TIMEOUT_FIX.sql`.
