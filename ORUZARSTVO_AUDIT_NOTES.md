# Oružarstvo audit — v4.26

Izvorni model: `SOV_Oruzarstvo_v1.xlsx`
Kontrolni izvor stanja: `Oružarstvo inventura opreme SOV - NOVO.xlsx`

## Što je popravljeno

- Užad je prije bila samo u internom tabu `Užad`, a korisnički zahtjev je čitao samo `DATA.items`, zato se užad nije mogla posuditi kroz katalog.
- Katalog je nadopunjen zadnjim stanjem iz stare inventurne tablice.
- Posebno su popravljene kategorije koje su u Claude modelu imale 0 ili su bile preskočene: Elektro/foto, Proširivanje, Medicina, Ostalo, Ekspedicijska/kamp.
- Dupli SKU kod užadi dobio je jedinstveni interni ID suffix, da Supabase import ne pregazi drugi komad istog SKU-a.

## Brojke

- Redova iz stare tablice uzeto u audit: 473
- Postojećih stavki ažurirano: 303
- Novih stavki dodano: 38
- Duplih rope ID-jeva popravljeno: 2
- Stavki u katalogu nakon audita: 482
- Užadi: 23 / 1397.0 m
