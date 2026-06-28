# SOV Web v6.1.43b — ručni Gmail sync zadnja 4 tjedna

- Na `zapisnici-najave.html` dodan gumb **Pokreni Gmail sync · zadnja 4 tjedna**.
- Ručni sync traži DOCX zapisnike iz posljednjih 28 dana, uvozi ih kroz postojeći Supabase ingest RPC i osvježava listu.
- Zadržan je automatski trigger srijedom oko 23:50.
- Priložen je kompletan Apps Script `SOV_GMAIL_ZAPISNICI_APPS_SCRIPT_v6_1_43b.gs` s `doGet` web-hookom.
- Prvi deployment Apps Scripta mora se napraviti jednom; dobiveni `/exec` URL upisuje se u `SOV_GMAIL_MINUTES_SYNC_ENDPOINT` u `assets/supabase-config.js`.
