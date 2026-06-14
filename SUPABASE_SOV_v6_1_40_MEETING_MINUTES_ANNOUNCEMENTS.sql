begin;

create table if not exists public.meeting_minutes (
  id uuid primary key default gen_random_uuid(),
  document_id uuid references public.sov_document_archive(id) on delete set null,
  meeting_date date not null,
  title text not null,
  original_filename text,
  storage_bucket text default 'sov-documents',
  storage_path text,
  checksum_sha256 text,
  plain_text text,
  announcements_text text,
  meeting_leader text,
  minutes_taker text,
  source text not null default 'manual_upload',
  gmail_message_id text,
  status text not null default 'draft',
  parsed_at timestamptz,
  imported_by uuid default auth.uid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  meta jsonb not null default '{}'::jsonb,
  constraint meeting_minutes_status_check check (status in ('draft','parsed','needs_review','archived','error'))
);

create unique index if not exists meeting_minutes_checksum_unique
  on public.meeting_minutes(checksum_sha256)
  where checksum_sha256 is not null and checksum_sha256 <> '';

create index if not exists meeting_minutes_date_idx on public.meeting_minutes(meeting_date desc);
create index if not exists meeting_minutes_document_id_idx on public.meeting_minutes(document_id);

create table if not exists public.trip_announcements_staging (
  id uuid primary key default gen_random_uuid(),
  meeting_minutes_id uuid references public.meeting_minutes(id) on delete cascade,
  document_id uuid references public.sov_document_archive(id) on delete set null,
  source_document_title text,
  source_meeting_date date,
  raw_text text not null,
  title text not null,
  location_name text,
  start_date date,
  end_date date,
  leader_name text,
  trip_category text not null default 'izlet',
  description text,
  confidence numeric(5,2) default 0,
  status text not null default 'novo',
  created_trip_id uuid references public.sov_trips(id) on delete set null,
  review_note text,
  created_by uuid default auth.uid(),
  reviewed_by uuid,
  approved_at timestamptz,
  rejected_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  meta jsonb not null default '{}'::jsonb,
  constraint trip_announcements_status_check check (status in ('novo','treba_provjeru','odobreno','odbijeno','duplikat'))
);

create index if not exists trip_announcements_staging_status_idx on public.trip_announcements_staging(status, start_date);
create index if not exists trip_announcements_staging_meeting_idx on public.trip_announcements_staging(meeting_minutes_id);
create index if not exists trip_announcements_staging_doc_idx on public.trip_announcements_staging(document_id);

alter table public.meeting_minutes enable row level security;
alter table public.trip_announcements_staging enable row level security;

drop policy if exists "meeting minutes staff read" on public.meeting_minutes;
create policy "meeting minutes staff read" on public.meeting_minutes
  for select using (auth.role() = 'authenticated' and public.sov_documents_is_staff());

drop policy if exists "meeting minutes staff insert" on public.meeting_minutes;
create policy "meeting minutes staff insert" on public.meeting_minutes
  for insert with check (public.sov_documents_is_staff());

drop policy if exists "meeting minutes staff update" on public.meeting_minutes;
create policy "meeting minutes staff update" on public.meeting_minutes
  for update using (public.sov_documents_is_staff()) with check (public.sov_documents_is_staff());

drop policy if exists "meeting minutes staff delete" on public.meeting_minutes;
create policy "meeting minutes staff delete" on public.meeting_minutes
  for delete using (public.sov_documents_is_staff());

drop policy if exists "trip announcements staff read" on public.trip_announcements_staging;
create policy "trip announcements staff read" on public.trip_announcements_staging
  for select using (auth.role() = 'authenticated' and public.sov_documents_is_staff());

drop policy if exists "trip announcements staff insert" on public.trip_announcements_staging;
create policy "trip announcements staff insert" on public.trip_announcements_staging
  for insert with check (public.sov_documents_is_staff());

drop policy if exists "trip announcements staff update" on public.trip_announcements_staging;
create policy "trip announcements staff update" on public.trip_announcements_staging
  for update using (public.sov_documents_is_staff()) with check (public.sov_documents_is_staff());

drop policy if exists "trip announcements staff delete" on public.trip_announcements_staging;
create policy "trip announcements staff delete" on public.trip_announcements_staging
  for delete using (public.sov_documents_is_staff());

create or replace function public.sov_approve_trip_announcement(p_announcement_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_a public.trip_announcements_staging%rowtype;
  v_trip_id uuid;
  v_user uuid;
begin
  if not public.sov_documents_is_staff() then
    raise exception 'Not allowed';
  end if;

  begin
    v_user := auth.uid();
  exception when others then
    v_user := null;
  end;

  select * into v_a
  from public.trip_announcements_staging
  where id = p_announcement_id
  for update;

  if not found then
    raise exception 'Announcement not found';
  end if;

  if v_a.status = 'odobreno' and v_a.created_trip_id is not null then
    return v_a.created_trip_id;
  end if;

  insert into public.sov_trips (
    title, start_date, end_date, leader_name, location_name, objective, description,
    status, visibility, source, created_by, updated_by, trip_category, meta
  ) values (
    v_a.title,
    v_a.start_date,
    coalesce(v_a.end_date, v_a.start_date),
    v_a.leader_name,
    v_a.location_name,
    v_a.trip_category,
    coalesce(v_a.description,'') || E'\n\nIzvor: ' || coalesce(v_a.source_document_title,'zapisnik sastanka'),
    'planned',
    'members',
    'meeting_minutes_announcement',
    v_user,
    v_user,
    v_a.trip_category,
    jsonb_build_object(
      'meeting_minutes_id', v_a.meeting_minutes_id,
      'announcement_id', v_a.id,
      'source_meeting_date', v_a.source_meeting_date,
      'raw_announcement', v_a.raw_text
    )
  ) returning id into v_trip_id;

  update public.trip_announcements_staging
  set status='odobreno', created_trip_id=v_trip_id, reviewed_by=v_user, approved_at=now(), updated_at=now()
  where id=p_announcement_id;

  return v_trip_id;
end;
$$;

grant select, insert, update, delete on public.meeting_minutes to authenticated;
grant select, insert, update, delete on public.trip_announcements_staging to authenticated;
grant execute on function public.sov_approve_trip_announcement(uuid) to authenticated;

commit;
