# SOV Admin v1.4.50a — Stable Baseline

## Kratko

`v1.4.50a-stable-baseline` je freeze trenutne dobre baze nakon:

- Supabase RPC/RLS hardeninga do stanja gdje su javna samo 4 namjerna public RPC-a
- encrypted session storea
- log redactiona
- local-only HTTP guarda
- Apps Script key zaštite
- manifest intent-filter cleanupa
- logger thread hygiene cleanupa
- accessibility labelsa
- theme pickera
- prvih unit testova
- kontroliranih l10n passova
- finalnog regression checklista

## Trenutna oznaka

```text
versionCode = 900148
versionName = 1.4.50a-stable-baseline
```

## Zadnja stabilna Supabase sigurnosna točka

Supabase security baseline ostaje:

```text
v1.4.38a RPC decision cleanup
SECURITY DEFINER anon executable: 4
```

Namjerno javni RPC-evi:

```text
sov_news_public_list
sov_news_public_detail
sov_list_runner_leaderboard
sov_submit_runner_score
```

## Što se smije raditi nakon ovog baselinea

Nove promjene raditi samo kao male, jasno verzionirane pakete:

1. jedan problem = jedan build
2. prvo patch, onda lokalni build
3. ako build padne, prvo buildfix, bez novih featurea
4. Supabase promjene samo uz report i rollback bilješku
5. self-update ne dirati bez eksplicitne odluke

## Što ne dirati bez posebnog razloga

- self-update sustav
- login/session flow
- Supabase public/private granice
- Field Hub lokalni HTTP flow
- import GPX/KML/MBTiles/SOVPKG flow
- Oružarstvo request/inventura status model

## Minimalni test prije proglašenja stabilnim

Pokrenuti checklist iz:

```text
SOV_REGRESSION_CHECKLIST_1.4.49a.md
```

Za ovaj baseline dodatno provjeriti:

```text
Settings → tema: Sustav/Svijetla/Tamna
Moja baza import i prikaz
Laptop Hub ekran
GPS/Kompas/Signal ekrani
Karta/Baza nakon logina
Izleti nakon logina
Runner leaderboard i score submit
Public vijesti na webu
```
