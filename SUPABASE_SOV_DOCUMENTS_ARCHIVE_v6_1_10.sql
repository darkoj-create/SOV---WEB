-- SOV DOCUMENTS / ZAPISNICI FULL ARCHIVE v6.1.10
-- Purpose: prepare the database/storage structure for the complete meeting-minutes archive 1960-2026.
-- Keep actual files in Supabase Storage; keep SQL only for metadata, filters and search.

begin;

-- Private Storage bucket for documents. Files are opened through signed URLs.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'sov-documents',
  'sov-documents',
  false,
  52428800,
  array[
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'image/jpeg',
    'image/png',
    'image/webp',
    'text/plain'
  ]::text[]
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create table if not exists public.sov_document_archive (
  id uuid primary key default gen_random_uuid(),
  collection text not null default 'zapisnici_sastanaka',
  title text not null,
  document_type text not null default 'zapisnik sastanka',
  document_date date,
  year int not null check (year between 1900 and 2100),
  month int check (month is null or month between 0 and 12),
  day int check (day is null or day between 1 and 31),
  meeting_number text,
  original_filename text,
  storage_bucket text not null default 'sov-documents',
  storage_path text not null,
  mime_type text,
  format text,
  size_bytes bigint not null default 0 check (size_bytes >= 0),
  checksum_sha256 text,
  source_batch text,
  tags text[] not null default '{}'::text[],
  summary text,
  ocr_text text,
  status text not null default 'active' check (status in ('active','draft','hidden','archived')),
  visibility text not null default 'members' check (visibility in ('members','public','staff')),
  imported_by uuid,
  imported_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint sov_document_archive_storage_unique unique(storage_bucket, storage_path)
);

create index if not exists sov_document_archive_collection_year_idx on public.sov_document_archive(collection, year desc, month desc);
create index if not exists sov_document_archive_type_idx on public.sov_document_archive(document_type);
create index if not exists sov_document_archive_status_idx on public.sov_document_archive(status);
create index if not exists sov_document_archive_date_idx on public.sov_document_archive(document_date desc nulls last);
create index if not exists sov_document_archive_search_idx on public.sov_document_archive using gin (
  to_tsvector('simple', coalesce(title,'') || ' ' || coalesce(original_filename,'') || ' ' || coalesce(summary,'') || ' ' || coalesce(ocr_text,''))
);

create or replace function public.sov_documents_touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_sov_document_archive_touch on public.sov_document_archive;
create trigger trg_sov_document_archive_touch
before update on public.sov_document_archive
for each row execute function public.sov_documents_touch_updated_at();

create or replace function public.sov_documents_is_staff()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(
    auth.uid() is not null and (
      exists (
        select 1
        from public.profiles p
        where p.id = auth.uid()
          and coalesce(p.status::text,'approved') = 'approved'
          and coalesce(p.role::text,'user') in ('webmaster','admin','arhivar')
      )
      or lower(coalesce(auth.jwt()->>'email','')) = 'darko.jeras@gmail.com'
    ), false
  );
$$;

alter table public.sov_document_archive enable row level security;

drop policy if exists "sov document archive members read active" on public.sov_document_archive;
drop policy if exists "sov document archive staff insert" on public.sov_document_archive;
drop policy if exists "sov document archive staff update" on public.sov_document_archive;
drop policy if exists "sov document archive staff delete" on public.sov_document_archive;

create policy "sov document archive members read active"
  on public.sov_document_archive for select
  using (
    auth.role() = 'authenticated'
    and (
      (status = 'active' and visibility in ('members','public'))
      or public.sov_documents_is_staff()
    )
  );

create policy "sov document archive staff insert"
  on public.sov_document_archive for insert
  with check (public.sov_documents_is_staff());

create policy "sov document archive staff update"
  on public.sov_document_archive for update
  using (public.sov_documents_is_staff())
  with check (public.sov_documents_is_staff());

create policy "sov document archive staff delete"
  on public.sov_document_archive for delete
  using (public.sov_documents_is_staff());

-- Storage policies. The bucket is private; authenticated members can read through signed URLs.
drop policy if exists "sov documents authenticated read" on storage.objects;
drop policy if exists "sov documents staff insert" on storage.objects;
drop policy if exists "sov documents staff update" on storage.objects;
drop policy if exists "sov documents staff delete" on storage.objects;

create policy "sov documents authenticated read"
  on storage.objects for select
  using (bucket_id = 'sov-documents' and auth.role() = 'authenticated');

create policy "sov documents staff insert"
  on storage.objects for insert
  with check (bucket_id = 'sov-documents' and public.sov_documents_is_staff());

create policy "sov documents staff update"
  on storage.objects for update
  using (bucket_id = 'sov-documents' and public.sov_documents_is_staff())
  with check (bucket_id = 'sov-documents' and public.sov_documents_is_staff());

create policy "sov documents staff delete"
  on storage.objects for delete
  using (bucket_id = 'sov-documents' and public.sov_documents_is_staff());

create or replace view public.sov_minutes_archive_public
with (security_invoker = true) as
select
  id,
  collection,
  title,
  document_type,
  document_date,
  year,
  month,
  day,
  meeting_number,
  original_filename,
  storage_bucket,
  storage_path,
  mime_type,
  upper(coalesce(format, split_part(coalesce(original_filename, storage_path), '.', array_length(string_to_array(coalesce(original_filename, storage_path), '.'), 1)))) as format,
  size_bytes,
  case
    when size_bytes >= 1073741824 then round(size_bytes::numeric / 1073741824, 1)::text || ' GB'
    when size_bytes >= 1048576 then round(size_bytes::numeric / 1048576, 1)::text || ' MB'
    when size_bytes >= 1024 then round(size_bytes::numeric / 1024, 0)::text || ' KB'
    else size_bytes::text || ' B'
  end as size_label,
  source_batch,
  tags,
  summary,
  status,
  visibility,
  imported_at,
  updated_at
from public.sov_document_archive
where collection = 'zapisnici_sastanaka';

create or replace view public.sov_minutes_archive_year_stats
with (security_invoker = true) as
select
  year,
  count(*)::int as count,
  coalesce(sum(size_bytes),0)::bigint as total_bytes,
  case
    when coalesce(sum(size_bytes),0) >= 1073741824 then round(coalesce(sum(size_bytes),0)::numeric / 1073741824, 1)::text || ' GB'
    when coalesce(sum(size_bytes),0) >= 1048576 then round(coalesce(sum(size_bytes),0)::numeric / 1048576, 1)::text || ' MB'
    when coalesce(sum(size_bytes),0) >= 1024 then round(coalesce(sum(size_bytes),0)::numeric / 1024, 0)::text || ' KB'
    else coalesce(sum(size_bytes),0)::text || ' B'
  end as total_size_label,
  count(*) filter (where document_type = 'zapisnik sastanka')::int as regular_minutes,
  count(*) filter (where size_bytes >= 10485760)::int as large_files
from public.sov_minutes_archive_public
where status = 'active'
group by year
order by year desc;

create or replace function public.sov_search_minutes_archive(
  p_query text default '',
  p_year int default null,
  p_type text default null,
  p_limit int default 300,
  p_offset int default 0
)
returns table (
  id uuid,
  title text,
  document_type text,
  document_date date,
  year int,
  month int,
  day int,
  original_filename text,
  storage_bucket text,
  storage_path text,
  mime_type text,
  format text,
  size_bytes bigint,
  size_label text,
  source_batch text,
  tags text[],
  summary text,
  status text,
  visibility text,
  imported_at timestamptz,
  updated_at timestamptz
)
language sql
stable
as $$
  select
    v.id,
    v.title,
    v.document_type,
    v.document_date,
    v.year,
    v.month,
    v.day,
    v.original_filename,
    v.storage_bucket,
    v.storage_path,
    v.mime_type,
    v.format,
    v.size_bytes,
    v.size_label,
    v.source_batch,
    v.tags,
    v.summary,
    v.status,
    v.visibility,
    v.imported_at,
    v.updated_at
  from public.sov_minutes_archive_public v
  where v.status = 'active'
    and (p_year is null or v.year = p_year)
    and (p_type is null or p_type = '' or p_type = 'all' or v.document_type = p_type)
    and (
      coalesce(trim(p_query),'') = ''
      or to_tsvector('simple', coalesce(v.title,'') || ' ' || coalesce(v.original_filename,'') || ' ' || coalesce(v.summary,'')) @@ plainto_tsquery('simple', p_query)
      or v.title ilike '%' || p_query || '%'
      or v.original_filename ilike '%' || p_query || '%'
      or v.summary ilike '%' || p_query || '%'
    )
  order by coalesce(v.document_date, make_date(v.year, greatest(coalesce(v.month,1),1), 1)) desc nulls last, v.title asc
  limit least(greatest(coalesce(p_limit,300),1),500)
  offset greatest(coalesce(p_offset,0),0);
$$;

grant usage on schema public to anon, authenticated;
grant select on public.sov_minutes_archive_public to authenticated;
grant select on public.sov_minutes_archive_year_stats to authenticated;
grant select, insert, update, delete on public.sov_document_archive to authenticated;
grant execute on function public.sov_search_minutes_archive(text,int,text,int,int) to authenticated;
grant execute on function public.sov_documents_is_staff() to authenticated;

commit;
