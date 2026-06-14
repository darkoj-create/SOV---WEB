-- SOV Oružarstvo v6.1.38 — category icon + persistent category note
-- Safe schema/data migration. Does not touch equipment_items quantities or loans.

begin;

create table if not exists public.sov_armory_category_meta_backup_20260614 (
  backup_id bigserial primary key,
  backed_up_at timestamptz not null default now(),
  category_id uuid,
  category_before jsonb not null
);

insert into public.sov_armory_category_meta_backup_20260614 (category_id, category_before)
select c.id, to_jsonb(c)
from public.equipment_categories c
where not exists (
  select 1
  from public.sov_armory_category_meta_backup_20260614 b
  where b.category_id = c.id
);

alter table public.equipment_categories
  add column if not exists icon text,
  add column if not exists note text;

update public.equipment_categories
set icon = case name
  when 'Osobni SRT komplet' then '🧗'
  when 'Užad' then '🪢'
  when 'Sidrišta i opremanje' then '⚓'
  when 'Tehničko spašavanje i Čisto podzemlje' then '🛟'
  when 'Proširivanje i regulirana oprema' then '⛏️'
  when 'Mjerenje, crtanje i dokumentacija' then '📐'
  when 'Rasvjeta, elektronika i komunikacija' then '🔦'
  when 'Logor, ekspedicija i kuhinja' then '⛺'
  when 'Medicinska oprema' then '🧰'
  when 'Alpinistička i penjačka oprema' then '⛰️'
  when 'Ronilačka oprema' then '🤿'
  when 'Alat i održavanje' then '🛠️'
  else coalesce(icon, '📦')
end,
updated_at = now()
where icon is null or trim(icon) = '';

commit;
