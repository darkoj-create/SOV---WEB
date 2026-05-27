# v5.11 — Live JSON vs SQL staging compare

SAFE MODE build based on v5.10.

Dodano:
- `speleo-sql-compare.html`
- usporedba live JSON objekta i SQL staging objekta
- highlight razlika po poljima
- gumb za označavanje staging objekta kao `ready_for_live`
- gumb za spremanje promoviranog kandidata u `speleo_objects_live_candidates`
- audit log za compare/promote akcije

Važno:
- live Baza i dalje čita JSON
- klik “Promoviraj kandidata” NE mijenja live prikaz automatski
- služi za sigurnu pripremu prije stvarnog prebacivanja izvora na SQL
