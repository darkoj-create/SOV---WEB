# SOV Oružarstvo v2.1 — Build 1 + Build 2 runbook

## Cilj

Srediti Klaićevu bez razbijanja ostatka sustava.

- **Build 1**: XLS inventura Klaićeva postaje službeno početno stanje (`opening_balance`).
- **Build 2**: dodaje siguran RPC za povrat stare opreme bez otvorene posudbe (`legacy_return`).

Ovo još nije web/APK build jer u ovom paketu nema web/APK source ZIP-a. Ovo je baza/Supabase dio koji treba biti temelj za idući web+APK build.

---

## Redoslijed

### 1. Prvo pokreni Build 1 SQL

File:

```text
SUPABASE_ORUZARSTVO_V2_1_BUILD1_KLAICEVA_OPENING_BALANCE.sql
```

Pokrenuti u Supabase SQL editoru.

Što radi:

- kreira/pojačava osnovne oružarske tablice ako fale
- backupira stare XLS/import seed redove
- stare XLS/import/KLAICEVA redove označava kao `superseded`
- unosi 174 reda iz XLS-a kao `opening_balance`
- stavke `Provjeriti` stavlja kao fizički postojeće, ali ne kao normalno dostupne članovima
- unosi movement log `opening_balance`

### 2. Provjeri Build 1

Pokreni:

```sql
select *
from public.sov_armory_import_batches
where import_key='KLAICEVA-2026-OPENING-BALANCE';
```

Očekivano:

- `imported_rows = 174`
- `ok_rows = 106`
- `check_rows = 68`

Zatim:

```sql
select status, availability, member_visible, count(*)
from public.equipment_items
where source_sheet ilike '%Inventura Klaićeva 2026%'
group by 1,2,3
order by 1,2,3;
```

Očekivano grubo:

- OK redovi: `aktivno / dostupno / true`
- nesigurni redovi: `za_provjeru / za provjeru / false`

Zatim:

```sql
select count(*) as movements
from public.sov_armory_stock_movements
where movement_type='opening_balance'
  and source_name='Inventura Klaićeva 2026';
```

Očekivano:

- `174`

### 3. Onda pokreni Build 2 SQL

File:

```text
SUPABASE_ORUZARSTVO_V2_1_BUILD2_LEGACY_RETURN_RPC_RLS.sql
```

Što dodaje:

- RPC `public.sov_armory_record_legacy_return(...)`
- helper RPC `public.sov_armory_add_item_and_legacy_return(...)`
- idempotenciju preko `client_event_id`
- read RLS policies za tablice koje web/APK trebaju čitati
- ne otvara široki direct-write na `equipment_assets`

---

## Važna napomena o staroj grešci

Stara greška:

```text
new row violates row-level security policy for table equipment_assets
```

Build 1 to ne može popraviti jer uopće ne dira `equipment_assets`.

Build 2 namjerno ne otvara široku write policy na `equipment_assets`, jer bi to moglo dati svim login korisnicima previše pisanja.

Ispravan smjer za web/APK build:

- frontend/APK ne smiju direktno mijenjati količine
- moraju zvati RPC
- za povrat stare opreme: `sov_armory_record_legacy_return`
- za dodavanje nepoznate stare opreme: `sov_armory_add_item_and_legacy_return`

Ako postojeći web/APK i dalje direktno pišu u `equipment_assets`, treba promijeniti app kod ili svjesno dodati privremenu compatibility RLS policy.

---

## Primjer testiranja `legacy_return`

Prvo nađi artikl:

```sql
select legacy_id, name, quantity, available, status
from public.equipment_items
where name ilike '%Karabiner%'
order by name
limit 10;
```

Zatim test:

```sql
select public.sov_armory_record_legacy_return(
  p_equipment_legacy_id := 'KLAICEVA-2026-XXXX',
  p_quantity := 1,
  p_source_name := 'TEST',
  p_note := 'TEST legacy return',
  p_client_event_id := 'TEST-LEGACY-RETURN-001'
);
```

Provjera:

```sql
select *
from public.sov_armory_stock_movements
where client_event_id='TEST-LEGACY-RETURN-001';
```

Ako isti test pozoveš drugi put s istim `client_event_id`, ne smije duplirati stanje.

---

## Web/APK Build 3 — što treba napraviti u kodu

Kad dodaš zadnji web/APK ZIP, treba napraviti jedan konzistentan build:

### Web

Dodati akciju:

```text
Oružarstvo > Povrat stare opreme / bez otvorene posudbe
```

Polja:

- search artikla
- količina
- od koga / izvor, opcionalno
- stanje: OK / za provjeru / oštećeno
- napomena

Submit:

```js
supabase.rpc('sov_armory_record_legacy_return', {
  p_item_id: selectedItem.id,
  p_quantity: quantity,
  p_condition_status: conditionStatus,
  p_source_name: sourceName,
  p_note: note,
  p_client_event_id: crypto.randomUUID()
})
```

### APK

Isto, ali jednostavnije:

```text
Povrat stare opreme
```

- typo-friendly search artikla
- količina
- napomena
- offline queue
- kod synca slati isti `client_event_id`, da nema duplanja

---

## Ne raditi

- ne rekonstruirati stare posudbe
- ne slati poruke članovima
- ne uvoditi claim/confirm sustav
- ne raditi QR/barcode workflow
- ne oduzimati automatski staru bazu od Klaićeve
- ne dirati druge module SOV-a u ovom build koraku
