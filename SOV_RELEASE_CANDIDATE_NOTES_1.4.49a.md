# SOV v1.4.49a — Release-candidate notes

## Status

`v1.4.49a` je release-candidate checkpoint prije `v1.4.50a STABLE BASELINE`.

Ovo nije feature build. Ovo je kontrolni build za provjeru da zadnja serija sitnih cleanupova nije napravila regresiju.

## Zadnji sigurni niz promjena

- `v1.4.34a` encrypted session
- `v1.4.35a` log redaction
- `v1.4.36a` local-only HTTP guard
- `v1.4.37a` Supabase RPC cleanup batch 2
- `v1.4.38a` final anon RPC decision cleanup
- `v1.4.39a` security baseline package
- `v1.4.40a` Apps Script key support
- `v1.4.41a` APK SHA update — rollback/odbačeno, ne koristiti kao bazu
- `v1.4.41b` manifest intent-filter cleanup
- `v1.4.42a` logger thread hygiene
- `v1.4.43a` accessibility labels
- `v1.4.44a` theme picker
- `v1.4.45a` first unit tests
- `v1.4.46a` l10n My Base
- `v1.4.47a` l10n Laptop Hub
- `v1.4.48a` l10n Field Status
- `v1.4.49a` final checklist

## Supabase security baseline

Zadnje potvrđeno stanje iz `v1.4.39a`:

- `SECURITY DEFINER anon executable`: 4
- Namjerno public:
  - `sov_news_public_list`
  - `sov_news_public_detail`
  - `sov_list_runner_leaderboard`
  - `sov_submit_runner_score`

Sve osjetljivo za mapu, izlete, debug, admin, ingest i maintenance više ne bi smjelo biti anonimno.

## Poznati oprez

1. Public web karta/izleti mogu trebati public-lite endpoint ako ih stvarno želimo za neprijavljene.
2. `usesCleartextTraffic=true` je namjerno ostavljen jer Field Hub i lokalni WMS trebaju HTTP; app-side guard blokira vanjski HTTP.
3. Apps Script guard ne deployati dok app i Script Properties nemaju isti `SOV_APPS_SCRIPT_KEY`.
4. Self-update sistem ne dirati; `v1.4.41a` SHA patch je odbačen.

## Kriterij za v1.4.50a stable

`v1.4.50a` se smije složiti tek kad:

- build prođe
- osnovni smoke test prođe
- nema novih crash/regression prijava iz v1.4.49a
- nema novih feature zahtjeva u istom buildu

## Preporuka

Ako `v1.4.49a` prođe lokalni build i osnovni test, `v1.4.50a` neka bude samo stable tag/baseline package, bez novih funkcija.
