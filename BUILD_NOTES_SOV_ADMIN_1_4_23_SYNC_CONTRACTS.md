# SOV Admin APK v1.4.23 — Sync contracts (oružarstvo write-RPC + arhivar _v2 + armory read-RPC)

Baseline: v1.4.22-armory-hidden-sync

Cilj: ukloniti mjesta gdje APK gađa sirove tablice umjesto istog
RPC ugovora koji web koristi, jer su to bila mjesta gdje su se
APK i web tiho razilazili.

## 1) Inventura stock → RPC (ISPRAVLJA STVARAN BUG)
- `EquipmentSupabaseRepository.submitInventoryRemote()`
- Prije: APK je sirovo PATCH-ao equipment_items i postavljao
  `quantity = counted`, `available = counted`. Time je BRISAO
  posuđenu količinu (loaned) iz totala → web i APK su pokazivali
  različit total za isti artikl.
- Sada: poziva `rpc/sov_armory_save_inventory_count` po artiklu,
  ista funkcija koju web koristi. RPC čuva loaned i računa
  `quantity = available + loaned`, postavlja status/labelu/datum.
- equipment_inventory_sessions + equipment_inventory_counts audit
  zapisi ostaju (povijest inventure, nije dirano).

## 2) Arhivar detalj objekta → _v2
- `ArchiveSupabaseRepository.loadObjectDetail()`
- `sov_arhivar_get_object_detail` → `sov_arhivar_get_object_detail_v2`.
- Isti parametar (`p_object_id text`). `_v2` vraća strogi superset
  ključeva koje APK već parsira → bez promjene parsera, bez rizika.
- Bio je jedini RPC gdje je APK zaostajao za webom (web je već na _v2).

## 3) Posudbe → read-RPC s fallbackom
- `EquipmentSupabaseRepository.fetchRequests()`
- Prvo pokušava `rpc/sov_armory_get_active_requests` (filter
  "nije skriveno" živi u bazi), pa ako RPC nije deployan PADNE NATRAG
  na v1.4.22 filtrirani upit `armory_hidden=not.is.true`.
- Zahtijeva: `SUPABASE_SOV_ARMORY_ACTIVE_REQUESTS_RPC_v1_4_23.sql`.
- Ako SQL NIJE pokrenut: ništa se ne lomi, koristi se fallback.

## Nije dirano
- Nema RLS izmjena. SQL je samo SELECT funkcija (#3) — bez mutacija.
- Lokacije, snapshoti, trips/tracking, import slojevi netaknuti.
- equipment_loans APK i dalje ne čita.
- _v2 unifikacija na WEB strani (web zove i stare i _v2 verzije
  paralelno) NIJE rađena ovdje — to je zaseban web redeploy i
  treba ga raditi promišljeno, ne uz APK build.

## Validacija
- { } i ( ) balansirani u oba mijenjana .kt fajla.
- Stari sirovi inventura PATCH uklonjen (0 pojava).
- looksLikeUuid / urlEncode / SimpleDateFormat i dalje korišteni drugdje.
- ZIP integrity test prolazi.
