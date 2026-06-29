# SOV web v6.1.5 — Oružarstvo XLS canonical fix

## Što je popravljeno

Ovaj build usklađuje Oružarstvo / Oružar Master s uploadanom XLS evidencijom `SOV_oruzarstvo_jednostavna_popunjena_evidencija.xlsx`.

Napomena: početni parcijalni pregled XLS-a je izgledao kao ~316 redova, ali puni scan tablice je našao **551 stvarni red opreme**. Ovaj build koristi puni scan, ne parcijalni pregled.

## XLS istina

- Izvorni sheet: `SOV oruzarstvo evidencija`
- XLS redovi opreme: **551**
- Kategorije: **12**
- Numeričke količine: **424**
- Prazna / nenumerička količina: **127** — u aplikaciji je označeno kao `prebrojiti`, ne kao dostupno.
- Zadnji datum evidencije u XLS-u: **2026-01-28**

### Kategorije iz XLS-a

- Oprema za logor: 202
- Medicinska oprema: 65
- Oprema za postavljanje: 56
- Oprema za proširivanje: 45
- Elektro i foto oprema: 39
- Ostali alat: 39
- Užeta: 26
- Alpinistička oprema: 23
- Čisto podzemlje: 19
- Osobna oprema: 15
- Oprema za crtanje: 14
- Ronilačka oprema: 8

## Logika baze

- Svaki XLS red je jedan `equipment_items` red (`XLS-0001` do `XLS-0551`).
- Nema spajanja po nazivu, nema agresivnog “normaliziranja” kategorija, nema deduplikacije užadi u posebnu rope tablicu.
- Užeta iz XLS-a ostaju normalni inventory redovi u kategoriji `Užeta`; SKU/kodovi ostaju u napomeni i source poljima.
- Redovi bez jasne količine imaju `quantity=0`, `available=0`, `status='za provjeru'`, `availability='nedostupno'`, `available_label='provjeriti'`.
- Public zahtjevi za opremu sada se omogućuju samo kad je `available > 0`, ne po tekstualnom statusu.
- Manual edit lokacije više ne akumulira staru količinu svaki put; `setItemLocationQuantity()` postavlja stvarnu trenutnu količinu.

## UI / UX

- Oružar Master koristi točne XLS kategorije i podkategorije.
- Kartice prikazuju jedinicu, red iz XLS-a, datum evidencije i napomenu.
- Export inventara koristi XLS-like stupce: Kategorija, Podkategorija, Naziv, Količina, Jedinica, Datum evidencije, Lokacija, Status, Napomena / detalji.
- Inventura export uključuje originalnu količinu, prebrojano, razliku, lokaciju i napomenu.
- Stare legacy grupe tipa `Užad i užetna oprema`, `Bušilice i baterije`, `Crtanje i mjerenje` uklonjene su iz aktivne kategorizacije.

## Datoteke promijenjene

- `data/oruzarstvo-data.json`
- `data/oruzarstvo-data-v1-model.json`
- `data/oruzarstvo-xls-canonical-v6.1.5.json`
- `assets/oruzarstvo-supabase.js`
- `assets/oruzar-master-clean.js`
- `assets/oruzarstvo-boot-v606.js`
- `assets/oruzarstvo-boot-v607.js`
- `assets/oruzarstvo-boot-v615.js` novo
- `oruzarstvo.html`
- `oruzar*.html` cache refs na `v=6.1.5`
- `SUPABASE_ORUZARSTVO_XLS_CANONICAL_v6_1_5.sql`

## Deploy

1. U Supabase SQL editoru pokreni: `SUPABASE_ORUZARSTVO_XLS_CANONICAL_v6_1_5.sql`.
2. Deployaj web ZIP.
3. Hard refresh browsera. Ako testiraš lokalno sa starim cacheom, očisti Local Storage za stranicu.
4. Provjera u Supabaseu:

```sql
select count(*) from public.sov_equipment_app_catalog;
select category_name, count(*) from public.sov_equipment_app_catalog group by category_name order by count(*) desc;
```

Očekivano je 551 XLS red ako u bazi nema dodatnih ručno dodanih aktivnih artikala.

## Verifikacija napravljena u build folderu

- `node --check assets/oruzarstvo-supabase.js`
- `node --check assets/oruzar-master-clean.js`
- `node --check assets/oruzarstvo-boot-v606.js`
- `node --check assets/oruzarstvo-boot-v607.js`
- `node --check assets/oruzarstvo-boot-v615.js`
- `python3 -m json.tool data/oruzarstvo-data.json`
- JSON count: 551 items, 0 ropes, 12 categories, 127 rows to recount.
