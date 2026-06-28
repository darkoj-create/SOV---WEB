-- SOV Web v5.38 — TopoDroid / Nacrti import pipeline
-- Safe patch: nadograđuje v5.37 arhivu, ne dira bazu objekata.
-- RLS ostaje uključen.

create extension if not exists pgcrypto;

-- Role helper samo ako ne postoji.
do $$
begin
  if not exists (
    select 1 from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public' and p.proname='sov_has_role' and p.pronargs=1
  ) then
    execute $fn$
    create function public.sov_has_role(roles text[])
    returns boolean
    language sql
    stable
    security definer
    set search_path = public, auth
    as $body$
      select exists (
        select 1 from public.profiles p
        where p.id = auth.uid()
          and coalesce(p.status::text,'') = 'approved'
          and lower(coalesce(p.role::text,'user')) = any(roles)
      )
    $body$;
    $fn$;
  end if;
end $$;

create table if not exists public.sov_topodroid_import_batches (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  created_by uuid default auth.uid(),
  import_source text not null default 'web_5_38',
  batch_name text,
  status text not null default 'draft',
  source_file_count integer not null default 0,
  matched_count integer not null default 0,
  review_count integer not null default 0,
  imported_count integer not null default 0,
  note text,
  metadata jsonb not null default '{}'::jsonb
);

create table if not exists public.sov_topodroid_import_items (
  id uuid primary key default gen_random_uuid(),
  batch_id uuid references public.sov_topodroid_import_batches(id) on delete cascade,
  created_at timestamptz not null default now(),
  created_by uuid default auth.uid(),
  status text not null default 'review',
  source_file_name text,
  source_file_type text,
  source_file_size text,
  object_id text,
  object_name text,
  object_slug text,
  match_score integer,
  match_status text not null default 'manual_review',
  drawing_title text,
  drawing_type text not null default 'topodroid',
  file_format text,
  mime_type text,
  drive_file_id text,
  drive_url text,
  preview_url text,
  station text,
  azimuth_deg numeric,
  survey_length_m numeric,
  survey_year integer,
  author_name text,
  note text,
  raw_metadata jsonb not null default '{}'::jsonb,
  drawing_id uuid references public.speleo_object_drawings(id) on delete set null
);

alter table public.sov_topodroid_import_batches add column if not exists metadata jsonb default '{}'::jsonb;
alter table public.sov_topodroid_import_items add column if not exists raw_metadata jsonb default '{}'::jsonb;
alter table public.sov_topodroid_import_items add column if not exists drawing_id uuid references public.speleo_object_drawings(id) on delete set null;

create index if not exists sov_topodroid_import_items_batch_idx on public.sov_topodroid_import_items(batch_id);
create index if not exists sov_topodroid_import_items_object_idx on public.sov_topodroid_import_items(object_id);
create index if not exists sov_topodroid_import_items_status_idx on public.sov_topodroid_import_items(status);

alter table public.sov_topodroid_import_batches enable row level security;
alter table public.sov_topodroid_import_items enable row level security;

drop policy if exists "topodroid batches admin arhivar select" on public.sov_topodroid_import_batches;
drop policy if exists "topodroid batches admin arhivar write" on public.sov_topodroid_import_batches;
drop policy if exists "topodroid items admin arhivar select" on public.sov_topodroid_import_items;
drop policy if exists "topodroid items admin arhivar write" on public.sov_topodroid_import_items;

create policy "topodroid batches admin arhivar select"
  on public.sov_topodroid_import_batches for select
  using (public.sov_has_role(array['admin','arhivar']));

create policy "topodroid batches admin arhivar write"
  on public.sov_topodroid_import_batches for all
  using (public.sov_has_role(array['admin','arhivar']))
  with check (public.sov_has_role(array['admin','arhivar']));

create policy "topodroid items admin arhivar select"
  on public.sov_topodroid_import_items for select
  using (public.sov_has_role(array['admin','arhivar']));

create policy "topodroid items admin arhivar write"
  on public.sov_topodroid_import_items for all
  using (public.sov_has_role(array['admin','arhivar']))
  with check (public.sov_has_role(array['admin','arhivar']));

-- Publish jedan import item u canonical speleo_object_drawings.
create or replace function public.sov_publish_topodroid_import_item(item_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  it public.sov_topodroid_import_items%rowtype;
  new_id uuid;
begin
  if not public.sov_has_role(array['admin','arhivar']) then
    raise exception 'not_allowed';
  end if;

  select * into it from public.sov_topodroid_import_items where id = item_id;
  if not found then
    raise exception 'import_item_not_found';
  end if;

  insert into public.speleo_object_drawings (
    object_id, object_name, object_slug, drawing_title, drawing_type,
    archive_status, file_format, mime_type, drive_file_id, drive_file_name,
    drive_url, preview_url, source, author_name, survey_year, match_score,
    match_status, public_visible, metadata, note, synced_by, synced_at
  ) values (
    it.object_id, it.object_name, it.object_slug,
    coalesce(nullif(it.drawing_title,''), nullif(it.source_file_name,''), 'TopoDroid export'),
    coalesce(nullif(it.drawing_type,''),'topodroid'),
    'draft', it.file_format, it.mime_type, it.drive_file_id, it.source_file_name,
    it.drive_url, it.preview_url, 'topodroid_import_5_38', it.author_name, it.survey_year, it.match_score,
    it.match_status, true,
    jsonb_build_object(
      'import_item_id', it.id,
      'batch_id', it.batch_id,
      'station', it.station,
      'azimuth_deg', it.azimuth_deg,
      'survey_length_m', it.survey_length_m,
      'raw_metadata', it.raw_metadata
    ),
    it.note, auth.uid(), now()
  ) returning id into new_id;

  update public.sov_topodroid_import_items
  set status='imported', drawing_id=new_id
  where id=item_id;

  update public.sov_topodroid_import_batches b
  set imported_count = (
    select count(*) from public.sov_topodroid_import_items i where i.batch_id=b.id and i.status='imported'
  )
  where b.id = it.batch_id;

  insert into public.sov_audit_log(action, entity_type, entity_id, new_data, metadata)
  values ('publish_import_item','drawing',new_id::text,to_jsonb(it),jsonb_build_object('module','topodroid_import','import_item_id',item_id));

  return new_id;
end $$;

drop view if exists public.sov_topodroid_import_overview cascade;
create view public.sov_topodroid_import_overview as
select
  b.id,
  b.created_at,
  b.created_by,
  b.batch_name,
  b.status,
  b.source_file_count,
  count(i.id)::integer as item_count,
  count(*) filter (where i.status='review')::integer as review_count,
  count(*) filter (where i.status='ready')::integer as ready_count,
  count(*) filter (where i.status='imported')::integer as imported_count,
  b.note,
  b.metadata
from public.sov_topodroid_import_batches b
left join public.sov_topodroid_import_items i on i.batch_id=b.id
group by b.id;

comment on table public.sov_topodroid_import_batches is 'SOV v5.38 TopoDroid/nacrti import batch queue za Admin/Arhivar.';
comment on table public.sov_topodroid_import_items is 'SOV v5.38 review/import stavke prije objave u speleo_object_drawings.';
