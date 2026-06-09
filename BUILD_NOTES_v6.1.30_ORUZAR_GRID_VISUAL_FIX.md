# SOV Web v6.1.30 — Oružar grid visual fix

Baseline: sov-web-build-v6.1.29-oruzar-inventura.zip

## Changed
- assets/oruzar-master-ops-v55818.css: item grid ograničen na uredan responsive layout bez rezanja kartica
- assets/oruzar-master-ops-v55818.css: kartice su kompaktnije, tekst se reže kontrolirano, badgevi i stock boxevi više ne cure iz kartica
- assets/oruzar-master-ops-v55818.css: item action gumbi su manji i manje vizualno agresivni
- assets/oruzar-master-clean.js: uklonjen user-facing izvor/XLS tekst iz kartica opreme
- oruzar-master*.html: cache-bust za oružar CSS/JS dignut na v6.1.30

## Not changed
- Nema SQL promjena
- Nema APK promjena
- Nema promjena u Supabase pozivima
- Nema promjena u CleanArmory business funkcijama za spremanje/brisanje/izvoz
- Nema promjena u onclick handlerima

## Why
Na manjim desktop/laptop širinama item grid je forsirao previše kolona pa su kartice izgledale odrezano i prenatrpano. Također je prikazivan interni tekst o XLS/source retku koji korisniku ne treba.
