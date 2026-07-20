# SOV web build v6.1.45aw

## v6.1.45aw — Trips force-refresh hotfix

- `Osvježi` i mobilno povlačenje sada pokreću potpuno novi Supabase zahtjev čak i ako prethodni poziv još traje.
- Stariji odgovor više ne može prepisati cache nakon što je stigao noviji rezultat.
- Dodan je izolirani regresijski test koji namjerno drži stari zahtjev otvorenim i potvrđuje da force refresh vraća novi rezultat.
- Nema SQL promjena.


## v6.1.45av — pre-release audit i stabilizacija

Ovo je kontrolirani release nakon punog audita weba, Vercela i Supabase ugovora.

### Automatski release gate

- statički audit svih **279 HTML stranica**
- provjera lokalnih linkova, asseta, ruta, auth registra i verzijskog ugovora
- provjera sintakse svih JavaScript datoteka
- zaštita od nezatvorenih vanjskih `<script src>` tagova
- Playwright browser smoke kroz svih 279 stranica, izoliran od live Supabasea
- funkcionalni test gumba **Osvježi** i mobilnog pull-to-refresh toka za Izlete

### Glavni popravci

- Izleti sada stvarno zaobilaze stari/in-flight zahtjev i povlače svježe retke iz baze.
- Dodan je jedan ispravan refresh gumb i sigurno povlačenje za osvježavanje na mobitelu.
- Popravljeni su ugniježđeni asseti i `update.json` dohvat na starim člancima.
- Uklonjeni su konfliktni redirect/rewrites i meta-refresh aliasi.
- Popravljen je dashboard script tag koji je gutao inline popravak topbara.
- Speleoškola više ne traži četiri nepostojeća lokalna WordPress asseta.
- TopoDroid import pravilno učitava Supabase klijent.

### Objedinjeni SQL

Primjenjuje se samo:

`sql/sov_release_v6145av.sql`

Migracija u jednoj transakciji:

- odvaja brzi map feed od sporog Arhivar worklista
- dodaje nedostajući review RPC za predane jame
- usklađuje reviewer role na `webmaster`, `admin`, `arhivar`
- prebacuje Spelo Runner leaderboard na `security_invoker`
- uklanja nepotrebne anonimne write grantove za runner rezultate

### Važno

- Povijesni `http://` linkovi u arhiviranim člancima nisu naslijepo promijenjeni; provjeravaju se odredište po odredište.
- Android cleanup RPC i dalje traži popravak u stvarnom APK sourceu ili sigurnoj Edge Function implementaciji; ne smije se rješavati otvaranjem direktnog brisanja iz `storage.objects`.
- Detaljni audit: `docs/PRE_RELEASE_AUDIT_2026-07-20.md`.
