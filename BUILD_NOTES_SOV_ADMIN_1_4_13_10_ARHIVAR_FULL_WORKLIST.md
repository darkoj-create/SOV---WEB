# SOV Admin 1.4.13.10 — Arhivar full worklist

## Problem

APK Arhivar je prikazivao oko 800 objekata, dok web Arhivar prikazuje oko 1285 objekata. Uzrok je bio hardkodirani `limit=800` u `ArchiveSupabaseRepository.fetchWorklist()`.

## Fix

- Uklonjen hard limit od 800 objekata.
- Worklist sada čita `public.sov_arhivar_worklist` paginirano po 1000 redova (`limit` + `offset`) dok ne povuče cijeli feed.
- Dedupliciranje ide po `object_id` uz fallback key za rubne slučajeve.
- Cache key je bumpan na `sov_archive_work_cache_v6_full_worklist` da se ne vraća stari skraćeni cache.
- Arhivar UX + persistent login iz 1.4.13.9 ostaju uključeni.

## Version

- versionCode: `900056`
- versionName: `1.4.13.10-arhivar-full-worklist`
- expected APK: `SOV-ADMIN-1.4.13.10.apk`

## SQL

Nije potreban novi SQL. APK koristi postojeći `sov_arhivar_worklist` view i postojeće detail/update RPC-eve.
