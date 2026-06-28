# SOV Web v5.58.0 — Arhivar predane jame flow

## Što je dodano

### Arhivar dashboard
- Novi `arhivar-dashboard.html` s 3 ulaza:
  1. `Uređivanje arhive` → postojeći `arhivar.html`
  2. `Predane jame` → novi `arhivar-predane-jame.html`
  3. `Izvoz / paketi` → novi `arhivar-izvoz.html`
- Glavni `dashboard.html` sada vodi Arhivar modul na novi dashboard.

### Baza → predaja nove jame
- `baza.html` dobiva gumb `+ Predaj novu jamu`.
- Obrazac skuplja speleo zapisnik: naziv, tip, koordinate, HTRS, dubina/duljina, pristup, opis, istraživanje, hidrologija/morfologija, opasnosti, napomene.
- Podržani privitci: fotografije, nacrti, KML, GPX, TopoDroid ZIP/datoteke.
- Predaja ne ide odmah u bazu, nego u `speleo_object_submissions`.

### Arhivar → predane jame
- Lijevo lista predaja; desno detalj kao oružar/inbox flow.
- Arhivar može:
  - approve u SQL bazu,
  - označiti što fali,
  - odbiti,
  - exportati pojedinačnu predaju u CSV/XML/ZIP.

### Izvoz
- `arhivar-izvoz.html` izvozi predaje po statusu u CSV/XML/ZIP paket.

## SQL
Pokrenuti:
`SUPABASE_SOV_ARHIVAR_SUBMISSIONS_v5_58_0.sql`

SQL dodaje:
- `speleo_object_submissions`
- `speleo_object_submission_files`
- storage bucket `speleo-submissions` ako Supabase dopusti iz SQL editora
- RPC `sov_approve_speleo_submission`
- RPC `sov_mark_speleo_submission_needs_changes`

## Napomena
Ako bucket ne nastane automatski, ručno ga napraviti u Supabase Storage kao `speleo-submissions`.
