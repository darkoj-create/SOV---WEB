-- SOV Arhivar v5.58.1 — Predane jame / submission → review → approve → baza
-- Pokreni nakon v5.57.8 SQL-a. Idempotentno: create if not exists / create or replace.
-- Namjena:
-- 1) Baza korisniku daje obrazac za novu jamu + privitke.
-- 2) Arhivar vidi predane jame, odobrava ili označava što fali.
-- 3) Tek approve upisuje objekt u speleo_objects_staging.

create extension if not exists pgcrypto;

-- -------------------------------------------------------------
-- 0b. Role/profile compatibility helpers
-- Fix v5.58.1: do NOT hard-reference a single profile table in RLS policies.
-- Existing SOV builds usually use public.profiles. Some builds may use
-- public.sov_profiles or public.sov_user_profiles. These helpers use dynamic SQL
-- and to_regclass(), so the script does not fail when one table is absent.
-- -------------------------------------------------------------

create or replace function public.sov_submission_current_role_safe()
returns text
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
  v_role text;
  v_table text;
  v_schema text;
  v_name text;
  v_id_col text;
begin
  if v_uid is null then
    return 'anon';
  end if;

  if to_regprocedure('public.sov_current_role()') is not null then
    begin
      execute 'select public.sov_current_role()::text' into v_role;
    exception when others then
      v_role := null;
    end;
  end if;

  if v_role is not null and length(trim(v_role)) > 0 then
    return lower(trim(v_role));
  end if;

  foreach v_table in array array['public.profiles','public.sov_profiles','public.sov_user_profiles'] loop
    if to_regclass(v_table) is not null then
      v_schema := split_part(v_table, '.', 1);
      v_name := split_part(v_table, '.', 2);
      v_id_col := null;

      if exists (select 1 from information_schema.columns where table_schema=v_schema and table_name=v_name and column_name='id') then
        v_id_col := 'id';
      elsif exists (select 1 from information_schema.columns where table_schema=v_schema and table_name=v_name and column_name='user_id') then
        v_id_col := 'user_id';
      end if;

      if v_id_col is not null and exists (select 1 from information_schema.columns where table_schema=v_schema and table_name=v_name and column_name='role') then
        begin
          execute format('select role::text from %s where %I = $1 limit 1', v_table, v_id_col)
            into v_role
            using v_uid;
        exception when others then
          v_role := null;
        end;
        if v_role is not null and length(trim(v_role)) > 0 then
          return lower(trim(v_role));
        end if;
      end if;
    end if;
  end loop;

  return 'user';
end;
$$;

create or replace function public.sov_submission_current_status_safe()
returns text
language plpgsql
stable
security definer
set search_path = public, auth
as $$
declare
  v_uid uuid := auth.uid();
  v_status text;
  v_table text;
  v_schema text;
  v_name text;
  v_id_col text;
begin
  if v_uid is null then
    return 'anon';
  end if;

  if to_regprocedure('public.sov_current_status()') is not null then
    begin
      execute 'select public.sov_current_status()::text' into v_status;
    exception when others then
      v_status := null;
    end;
  end if;

  if v_status is not null and length(trim(v_status)) > 0 then
    return lower(trim(v_status));
  end if;

  foreach v_table in array array['public.profiles','public.sov_profiles','public.sov_user_profiles'] loop
    if to_regclass(v_table) is not null then
      v_schema := split_part(v_table, '.', 1);
      v_name := split_part(v_table, '.', 2);
      v_id_col := null;

      if exists (select 1 from information_schema.columns where table_schema=v_schema and table_name=v_name and column_name='id') then
        v_id_col := 'id';
      elsif exists (select 1 from information_schema.columns where table_schema=v_schema and table_name=v_name and column_name='user_id') then
        v_id_col := 'user_id';
      end if;

      if v_id_col is not null and exists (select 1 from information_schema.columns where table_schema=v_schema and table_name=v_name and column_name='status') then
        begin
          execute format('select status::text from %s where %I = $1 limit 1', v_table, v_id_col)
            into v_status
            using v_uid;
        exception when others then
          v_status := null;
        end;
        if v_status is not null and length(trim(v_status)) > 0 then
          return lower(trim(v_status));
        end if;
      end if;
    end if;
  end loop;

  -- If a role table has no status column, role is enough.
  return 'approved';
end;
$$;

create or replace function public.sov_submissions_is_reviewer()
returns boolean
language sql
stable
security definer
set search_path = public, auth
as $$
  select public.sov_submission_current_role_safe() in ('admin','arhivar')
     and public.sov_submission_current_status_safe() in ('approved','active','enabled','ok');
$$;

grant execute on function public.sov_submission_current_role_safe() to authenticated;
grant execute on function public.sov_submission_current_status_safe() to authenticated;
grant execute on function public.sov_submissions_is_reviewer() to authenticated;


-- Storage bucket za privitke. Ako storage schema nije dostupna u SQL editoru, ovaj blok možeš preskočiti i bucket napraviti ručno.
do $$
begin
  if exists (select 1 from information_schema.schemata where schema_name = 'storage') then
    insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
    values ('speleo-submissions', 'speleo-submissions', false, 104857600, null)
    on conflict (id) do nothing;
  end if;
exception when others then
  raise notice 'Storage bucket nije napravljen iz SQL-a: %', sqlerrm;
end $$;

create table if not exists public.speleo_object_submissions (
  id uuid primary key default gen_random_uuid(),
  status text not null default 'submitted' check (status in ('draft','submitted','needs_changes','approved','rejected')),
  source text not null default 'baza.html',
  submitted_by uuid default auth.uid(),
  submitter_email text,
  submitter_name text,

  object_name text not null,
  object_type text,
  county text,
  municipality text,
  nearest_place text,
  locality text,
  lat double precision,
  lon double precision,
  htrs_x text,
  htrs_y text,
  depth_m double precision,
  length_m double precision,
  survey_date date,
  team text,
  access_description text,
  technical_description text,
  research_history text,
  notes text,

  record_json jsonb not null default '{}'::jsonb,
  missing_categories text[] not null default '{}'::text[],
  archivist_note text,
  reviewed_by uuid,
  reviewed_at timestamptz,
  approved_by uuid,
  approved_at timestamptz,
  approved_object_id text,
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.speleo_object_submission_files (
  id uuid primary key default gen_random_uuid(),
  submission_id uuid not null references public.speleo_object_submissions(id) on delete cascade,
  file_type text not null check (file_type in ('photo','drawing','kml','gpx','topodroid_zip','other')),
  file_name text not null,
  mime_type text,
  size_bytes bigint,
  storage_bucket text not null default 'speleo-submissions',
  storage_path text not null,
  public_url text,
  metadata jsonb not null default '{}'::jsonb,
  uploaded_by uuid default auth.uid(),
  created_at timestamptz not null default now()
);

create index if not exists idx_speleo_submissions_status on public.speleo_object_submissions(status, created_at desc);
create index if not exists idx_speleo_submissions_name on public.speleo_object_submissions using gin (to_tsvector('simple', coalesce(object_name,'')));
create index if not exists idx_speleo_submissions_submitter on public.speleo_object_submissions(submitted_by, created_at desc);
create index if not exists idx_speleo_submission_files_submission on public.speleo_object_submission_files(submission_id);

create or replace function public.touch_speleo_object_submissions_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_touch_speleo_object_submissions on public.speleo_object_submissions;
create trigger trg_touch_speleo_object_submissions
before update on public.speleo_object_submissions
for each row execute function public.touch_speleo_object_submissions_updated_at();

alter table public.speleo_object_submissions enable row level security;
alter table public.speleo_object_submission_files enable row level security;

drop policy if exists "speleo submissions insert authenticated" on public.speleo_object_submissions;
create policy "speleo submissions insert authenticated"
on public.speleo_object_submissions for insert
to authenticated
with check (submitted_by is null or submitted_by = auth.uid());

drop policy if exists "speleo submissions read own or archive" on public.speleo_object_submissions;
create policy "speleo submissions read own or archive"
on public.speleo_object_submissions for select
to authenticated
using (
  submitted_by = auth.uid()
  or public.sov_submissions_is_reviewer()
);

drop policy if exists "speleo submissions update own draft or archive" on public.speleo_object_submissions;
create policy "speleo submissions update own draft or archive"
on public.speleo_object_submissions for update
to authenticated
using (
  (submitted_by = auth.uid() and status in ('draft','needs_changes'))
  or public.sov_submissions_is_reviewer()
)
with check (
  (submitted_by = auth.uid() and status in ('draft','submitted','needs_changes'))
  or public.sov_submissions_is_reviewer()
);

drop policy if exists "speleo submission files insert authenticated" on public.speleo_object_submission_files;
create policy "speleo submission files insert authenticated"
on public.speleo_object_submission_files for insert
to authenticated
with check (
  exists (
    select 1 from public.speleo_object_submissions s
    where s.id = submission_id and (s.submitted_by = auth.uid() or s.submitted_by is null)
  )
);

drop policy if exists "speleo submission files read own or archive" on public.speleo_object_submission_files;
create policy "speleo submission files read own or archive"
on public.speleo_object_submission_files for select
to authenticated
using (
  exists (
    select 1 from public.speleo_object_submissions s
    where s.id = submission_id
      and (
        s.submitted_by = auth.uid()
        or public.sov_submissions_is_reviewer()
      )
  )
);

-- Storage RLS policies, safe no-op if storage schema is unavailable.
do $$
begin
  if exists (select 1 from information_schema.tables where table_schema='storage' and table_name='objects') then
    drop policy if exists "speleo submissions upload own folder" on storage.objects;
    create policy "speleo submissions upload own folder"
    on storage.objects for insert to authenticated
    with check (bucket_id = 'speleo-submissions');

    drop policy if exists "speleo submissions read own or archive" on storage.objects;
    create policy "speleo submissions read own or archive"
    on storage.objects for select to authenticated
    using (bucket_id = 'speleo-submissions');
  end if;
exception when others then
  raise notice 'Storage policies nisu postavljene: %', sqlerrm;
end $$;

create or replace view public.speleo_object_submissions_review as
select
  s.*,
  coalesce(f.file_count,0) as file_count,
  coalesce(f.photo_count,0) as photo_count,
  coalesce(f.drawing_count,0) as drawing_count,
  coalesce(f.kml_count,0) as kml_count,
  coalesce(f.gpx_count,0) as gpx_count,
  coalesce(f.topodroid_count,0) as topodroid_count
from public.speleo_object_submissions s
left join (
  select submission_id,
    count(*) as file_count,
    count(*) filter (where file_type='photo') as photo_count,
    count(*) filter (where file_type='drawing') as drawing_count,
    count(*) filter (where file_type='kml') as kml_count,
    count(*) filter (where file_type='gpx') as gpx_count,
    count(*) filter (where file_type='topodroid_zip') as topodroid_count
  from public.speleo_object_submission_files
  group by submission_id
) f on f.submission_id = s.id;

create or replace function public.sov_mark_speleo_submission_needs_changes(
  p_submission_id uuid,
  p_missing_categories text[] default '{}'::text[],
  p_archivist_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
begin
  if not public.sov_submissions_is_reviewer() then
    raise exception 'Samo admin/arhivar može označiti predaju.';
  end if;

  update public.speleo_object_submissions
  set status = 'needs_changes',
      missing_categories = coalesce(p_missing_categories,'{}'::text[]),
      archivist_note = p_archivist_note,
      reviewed_by = auth.uid(),
      reviewed_at = now()
  where id = p_submission_id;

  return jsonb_build_object('ok', true, 'status', 'needs_changes');
end;
$$;

create or replace function public.sov_approve_speleo_submission(
  p_submission_id uuid,
  p_archivist_note text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  s public.speleo_object_submissions%rowtype;
  v_source_id text;
begin
  if not public.sov_submissions_is_reviewer() then
    raise exception 'Samo admin/arhivar može odobriti predaju.';
  end if;

  select * into s from public.speleo_object_submissions where id = p_submission_id;
  if not found then raise exception 'Predaja ne postoji.'; end if;

  v_source_id := 'submission_' || s.id::text;

  insert into public.speleo_objects_staging (
    source_id, source_system, name, lat, lon, cadastre_status, cadastral_number, record_status,
    object_type_final, county, municipality, nearest_place, locality, depth_m, length_m,
    field_tasks, workflow_raw, raw, import_batch
  ) values (
    v_source_id,
    'sov_user_submission',
    s.object_name,
    s.lat,
    s.lon,
    'nije_u_katastru_provjeriti',
    null,
    'predano_arhivaru_odobreno',
    s.object_type,
    s.county,
    s.municipality,
    s.nearest_place,
    s.locality,
    s.depth_m,
    s.length_m,
    '',
    concat_ws(E'\n', 'Predano iz SOV baze/weba', 'Odobrio arhivar', p_archivist_note),
    coalesce(s.record_json,'{}'::jsonb) || jsonb_build_object(
      'submission_id', s.id,
      'survey_date', s.survey_date,
      'team', s.team,
      'access_description', s.access_description,
      'technical_description', s.technical_description,
      'research_history', s.research_history,
      'notes', s.notes,
      'approved_by', auth.uid(),
      'approved_at', now()
    ),
    'sov_arhivar_submission_v5_58_1'
  )
  on conflict (source_id) do update set
    name = excluded.name,
    lat = excluded.lat,
    lon = excluded.lon,
    record_status = excluded.record_status,
    object_type_final = excluded.object_type_final,
    county = excluded.county,
    municipality = excluded.municipality,
    nearest_place = excluded.nearest_place,
    locality = excluded.locality,
    depth_m = excluded.depth_m,
    length_m = excluded.length_m,
    workflow_raw = excluded.workflow_raw,
    raw = excluded.raw,
    updated_at = now();

  insert into public.sov_archive_object_status (
    object_id, object_name, has_coordinates, has_drawing, has_record, has_plate, has_photo,
    needs_redraw, archive_status, priority, last_note, source, metadata, updated_by, updated_at
  ) values (
    v_source_id,
    s.object_name,
    s.lat is not null and s.lon is not null,
    exists(select 1 from public.speleo_object_submission_files f where f.submission_id=s.id and f.file_type='drawing'),
    true,
    false,
    exists(select 1 from public.speleo_object_submission_files f where f.submission_id=s.id and f.file_type='photo'),
    false,
    'needs_review',
    'normal',
    coalesce(p_archivist_note, 'Odobreno iz predane jame; provjeriti pločicu i finalni katastarski status.'),
    'submission_approve',
    jsonb_build_object('submission_id', s.id, 'source', 'sov_approve_speleo_submission'),
    auth.uid(),
    now()
  )
  on conflict (object_id) do update set
    object_name = excluded.object_name,
    has_coordinates = excluded.has_coordinates,
    has_drawing = excluded.has_drawing,
    has_record = excluded.has_record,
    has_photo = excluded.has_photo,
    last_note = excluded.last_note,
    metadata = excluded.metadata,
    updated_by = auth.uid(),
    updated_at = now();

  update public.speleo_object_submissions
  set status='approved',
      archivist_note = coalesce(p_archivist_note, archivist_note),
      reviewed_by = auth.uid(),
      reviewed_at = now(),
      approved_by = auth.uid(),
      approved_at = now(),
      approved_object_id = v_source_id
  where id = s.id;

  return jsonb_build_object('ok', true, 'source_id', v_source_id, 'status', 'approved');
end;
$$;

grant execute on function public.sov_mark_speleo_submission_needs_changes(uuid,text[],text) to authenticated;
grant execute on function public.sov_approve_speleo_submission(uuid,text) to authenticated;
grant select, insert, update on public.speleo_object_submissions to authenticated;
grant select, insert on public.speleo_object_submission_files to authenticated;
grant select on public.speleo_object_submissions_review to authenticated;

comment on table public.speleo_object_submissions is 'Korisničke predaje novih jama iz Baza ekrana. Tek Arhivar approve upisuje u speleo_objects_staging.';

-- v5.58.1 fix: RLS/RPC role checks no longer hard-reference one missing profile table.
