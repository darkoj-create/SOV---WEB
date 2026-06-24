# SOV Oružarstvo v2.1 — simple import + povrati stare opreme

## 1. Ključna odluka

Ne radimo posebni sustav za stare loše posudbe, ne šaljemo poruke, ne vodimo zasebne “legacy claim” tablice i ne pokušavamo rekonstruirati svaku staru posudbu.

Radimo jednostavno:

> **XLS inventura Klaićeva = odmah ide u glavnu oružarsku bazu kao službeno trenutno stanje.**

To je početna istina za Klaićevu.

- lokacija: `Oružarstvo Klaićeva`
- movement type: `opening_balance`
- source: `Inventura Klaićeva 2026`
- status: `available` / raspoloživo, osim stavki označenih kao `za provjeru`

Stara SOV baza i stare posudbe ne smiju prepisivati ovo stanje.

---

## 2. Što s opremom koja se bude vraćala iz starih loše evidentiranih posudbi?

Svaki povrat stare opreme tretira se kao normalan ulaz u glavnu bazu.

Ne mora postojati otvorena posudba u sustavu da bi se nešto moglo vratiti.

### Novi movement type

Dodati movement type:

```text
legacy_return
```

Značenje:

> Oprema se fizički vratila u Klaićevu iz stare / loše / izvan-sustavske posudbe.

Primjer:

- početno stanje Klaićeva: 17 karabinera
- netko vrati još 5 karabinera iz stare posudbe
- oružar klikne **Zaprimanje stare opreme / Povrat bez otvorene posudbe**
- baza upiše movement `legacy_return +5`
- novo stanje Klaićeva: 22 karabinera

---

## 3. Minimalni workflow u webu

U Oružarstvu dodati jednostavnu akciju:

## Zaprimanje / povrat bez otvorene posudbe

Polja:

- artikl
- količina
- ako je asset: odabir postojećeg asseta ili kreiranje novog asseta
- od koga / izvor, free text, opcionalno
- napomena
- stanje opreme: OK / za provjeru / oštećeno

Akcija radi:

- za bulk: poveća stanje u Klaićevoj
- za asset: kreira ili vrati pojedinačni komad u Klaićevu
- uvijek piše movement `legacy_return`
- ne traži postojeću posudbu
- ne blokira rad ako osoba nije u bazi

---

## 4. Minimalni workflow u APK-u

APK može imati isto, ali pojednostavljeno:

- `Povrat stare opreme`
- search artikla typo-friendly
- količina
- napomena
- spremi

Ako nema interneta:

- spremiti u offline queue
- pri syncu poslati isti RPC
- koristiti `client_event_id` da ne duplira povrat

---

## 5. Ne raditi ove stvari

Ne raditi:

- zaseban “legacy posudbe” modul
- kontaktiranje ljudi kroz sustav
- claim/confirm/expected-return statuse
- automatsko oduzimanje starih posudbi od Klaićeve
- automatsku rekonstrukciju povijesti
- QR/barcode workflow

---

## 6. Kako tretirati pojedinačnu opremu koja se vrati

Ako se vraća vrijedna oprema, npr. GPS, bušilica, DistoX, šator, uže:

### Ako već postoji asset zapis

Movement:

```text
legacy_return: van sustava / kod člana -> Oružarstvo Klaićeva
```

Ažurirati:

- `current_location_id = Klaićeva`
- `asset_status = available` ili `quarantine/damaged` ako nije OK

### Ako ne postoji asset zapis

Kreirati novi asset zapis s običnim inventurnim kodom, bez QR-a.

Primjeri:

```text
SOV-GPS-001
SOV-DISTO-001
SOV-BUS-001
SOV-UZE-060-001
```

Zatim upisati `legacy_return` movement.

---

## 7. Kako tretirati bulk opremu koja se vrati

Za bulk artikle samo povećati stock u Klaićevoj kroz movement.

Primjeri:

```text
legacy_return +5 Karabiner ALU -> Klaićeva
legacy_return +20 Pločica ALU L -> Klaićeva
legacy_return +10 Mailon -> Klaićeva
```

Nema potrebe znati staru posudbu.

---

## 8. Ako se vrati oprema koja nije u katalogu

Oružar mora moći brzo dodati novi artikl:

- kategorija
- naziv
- tracking mode: `bulk` ili `asset`
- jedinica
- napomena

Zatim odmah napraviti `legacy_return`.

Ako je naziv nejasan, staviti kategoriju `Za provjeru` ili status `quarantine`, ali svejedno unijeti u bazu.

---

## 9. SQL/RPC pravilo

Frontend i APK ne rade direktni update količina.

Mora postojati RPC, npr.:

```sql
select public.sov_armory_record_legacy_return(
  p_item_id := ..., 
  p_quantity := ..., 
  p_to_location_id := ..., 
  p_source_name := ..., 
  p_note := ..., 
  p_client_event_id := ...
);
```

Za asset:

```sql
select public.sov_armory_record_asset_legacy_return(
  p_asset_id := ..., 
  p_item_id := ..., 
  p_asset_code := ..., 
  p_to_location_id := ..., 
  p_condition_status := ..., 
  p_source_name := ..., 
  p_note := ..., 
  p_client_event_id := ...
);
```

---

## 10. Konačna logika

Baza se vodi ovako:

1. Import XLS-a u glavnu bazu kao `opening_balance` Klaićeva.
2. Sve što se kasnije vrati iz starih posudbi ide kao `legacy_return`.
3. Sve nove posudbe nakon importa idu kao normalni `loan` / `return` workflow.
4. Stare posudbe se ne rekonstruiraju posebno.
5. Klaićeva stock raste kad se nešto fizički vrati.
6. Povijest ostaje čista jer se vidi što je bilo opening balance, a što je naknadni povrat stare opreme.
