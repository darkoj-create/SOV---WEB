# SOV Web v5.13 — Controlled SQL Promotion

Sigurna faza migracije Speleo Baze prema SQL-u.

Dodano:
- `speleo-sql-promote.html`
- `SUPABASE_SPELEO_BAZA_CONTROLLED_PROMOTION_v5_13.sql`
- staging → SQL live kandidat (`speleo_objects_live_sql`)
- batch promocija
- pojedinačna promocija objekta
- pregled razlika staging vs SQL live
- promotion audit
- rollback zadnjeg batcha

Bitno:
- Live JSON Baza se NE dira.
- Web Baza se NE prebacuje automatski na SQL.
- Ovo je kontrolirani SQL live kandidat za kasniji final switch.
