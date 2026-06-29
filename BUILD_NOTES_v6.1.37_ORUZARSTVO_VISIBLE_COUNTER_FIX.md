# SOV Web v6.1.37 — Oružarstvo visible counter fix

Fix za situaciju gdje `legacy_return` prođe, ali se broj u web inventaru ne promijeni.

## Što je promijenjeno

- Dodan SQL Build 3: `SUPABASE_ORUZARSTVO_V2_1_BUILD3_LEGACY_RETURN_VISIBLE_COUNTER_FIX.sql`.
- RPC `sov_armory_record_legacy_return` sada eksplicitno ažurira vidljive brojače u `equipment_items`:
  - `quantity`
  - `available`
  - `quantity_label`
  - `available_label`
  - `updated_at`
- RPC i dalje piše `sov_armory_stock_movements` i `equipment_item_locations`.
- Dodana/obnovljena manifest invalidacija za web cache.
- Web nakon spremanja briše sve poznate armory cache ključeve i radi strogi live refresh.
- Web radi i optimistički update kartice odmah nakon uspješnog RPC odgovora.

## Važno

Ako se u modalu odabere `Za provjeru` ili `Oštećeno`, ukupna količina raste, ali dostupno stanje ne raste. To je namjerno.

## Redoslijed

1. Pokrenuti SQL Build 3 u Supabase SQL editoru.
2. Deployati ovaj web ZIP.
3. Hard refresh browsera / mobitela.
4. Testirati `Povrat stare` na jednom artiklu s condition `OK / raspoloživo`.
