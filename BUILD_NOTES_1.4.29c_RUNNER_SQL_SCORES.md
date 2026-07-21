# SOV Admin APK v1.4.29c — Speleo Runner SQL scores

## Cilj
Speleo Runner rezultati više nisu vezani uz Google Sheet kao izvor istine.
Postojeći CSV rezultati se importaju u Supabase jednom, a novi rezultati se samo nadodaju u SQL bazu.

## APK promjene
- `SpeleoRunnerLeaderboardClient.kt` sada čita leaderboard iz Supabase RPC-a `sov_list_runner_leaderboard`.
- Novi score se šalje samo u Supabase RPC `sov_submit_runner_score`.
- Maknut je Google Sheet online fallback i Apps Script write fallback.
- Ako nema interneta, app prikazuje zadnji lokalni cache i čuva jedan pending rezultat za kasniji upload.

## SQL promjene
- `SUPABASE_SPELEO_RUNNER_SQL_SCORES_v1_4_29c.sql` osigurava tablicu `public.sov_runner_scores`.
- Seed import koristi `on conflict (client_key) do nothing`, pa se isti CSV ne može duplo importati.
- Uključuje 125 rezultata iz CSV-a `Spele run score - Sheet1.csv`.

## Ne dirati
- Ne raditi replace/delete postojećih rezultata.
- Novi score uvijek ide kao novi insert/upsert po `client_key`.
- Sheet više nije izvor istine.
