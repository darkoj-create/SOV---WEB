-- =====================================================================
-- SOV Oružarstvo — poravnanje kanonskog redoslijeda kategorija
-- Datum: 2026-06-30  | Build ref: web v6.1.45ai / apk v1.4.28
-- Cilj: sov_armory_category_priority mora poznavati STVARNE (nove) nazive
--       kategorija koji se nalaze u equipment_items / katalog view-u.
--       Ranija verzija je referencirala stare nazive -> priority=999 za
--       10 od 12 kategorija -> pokvaren redoslijed na webu i u APK-u.
--
-- Kanonski redoslijed (dogovoren):
--   10  Osobni SRT komplet            (web prikaz: "Osobna oprema")
--   20  Sidrišta i opremanje
--   30  Tehničko spašavanje i Čisto podzemlje
--   40  Mjerenje, crtanje i dokumentacija
--   50  Proširivanje i regulirana oprema
--   60  Rasvjeta, elektronika i komunikacija
--   70  Alpinistička i penjačka oprema
--   80  Ronilačka oprema
--   90  Alat i održavanje
--  100  Užad
--  110  Logor, ekspedicija i kuhinja
--  120  Medicinska oprema
--  999  Ostalo / nepoznato
--
-- View-i (sov_equipment_app_catalog / _grouped) automatski preuzimaju novi
-- priority jer pozivaju ovu funkciju — nije potrebna izmjena view-a.
-- Idempotentno: create or replace. Rollback = vratiti staru definiciju.
-- =====================================================================

create or replace function public.sov_armory_category_priority(category text)
returns int
language sql
immutable
as $$
  select case lower(regexp_replace(btrim(coalesce(category, '')), '\s+', ' ', 'g'))
    -- 10 — Osobna oprema / Osobni SRT komplet
    when 'osobni srt komplet'                      then 10
    when 'osobna oprema'                           then 10
    when 'osobna oprema - komplet'                 then 10
    -- 20 — Sidrišta i opremanje
    when 'sidrišta i opremanje'                    then 20
    when 'oprema za postavljanje'                  then 20
    -- 30 — Tehničko spašavanje i Čisto podzemlje
    when 'tehničko spašavanje i čisto podzemlje'   then 30
    when 'čisto podzemlje'                          then 30
    -- 40 — Mjerenje, crtanje i dokumentacija
    when 'mjerenje, crtanje i dokumentacija'       then 40
    when 'oprema za crtanje'                        then 40
    -- 50 — Proširivanje i regulirana oprema
    when 'proširivanje i regulirana oprema'        then 50
    when 'oprema za proširivanje'                  then 50
    -- 60 — Rasvjeta, elektronika i komunikacija
    when 'rasvjeta, elektronika i komunikacija'    then 60
    when 'elektro i foto oprema'                    then 60
    -- 70 — Alpinistička i penjačka oprema
    when 'alpinistička i penjačka oprema'          then 70
    when 'alpinistička oprema'                      then 70
    -- 80 — Ronilačka oprema
    when 'ronilačka oprema'                         then 80
    -- 90 — Alat i održavanje
    when 'alat i održavanje'                        then 90
    when 'ostali alat'                              then 90
    -- 100 — Užad
    when 'užad'                                     then 100
    when 'užeta'                                    then 100
    -- 110 — Logor, ekspedicija i kuhinja
    when 'logor, ekspedicija i kuhinja'            then 110
    when 'oprema za logor'                          then 110
    -- 120 — Medicinska oprema
    when 'medicinska oprema'                        then 120
    else 999
  end
$$;

-- Provjera nakon primjene:
-- select main_category, priority, count(*)
-- from public.sov_equipment_app_catalog_grouped
-- group by 1,2 order by priority, 1;
