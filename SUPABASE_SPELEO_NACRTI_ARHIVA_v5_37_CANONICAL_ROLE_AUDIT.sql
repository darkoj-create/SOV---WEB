-- SOV Web v5.37 — Arhiva/Nacrti canonical + role/audit
-- Safe patch: ne dira bazu objekata; nadograđuje tablicu nacrta i RLS.

create extension if not exists pgcrypto;

-- Minimal role helper only if project does not already have it.
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
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and coalesce(p.status::text,'') = 'approved'
          and lower(coalesce(p.role::text,'user')) = any(roles)
      )
    $body$;
    $fn$;
  end if;
end $$;

create table if not exists public.sov_audit_log (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  actor_id uuid default auth.uid(),
  actor_email text,
  action text not null,
  entity_type text not null,
  entity_id text,
  old_data jsonb,
  new_data jsonb,
  metadata jsonb not null default '{}'::jsonb
);

create table if not exists public.speleo_object_drawings (
  id uuid primary key default gen_random_uuid(),
  object_id text,
  object_name text,
  object_slug text,
  plate_number text,
  region text,
  drawing_title text,
  drawing_type text not null default 'nacrt',
  archive_status text not null default 'draft',
  file_format text,
  mime_type text,
  file_size text,
  drive_file_id text,
  drive_file_name text,
  drive_url text,
  preview_url text,
  source text not null default 'manual',
  author_name text,
  survey_year integer,
  match_score integer,
  match_status text not null default 'manual_review',
  public_visible boolean not null default true,
  verified_by uuid,
  verified_at timestamptz,
  synced_by uuid,
  synced_at timestamptz,
  created_by uuid default auth.uid(),
  created_at timestamptz not null default now(),
  updated_by uuid,
  updated_at timestamptz not null default now(),
  metadata jsonb not null default '{}'::jsonb,
  note text
);

alter table public.speleo_object_drawings add column if not exists object_id text;
alter table public.speleo_object_drawings add column if not exists object_name text;
alter table public.speleo_object_drawings add column if not exists object_slug text;
alter table public.speleo_object_drawings add column if not exists plate_number text;
alter table public.speleo_object_drawings add column if not exists region text;
alter table public.speleo_object_drawings add column if not exists drawing_title text;
alter table public.speleo_object_drawings add column if not exists drawing_type text default 'nacrt';
alter table public.speleo_object_drawings add column if not exists archive_status text default 'draft';
alter table public.speleo_object_drawings add column if not exists file_format text;
alter table public.speleo_object_drawings add column if not exists mime_type text;
alter table public.speleo_object_drawings add column if not exists file_size text;
alter table public.speleo_object_drawings add column if not exists drive_file_id text;
alter table public.speleo_object_drawings add column if not exists drive_file_name text;
alter table public.speleo_object_drawings add column if not exists drive_url text;
alter table public.speleo_object_drawings add column if not exists preview_url text;
alter table public.speleo_object_drawings add column if not exists source text default 'manual';
alter table public.speleo_object_drawings add column if not exists author_name text;
alter table public.speleo_object_drawings add column if not exists survey_year integer;
alter table public.speleo_object_drawings add column if not exists match_score integer;
alter table public.speleo_object_drawings add column if not exists match_status text default 'manual_review';
alter table public.speleo_object_drawings add column if not exists public_visible boolean default true;
alter table public.speleo_object_drawings add column if not exists verified_by uuid;
alter table public.speleo_object_drawings add column if not exists verified_at timestamptz;
alter table public.speleo_object_drawings add column if not exists synced_by uuid;
alter table public.speleo_object_drawings add column if not exists synced_at timestamptz;
alter table public.speleo_object_drawings add column if not exists created_by uuid default auth.uid();
alter table public.speleo_object_drawings add column if not exists created_at timestamptz default now();
alter table public.speleo_object_drawings add column if not exists updated_by uuid;
alter table public.speleo_object_drawings add column if not exists updated_at timestamptz default now();
alter table public.speleo_object_drawings add column if not exists metadata jsonb default '{}'::jsonb;
alter table public.speleo_object_drawings add column if not exists note text;

update public.speleo_object_drawings
set drawing_title = coalesce(nullif(drawing_title,''), nullif(drive_file_name,''), nullif(object_name,''), 'Nacrt'),
    file_format = coalesce(nullif(file_format,''), upper(regexp_replace(coalesce(drive_file_name,''), '^.*\.', ''))),
    archive_status = coalesce(nullif(archive_status,''), case when coalesce(public_visible,true) then 'published' else 'draft' end),
    source = coalesce(nullif(source,''), 'legacy')
where drawing_title is null or file_format is null or archive_status is null or source is null;

create unique index if not exists speleo_object_drawings_drive_file_id_uq
  on public.speleo_object_drawings (drive_file_id)
  where drive_file_id is not null and drive_file_id <> '';
create index if not exists speleo_object_drawings_object_id_idx on public.speleo_object_drawings (object_id);
create index if not exists speleo_object_drawings_object_name_idx on public.speleo_object_drawings (lower(coalesce(object_name,'')));
create index if not exists speleo_object_drawings_status_idx on public.speleo_object_drawings (archive_status);
create index if not exists speleo_object_drawings_type_idx on public.speleo_object_drawings (drawing_type);

create or replace function public.sov_touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  new.updated_by = auth.uid();
  return new;
end $$;

drop trigger if exists speleo_object_drawings_touch_updated_at on public.speleo_object_drawings;
create trigger speleo_object_drawings_touch_updated_at
before update on public.speleo_object_drawings
for each row execute function public.sov_touch_updated_at();

create or replace function public.sov_audit_drawings_change()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  insert into public.sov_audit_log(action, entity_type, entity_id, old_data, new_data, metadata)
  values (
    lower(tg_op),
    'drawing',
    coalesce(new.id::text, old.id::text),
    case when tg_op in ('UPDATE','DELETE') then to_jsonb(old) else null end,
    case when tg_op in ('INSERT','UPDATE') then to_jsonb(new) else null end,
    jsonb_build_object('module','arhiva_nacrti','table','speleo_object_drawings')
  );
  return coalesce(new, old);
exception when others then
  return coalesce(new, old);
end $$;

drop trigger if exists speleo_object_drawings_audit on public.speleo_object_drawings;
create trigger speleo_object_drawings_audit
after insert or update or delete on public.speleo_object_drawings
for each row execute function public.sov_audit_drawings_change();

alter table public.speleo_object_drawings enable row level security;

drop policy if exists "speleo drawings public select" on public.speleo_object_drawings;
drop policy if exists "speleo drawings approved select" on public.speleo_object_drawings;
drop policy if exists "speleo drawings admin archive insert" on public.speleo_object_drawings;
drop policy if exists "speleo drawings admin archive update" on public.speleo_object_drawings;
drop policy if exists "speleo drawings admin archive delete" on public.speleo_object_drawings;
drop policy if exists "speleo drawings admin sync insert" on public.speleo_object_drawings;
drop policy if exists "speleo drawings admin sync update" on public.speleo_object_drawings;
drop policy if exists "speleo drawings open select" on public.speleo_object_drawings;
drop policy if exists "speleo drawings open insert" on public.speleo_object_drawings;
drop policy if exists "speleo drawings open update" on public.speleo_object_drawings;

-- Svi odobreni korisnici vide public/published nacrte.
create policy "speleo drawings approved select"
  on public.speleo_object_drawings for select
  using (auth.uid() is not null and coalesce(public_visible,true) = true);

-- Admin + Arhivar imaju operativni pristup.
create policy "speleo drawings admin archive insert"
  on public.speleo_object_drawings for insert
  with check (public.sov_has_role(array['admin','arhivar']));

create policy "speleo drawings admin archive update"
  on public.speleo_object_drawings for update
  using (public.sov_has_role(array['admin','arhivar']))
  with check (public.sov_has_role(array['admin','arhivar']));

create policy "speleo drawings admin archive delete"
  on public.speleo_object_drawings for delete
  using (public.sov_has_role(array['admin']));

drop view if exists public.sov_drawings_public cascade;
create view public.sov_drawings_public as
select
  id, object_id, object_name, object_slug, plate_number, region,
  drawing_title, drawing_type, archive_status,
  file_format, mime_type, file_size,
  drive_file_id, drive_file_name, drive_url, preview_url,
  author_name, survey_year,
  match_score, match_status, public_visible,
  source, synced_at, verified_at, created_at, updated_at,
  metadata, note
from public.speleo_object_drawings
where coalesce(public_visible,true) = true;

drop view if exists public.sov_drawings_admin_overview cascade;
create view public.sov_drawings_admin_overview as
select
  d.*,
  coalesce(d.object_name, d.metadata->>'object_name', d.drive_file_name, d.drawing_title) as search_label,
  case
    when d.archive_status in ('published','verified') then 'ok'
    when d.match_status in ('needs_review','manual_review') then 'review'
    else 'draft'
  end as archive_health
from public.speleo_object_drawings d;

comment on table public.speleo_object_drawings is 'SOV canonical arhiva/nacrti: nacrti, TopoDroid exporti, Drive linkovi i metadata vezani uz speleo objekte.';
