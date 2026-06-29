-- SOV v6.1.44e — Gmail zapisnici kao native tekst, bez DOCX uploada
-- Pokrenuti u Supabase SQL editoru. Ne dira postojeće Izlete/Oružarstvo/Arhivar.

create extension if not exists pgcrypto;

create table if not exists public.meeting_minutes (
  id uuid primary key default gen_random_uuid(),
  document_id uuid,
  title text not null default 'Zapisnik sastanka',
  meeting_date date,
  original_filename text,
  checksum_sha256 text,
  plain_text text not null default '',
  announcements_text text,
  source text not null default 'manual_native',
  status text not null default 'parsed',
  gmail_message_id text,
  gmail_thread_id text,
  from_email text,
  storage_path text,
  rendered_html text,
  meta jsonb not null default '{}'::jsonb,
  parsed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists meeting_minutes_checksum_uidx on public.meeting_minutes(checksum_sha256) where checksum_sha256 is not null;
create index if not exists meeting_minutes_date_idx on public.meeting_minutes(meeting_date desc nulls last);
create index if not exists meeting_minutes_source_idx on public.meeting_minutes(source);

alter table public.meeting_minutes enable row level security;

drop policy if exists meeting_minutes_select_authenticated on public.meeting_minutes;
create policy meeting_minutes_select_authenticated on public.meeting_minutes for select to authenticated using (true);

drop policy if exists meeting_minutes_insert_authenticated on public.meeting_minutes;
create policy meeting_minutes_insert_authenticated on public.meeting_minutes for insert to authenticated with check (true);

drop policy if exists meeting_minutes_update_authenticated on public.meeting_minutes;
create policy meeting_minutes_update_authenticated on public.meeting_minutes for update to authenticated using (true) with check (true);

create table if not exists public.sov_gmail_minutes_import_log (
  id bigserial primary key,
  created_at timestamptz not null default now(),
  gmail_message_id text,
  gmail_thread_id text,
  subject text,
  from_email text,
  attachment_name text,
  checksum_sha256 text,
  meeting_minutes_id uuid references public.meeting_minutes(id) on delete set null,
  status text not null,
  message text,
  meta jsonb not null default '{}'::jsonb
);

alter table public.sov_gmail_minutes_import_log enable row level security;

drop policy if exists gmail_log_select_authenticated on public.sov_gmail_minutes_import_log;
create policy gmail_log_select_authenticated on public.sov_gmail_minutes_import_log for select to authenticated using (true);

create table if not exists public.sov_gmail_sync_requests (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  requested_by uuid,
  requested_days int not null default 28,
  status text not null default 'queued',
  started_at timestamptz,
  finished_at timestamptz,
  imported_count int not null default 0,
  skipped_count int not null default 0,
  failed_count int not null default 0,
  result_message text,
  meta jsonb not null default '{}'::jsonb
);

alter table public.sov_gmail_sync_requests enable row level security;

drop policy if exists gmail_sync_select_authenticated on public.sov_gmail_sync_requests;
create policy gmail_sync_select_authenticated on public.sov_gmail_sync_requests for select to authenticated using (true);

drop policy if exists gmail_sync_insert_authenticated on public.sov_gmail_sync_requests;
create policy gmail_sync_insert_authenticated on public.sov_gmail_sync_requests for insert to authenticated with check (true);

create or replace function public.sov_extract_minutes_date(p_attachment_name text, p_subject text default null)
returns date language plpgsql immutable as $$
declare
  s text := coalesce(p_attachment_name,'') || ' ' || coalesce(p_subject,'');
  m text[];
begin
  m := regexp_match(s, '(20[0-9]{2})[-_. ]([01]?[0-9])[-_. ]([0-3]?[0-9])');
  if m is not null then return make_date(m[1]::int, m[2]::int, m[3]::int); end if;
  m := regexp_match(s, '([0-3]?[0-9])[-_. ]([01]?[0-9])[-_. ](20[0-9]{2})');
  if m is not null then return make_date(m[3]::int, m[2]::int, m[1]::int); end if;
  return null;
exception when others then
  return null;
end $$;

create or replace function public.sov_extract_announcements_text(p_plain_text text)
returns text language plpgsql immutable as $$
declare
  t text := coalesce(p_plain_text,'');
  start_pos int;
  body text;
  stop_pos int;
begin
  start_pos := position('NAJAVE' in upper(t));
  if start_pos <= 0 then return null; end if;
  body := substring(t from start_pos);
  stop_pos := nullif(least(
    nullif(position(E'\nRAZNO' in upper(body)),0),
    nullif(position(E'\nSASTANAK VODIO' in upper(body)),0),
    nullif(position(E'\nSASTANAK VODILA' in upper(body)),0),
    nullif(position(E'\nZAPISNI' in upper(body)),0)
  ),0);
  if stop_pos is not null and stop_pos > 1 then body := substring(body from 1 for stop_pos-1); end if;
  return btrim(body);
end $$;

create or replace function public.sov_ingest_meeting_minutes_from_gmail(
  p_ingest_key text,
  p_gmail_message_id text,
  p_gmail_thread_id text,
  p_subject text,
  p_from_email text,
  p_attachment_name text,
  p_checksum_sha256 text,
  p_plain_text text,
  p_storage_path text default null
) returns jsonb
language plpgsql
security definer
set search_path=public
as $$
declare
  v_expected text := 'SOV_GMAIL_ZAPISNICI_2026_CHANGE_ME';
  v_existing public.meeting_minutes%rowtype;
  v_minutes_id uuid;
  v_date date;
  v_title text;
  v_ann text;
begin
  if p_ingest_key is distinct from v_expected then
    raise exception 'Invalid ingest key';
  end if;
  if coalesce(length(btrim(p_plain_text)),0) < 20 then
    raise exception 'DOCX text is empty or too short';
  end if;

  select * into v_existing from public.meeting_minutes where checksum_sha256 = p_checksum_sha256 limit 1;
  if found then
    insert into public.sov_gmail_minutes_import_log(gmail_message_id,gmail_thread_id,subject,from_email,attachment_name,checksum_sha256,meeting_minutes_id,status,message)
    values(p_gmail_message_id,p_gmail_thread_id,p_subject,p_from_email,p_attachment_name,p_checksum_sha256,v_existing.id,'skipped','Već postoji zapisnik s istim checksumom.');
    return jsonb_build_object('ok',true,'status','skipped','meeting_minutes_id',v_existing.id);
  end if;

  v_date := public.sov_extract_minutes_date(p_attachment_name, p_subject);
  v_title := case when v_date is not null then 'Zapisnik sastanka ' || to_char(v_date,'DD.MM.YYYY.') else coalesce(nullif(p_subject,''),'Zapisnik sastanka') end;
  v_ann := public.sov_extract_announcements_text(p_plain_text);

  insert into public.meeting_minutes(title,meeting_date,original_filename,checksum_sha256,plain_text,announcements_text,source,status,gmail_message_id,gmail_thread_id,from_email,storage_path,parsed_at,meta)
  values(v_title,v_date,p_attachment_name,p_checksum_sha256,p_plain_text,v_ann,'gmail','parsed',p_gmail_message_id,p_gmail_thread_id,p_from_email,null,now(),jsonb_build_object('subject',p_subject,'native_text_only',true,'note','DOCX file was not uploaded; only extracted text was stored.'))
  returning id into v_minutes_id;

  insert into public.sov_gmail_minutes_import_log(gmail_message_id,gmail_thread_id,subject,from_email,attachment_name,checksum_sha256,meeting_minutes_id,status,message)
  values(p_gmail_message_id,p_gmail_thread_id,p_subject,p_from_email,p_attachment_name,p_checksum_sha256,v_minutes_id,'imported','Uvezen native tekst zapisnika bez DOCX uploada.');

  return jsonb_build_object('ok',true,'status','imported','meeting_minutes_id',v_minutes_id);
end $$;

grant execute on function public.sov_ingest_meeting_minutes_from_gmail(text,text,text,text,text,text,text,text,text) to anon, authenticated;

create or replace function public.sov_claim_gmail_sync_request(p_ingest_key text)
returns jsonb language plpgsql security definer set search_path=public as $$
declare
  v_expected text := 'SOV_GMAIL_ZAPISNICI_2026_CHANGE_ME';
  r public.sov_gmail_sync_requests%rowtype;
begin
  if p_ingest_key is distinct from v_expected then raise exception 'Invalid ingest key'; end if;
  select * into r from public.sov_gmail_sync_requests where status='queued' order by created_at asc limit 1 for update skip locked;
  if not found then return jsonb_build_object('ok',true,'request',null); end if;
  update public.sov_gmail_sync_requests set status='processing', started_at=now(), updated_at=now() where id=r.id returning * into r;
  return jsonb_build_object('ok',true,'request',jsonb_build_object('id',r.id,'requested_days',r.requested_days));
end $$;

grant execute on function public.sov_claim_gmail_sync_request(text) to anon, authenticated;

create or replace function public.sov_finish_gmail_sync_request(
  p_ingest_key text,
  p_request_id uuid,
  p_status text,
  p_imported_count int default 0,
  p_skipped_count int default 0,
  p_failed_count int default 0,
  p_result_message text default null,
  p_meta jsonb default '{}'::jsonb
) returns jsonb language plpgsql security definer set search_path=public as $$
declare v_expected text := 'SOV_GMAIL_ZAPISNICI_2026_CHANGE_ME';
begin
  if p_ingest_key is distinct from v_expected then raise exception 'Invalid ingest key'; end if;
  update public.sov_gmail_sync_requests set status=coalesce(p_status,'completed'), finished_at=now(), updated_at=now(), imported_count=coalesce(p_imported_count,0), skipped_count=coalesce(p_skipped_count,0), failed_count=coalesce(p_failed_count,0), result_message=p_result_message, meta=coalesce(p_meta,'{}'::jsonb) where id=p_request_id;
  return jsonb_build_object('ok',true,'id',p_request_id);
end $$;

grant execute on function public.sov_finish_gmail_sync_request(text,uuid,text,int,int,int,text,jsonb) to anon, authenticated;
