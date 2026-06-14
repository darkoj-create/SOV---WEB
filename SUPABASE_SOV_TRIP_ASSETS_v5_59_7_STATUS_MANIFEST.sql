-- SOV Trip Assets v5.59.7 — Status / manifest / checksum
-- Extends v5.59.6. Run this after previous SOV tracking/trips SQL.
-- Purpose: web Trip Assets Manager + APK offline-ready status.

create extension if not exists pgcrypto;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'sov-trip-assets',
  'sov-trip-assets',
  false,
  2147483648,
  array['application/zip','application/octet-stream','application/x-zip-compressed','application/vnd.google-earth.kml+xml','application/gpx+xml','text/xml','application/json','application/geo+json']::text[]
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create table if not exists public.sov_trip_assets (
  id uuid primary key default gen_random_uuid(),
  trip_id uuid not null references public.sov_trips(id) on delete cascade,
  uploader_id uuid default auth.uid(),
  asset_type text not null default 'sovpkg' check (asset_type in ('sovpkg','offline_map','gpx','kml','topodroid','other')),
  title text not null default 'Paket izleta',
  description text not null default '',
  storage_bucket text not null default 'sov-trip-assets',
  storage_path text not null unique,
  original_filename text not null default '',
  content_type text not null default 'application/zip',
  size_bytes bigint not null default 0,
  metadata jsonb not null default '{}'::jsonb,
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  expires_at timestamptz,
  deleted_at timestamptz
);

alter table public.sov_trip_assets add column if not exists checksum_sha256 text;
alter table public.sov_trip_assets add column if not exists package_version integer not null default 1;
alter table public.sov_trip_assets add column if not exists download_count integer not null default 0;
alter table public.sov_trip_assets add column if not exists last_download_at timestamptz;
alter table public.sov_trip_assets add column if not exists updated_at timestamptz not null default now();

create index if not exists idx_sov_trip_assets_trip_active on public.sov_trip_assets(trip_id, is_active, expires_at, created_at desc);
create index if not exists idx_sov_trip_assets_expires on public.sov_trip_assets(expires_at) where is_active;
create index if not exists idx_sov_trip_assets_checksum on public.sov_trip_assets(trip_id, checksum_sha256) where checksum_sha256 is not null;

alter table public.sov_trip_assets enable row level security;

drop policy if exists sov_trip_assets_read_auth on public.sov_trip_assets;
drop policy if exists sov_trip_assets_insert_own on public.sov_trip_assets;
drop policy if exists sov_trip_assets_update_own on public.sov_trip_assets;

create policy sov_trip_assets_read_auth
on public.sov_trip_assets
for select
to authenticated
using (
  is_active = true
  and deleted_at is null
  and (expires_at is null or expires_at > now())
);

create policy sov_trip_assets_insert_own
on public.sov_trip_assets
for insert
to authenticated
with check (uploader_id = auth.uid());

create policy sov_trip_assets_update_own
on public.sov_trip_assets
for update
to authenticated
using (uploader_id = auth.uid())
with check (uploader_id = auth.uid());

-- Storage policies: private bucket. Authenticated SOV users can read/write.
drop policy if exists sov_trip_assets_storage_read_auth on storage.objects;
drop policy if exists sov_trip_assets_storage_insert_auth on storage.objects;
drop policy if exists sov_trip_assets_storage_update_auth on storage.objects;
drop policy if exists sov_trip_assets_storage_delete_auth on storage.objects;

create policy sov_trip_assets_storage_read_auth
on storage.objects
for select
to authenticated
using (bucket_id = 'sov-trip-assets');

create policy sov_trip_assets_storage_insert_auth
on storage.objects
for insert
to authenticated
with check (bucket_id = 'sov-trip-assets');

create policy sov_trip_assets_storage_update_auth
on storage.objects
for update
to authenticated
using (bucket_id = 'sov-trip-assets')
with check (bucket_id = 'sov-trip-assets');

create policy sov_trip_assets_storage_delete_auth
on storage.objects
for delete
to authenticated
using (bucket_id = 'sov-trip-assets');

-- Recreate view safely because old view shape changed across builds.
drop view if exists public.sov_trip_assets_active;
create view public.sov_trip_assets_active as
select
  a.id,
  a.trip_id,
  a.asset_type,
  a.title,
  a.description,
  a.storage_bucket,
  a.storage_path,
  a.original_filename,
  a.content_type,
  a.size_bytes,
  a.checksum_sha256,
  a.package_version,
  a.download_count,
  a.last_download_at,
  a.metadata,
  a.created_at,
  a.updated_at,
  a.expires_at,
  t.location_name,
  t.start_date,
  t.end_date,
  t.status as trip_status
from public.sov_trip_assets a
join public.sov_trips t on t.id = a.trip_id
where a.is_active = true
  and a.deleted_at is null
  and (a.expires_at is null or a.expires_at > now())
  and coalesce(t.status::text, '') not in ('cancelled','deleted','archived');

create or replace function public.sov_trip_asset_register_v2(
  p_trip_id uuid,
  p_asset_type text,
  p_title text,
  p_description text,
  p_storage_path text,
  p_original_filename text,
  p_content_type text,
  p_size_bytes bigint,
  p_checksum_sha256 text default null,
  p_metadata jsonb default '{}'::jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = public, storage
as $$
declare
  v_asset public.sov_trip_assets%rowtype;
  v_expires_at timestamptz;
  v_next_version integer;
begin
  if auth.uid() is null then
    raise exception 'Nisi prijavljen u SOV Cloud.';
  end if;

  if p_trip_id is null then
    raise exception 'Nedostaje trip_id.';
  end if;

  select
    case
      when end_date is not null then (end_date::date + interval '1 day')::timestamptz
      else null
    end
  into v_expires_at
  from public.sov_trips
  where id = p_trip_id;

  if not found then
    raise exception 'Izlet ne postoji.';
  end if;

  select coalesce(max(package_version),0)+1 into v_next_version
  from public.sov_trip_assets
  where trip_id = p_trip_id;

  insert into public.sov_trip_assets(
    trip_id,
    uploader_id,
    asset_type,
    title,
    description,
    storage_bucket,
    storage_path,
    original_filename,
    content_type,
    size_bytes,
    checksum_sha256,
    package_version,
    metadata,
    expires_at,
    updated_at
  ) values (
    p_trip_id,
    auth.uid(),
    coalesce(nullif(p_asset_type,''), 'sovpkg'),
    coalesce(nullif(p_title,''), 'Paket izleta'),
    coalesce(p_description, ''),
    'sov-trip-assets',
    p_storage_path,
    coalesce(p_original_filename, ''),
    coalesce(nullif(p_content_type,''), 'application/zip'),
    greatest(coalesce(p_size_bytes,0),0),
    nullif(p_checksum_sha256,''),
    v_next_version,
    coalesce(p_metadata, '{}'::jsonb) || jsonb_build_object('checksum_sha256', nullif(p_checksum_sha256,''), 'offline_ready', true),
    v_expires_at,
    now()
  )
  on conflict (storage_path) do update set
    title = excluded.title,
    description = excluded.description,
    size_bytes = excluded.size_bytes,
    checksum_sha256 = excluded.checksum_sha256,
    metadata = excluded.metadata,
    is_active = true,
    deleted_at = null,
    expires_at = excluded.expires_at,
    updated_at = now()
  returning * into v_asset;

  if exists (select 1 from information_schema.columns where table_schema='public' and table_name='sov_trips' and column_name='meta') then
    execute 'update public.sov_trips set meta = coalesce(meta, ''{}''::jsonb) || jsonb_build_object(''hasTripAssets'', true, ''lastTripAssetAt'', now(), ''lastTripAssetTitle'', $1) where id = $2'
    using v_asset.title, p_trip_id;
  end if;

  return to_jsonb(v_asset);
end;
$$;

-- Legacy signature kept for APK/web fallback.
create or replace function public.sov_trip_asset_register(
  p_trip_id uuid,
  p_asset_type text,
  p_title text,
  p_description text,
  p_storage_path text,
  p_original_filename text,
  p_content_type text,
  p_size_bytes bigint,
  p_metadata jsonb default '{}'::jsonb
)
returns jsonb
language sql
security definer
set search_path = public
as $$
  select public.sov_trip_asset_register_v2(
    p_trip_id,
    p_asset_type,
    p_title,
    p_description,
    p_storage_path,
    p_original_filename,
    p_content_type,
    p_size_bytes,
    nullif(p_metadata->>'checksum_sha256',''),
    p_metadata
  );
$$;

create or replace function public.sov_trip_assets_for_trip(p_trip_id uuid)
returns setof public.sov_trip_assets_active
language sql
security definer
set search_path = public
as $$
  select *
  from public.sov_trip_assets_active
  where trip_id = p_trip_id
  order by created_at desc;
$$;

create or replace function public.sov_trip_asset_mark_downloaded(p_asset_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_asset public.sov_trip_assets%rowtype;
begin
  update public.sov_trip_assets
  set download_count = coalesce(download_count,0) + 1,
      last_download_at = now(),
      updated_at = now()
  where id = p_asset_id
    and is_active = true
    and deleted_at is null
  returning * into v_asset;
  if not found then
    raise exception 'Paket nije pronađen.';
  end if;
  return jsonb_build_object('ok', true, 'asset_id', v_asset.id, 'download_count', v_asset.download_count);
end;
$$;

create or replace function public.sov_trip_assets_cleanup_expired()
returns jsonb
language plpgsql
security definer
set search_path = public, storage
as $$
declare
  v_deleted_objects int := 0;
  v_deleted_rows int := 0;
begin
  with expired as (
    select id, storage_path
    from public.sov_trip_assets
    where deleted_at is null
      and is_active = true
      and expires_at is not null
      and expires_at <= now()
  ), deleted_storage as (
    delete from storage.objects o
    using expired e
    where o.bucket_id = 'sov-trip-assets'
      and o.name = e.storage_path
    returning o.id
  ), updated_assets as (
    update public.sov_trip_assets a
    set is_active = false,
        deleted_at = now(),
        updated_at = now()
    from expired e
    where a.id = e.id
    returning a.id
  )
  select
    (select count(*) from deleted_storage),
    (select count(*) from updated_assets)
  into v_deleted_objects, v_deleted_rows;

  return jsonb_build_object('ok', true, 'deleted_storage_objects', v_deleted_objects, 'deleted_asset_rows', v_deleted_rows);
end;
$$;

drop view if exists public.sov_trip_assets_sync_status;
create view public.sov_trip_assets_sync_status as
select
  count(*)::int as assets_total,
  count(*) filter (where is_active and deleted_at is null and (expires_at is null or expires_at > now()))::int as assets_active,
  count(distinct trip_id)::int as trips_with_assets,
  coalesce(sum(size_bytes),0)::bigint as bytes_total,
  max(created_at) as last_asset_at
from public.sov_trip_assets;

grant select on public.sov_trip_assets_active to authenticated;
grant select on public.sov_trip_assets_sync_status to authenticated;
grant execute on function public.sov_trip_asset_register_v2(uuid,text,text,text,text,text,text,bigint,text,jsonb) to authenticated;
grant execute on function public.sov_trip_asset_register(uuid,text,text,text,text,text,text,bigint,jsonb) to authenticated;
grant execute on function public.sov_trip_assets_for_trip(uuid) to authenticated;
grant execute on function public.sov_trip_asset_mark_downloaded(uuid) to authenticated;
grant execute on function public.sov_trip_assets_cleanup_expired() to authenticated;
