-- SOV web v5.58.4 — News CMS / profiles RLS recursion fix
-- Pokreni POVRH v5.58.3.
-- Popravlja: "infinite recursion detected in policy for relation profiles"
-- Uzrok: profiles RLS policy zove helper koji opet cita profiles.
-- Rjesenje: role helperi citaju profiles kao SECURITY DEFINER + row_security=off,
-- a sov_news/storage politike koriste te helper funkcije bez direktnog dodira profiles RLS-a.

create extension if not exists pgcrypto;
create extension if not exists unaccent;

-- -------------------------------------------------------------
-- 1) Canonical role helpers — non-recursive, RLS-safe
-- -------------------------------------------------------------

create or replace function public.sov_current_role()
returns text
language plpgsql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_uid uuid := auth.uid();
  v_role text;
  v_jwt jsonb := auth.jwt();
  v_id_col text;
begin
  if v_uid is null then
    return 'anon';
  end if;

  -- First try JWT claims, if the project ever stores role there.
  v_role := lower(nullif(trim(coalesce(
    v_jwt -> 'app_metadata' ->> 'role',
    v_jwt -> 'user_metadata' ->> 'role',
    v_jwt ->> 'role'
  )), ''));
  if v_role is not null then
    return v_role;
  end if;

  -- Then profiles, but with row_security off inside this definer function.
  if to_regclass('public.profiles') is not null
     and exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='role') then

    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then
      v_id_col := 'id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then
      v_id_col := 'user_id';
    else
      v_id_col := null;
    end if;

    if v_id_col is not null then
      execute format('select lower(coalesce(role::text,'''')) from public.profiles where %I = $1 limit 1', v_id_col)
        into v_role
        using v_uid;
      if v_role is not null and length(trim(v_role)) > 0 then
        return lower(trim(v_role));
      end if;
    end if;
  end if;

  return 'user';
exception when others then
  -- Never let a role helper break public/news reads.
  return 'user';
end;
$$;

create or replace function public.sov_current_status()
returns text
language plpgsql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_uid uuid := auth.uid();
  v_status text;
  v_jwt jsonb := auth.jwt();
  v_id_col text;
begin
  if v_uid is null then
    return 'anon';
  end if;

  v_status := lower(nullif(trim(coalesce(
    v_jwt -> 'app_metadata' ->> 'status',
    v_jwt -> 'user_metadata' ->> 'status',
    v_jwt ->> 'status'
  )), ''));
  if v_status is not null then
    return v_status;
  end if;

  if to_regclass('public.profiles') is not null
     and exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='status') then

    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then
      v_id_col := 'id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then
      v_id_col := 'user_id';
    else
      v_id_col := null;
    end if;

    if v_id_col is not null then
      execute format('select lower(coalesce(status::text,'''')) from public.profiles where %I = $1 limit 1', v_id_col)
        into v_status
        using v_uid;
      if v_status is not null and length(trim(v_status)) > 0 then
        return lower(trim(v_status));
      end if;
    end if;
  end if;

  -- If the old schema has no status column, do not block legitimate logged-in users by default.
  return 'approved';
exception when others then
  return 'approved';
end;
$$;

create or replace function public.sov_is_admin()
returns boolean
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
  select public.sov_current_role() = 'admin'
     and public.sov_current_status() in ('approved','active','enabled','ok');
$$;

create or replace function public.sov_has_permission(permission_name text)
returns boolean
language plpgsql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_role text := public.sov_current_role();
  v_status text := public.sov_current_status();
  v_allowed boolean := false;
begin
  if auth.uid() is null then
    return false;
  end if;

  if v_status not in ('approved','active','enabled','ok') then
    return false;
  end if;

  if v_role = 'admin' then
    return true;
  end if;

  if to_regclass('public.sov_role_permissions') is not null then
    execute format(
      'select coalesce(%I,false) from public.sov_role_permissions where role = $1 limit 1',
      case permission_name
        when 'view_sov_base' then 'can_view_sov_base'
        when 'view_katastar' then 'can_view_katastar'
        when 'edit_objects' then 'can_edit_objects'
        when 'upload_drawings' then 'can_upload_drawings'
        when 'verify_drawings' then 'can_verify_drawings'
        when 'manage_trips' then 'can_manage_trips'
        when 'manage_equipment' then 'can_manage_equipment'
        when 'edit_news' then 'can_edit_news'
        when 'use_sql_tools' then 'can_use_sql_tools'
        when 'manage_users' then 'can_manage_users'
        else 'can_view_sov_base'
      end
    ) into v_allowed using v_role;
  end if;

  -- Hard fallback for news editor role names, even if sov_role_permissions is absent/incomplete.
  if permission_name = 'edit_news' and v_role in ('editor','urednik') then
    return true;
  end if;

  return coalesce(v_allowed,false);
exception when others then
  if permission_name = 'edit_news' and v_role in ('admin','editor','urednik') then
    return true;
  end if;
  return false;
end;
$$;

create or replace function public.sov_can_edit_news()
returns boolean
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
  select auth.uid() is not null
     and public.sov_current_status() in ('approved','active','enabled','ok')
     and (
       public.sov_current_role() in ('admin','editor','urednik')
       or public.sov_has_permission('edit_news')
       or public.sov_has_permission('manage_users')
     );
$$;

grant execute on function public.sov_current_role() to anon, authenticated;
grant execute on function public.sov_current_status() to anon, authenticated;
grant execute on function public.sov_is_admin() to anon, authenticated;
grant execute on function public.sov_has_permission(text) to anon, authenticated;
grant execute on function public.sov_can_edit_news() to anon, authenticated;

-- -------------------------------------------------------------
-- 2) Replace the known recursive profiles policies from older role builds
-- -------------------------------------------------------------

do $$
begin
  if to_regclass('public.profiles') is not null then
    execute 'alter table public.profiles enable row level security';

    -- Drop the known problematic policy names from v5.33/v5.34 family.
    execute 'drop policy if exists "profiles self read" on public.profiles';
    execute 'drop policy if exists "profiles self insert" on public.profiles';
    execute 'drop policy if exists "profiles self update basic" on public.profiles';
    execute 'drop policy if exists "profiles admin read all" on public.profiles';
    execute 'drop policy if exists "profiles admin update all" on public.profiles';

    execute 'drop policy if exists "profiles self read safe v5584" on public.profiles';
    execute 'drop policy if exists "profiles self insert safe v5584" on public.profiles';
    execute 'drop policy if exists "profiles self update safe v5584" on public.profiles';
    execute 'drop policy if exists "profiles admin read safe v5584" on public.profiles';
    execute 'drop policy if exists "profiles admin update safe v5584" on public.profiles';

    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then
      execute 'create policy "profiles self read safe v5584" on public.profiles for select using (id = auth.uid() or public.sov_is_admin())';
      execute 'create policy "profiles self insert safe v5584" on public.profiles for insert with check (id = auth.uid())';
      execute 'create policy "profiles self update safe v5584" on public.profiles for update using (id = auth.uid() or public.sov_is_admin()) with check (id = auth.uid() or public.sov_is_admin())';
      execute 'create policy "profiles admin read safe v5584" on public.profiles for select using (public.sov_is_admin())';
      execute 'create policy "profiles admin update safe v5584" on public.profiles for update using (public.sov_is_admin()) with check (public.sov_is_admin())';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then
      execute 'create policy "profiles self read safe v5584" on public.profiles for select using (user_id = auth.uid() or public.sov_is_admin())';
      execute 'create policy "profiles self insert safe v5584" on public.profiles for insert with check (user_id = auth.uid())';
      execute 'create policy "profiles self update safe v5584" on public.profiles for update using (user_id = auth.uid() or public.sov_is_admin()) with check (user_id = auth.uid() or public.sov_is_admin())';
      execute 'create policy "profiles admin read safe v5584" on public.profiles for select using (public.sov_is_admin())';
      execute 'create policy "profiles admin update safe v5584" on public.profiles for update using (public.sov_is_admin()) with check (public.sov_is_admin())';
    end if;
  end if;
end $$;

-- -------------------------------------------------------------
-- 3) Recreate news table/policies safely
-- -------------------------------------------------------------

create table if not exists public.sov_news (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  summary text,
  body text,
  image_url text,
  pdf_url text,
  cta_label text,
  cta_url text,
  published boolean not null default true,
  pinned boolean not null default false,
  published_at timestamptz default now(),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  created_by uuid,
  updated_by uuid
);

alter table public.sov_news add column if not exists slug text;
alter table public.sov_news add column if not exists category text default 'Novosti';
alter table public.sov_news add column if not exists author_name text;
alter table public.sov_news add column if not exists image_alt text;
alter table public.sov_news add column if not exists gallery_urls text[] not null default '{}';
alter table public.sov_news add column if not exists attachment_urls text[] not null default '{}';
alter table public.sov_news add column if not exists content_html text;
alter table public.sov_news add column if not exists featured boolean not null default false;
alter table public.sov_news add column if not exists source text default 'sov-web';
alter table public.sov_news add column if not exists legacy_url text;
alter table public.sov_news add column if not exists tags text[] not null default '{}';

update public.sov_news
set slug = lower(regexp_replace(regexp_replace(unaccent(coalesce(title,'')), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'))
where slug is null or slug = '';

create unique index if not exists sov_news_slug_key on public.sov_news (slug);
create index if not exists sov_news_published_idx on public.sov_news (published, pinned desc, featured desc, published_at desc);
create index if not exists sov_news_category_idx on public.sov_news (category);

alter table public.sov_news enable row level security;

drop policy if exists "sov_news_public_read_published" on public.sov_news;
create policy "sov_news_public_read_published"
  on public.sov_news for select
  using (published = true or public.sov_can_edit_news());

drop policy if exists "sov_news_editor_insert" on public.sov_news;
create policy "sov_news_editor_insert"
  on public.sov_news for insert
  with check (public.sov_can_edit_news());

drop policy if exists "sov_news_editor_update" on public.sov_news;
create policy "sov_news_editor_update"
  on public.sov_news for update
  using (public.sov_can_edit_news())
  with check (public.sov_can_edit_news());

drop policy if exists "sov_news_editor_delete" on public.sov_news;
create policy "sov_news_editor_delete"
  on public.sov_news for delete
  using (public.sov_can_edit_news());

create or replace function public.set_sov_news_updated_at()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  new.updated_at = now();
  new.updated_by = auth.uid();
  if new.created_by is null then new.created_by = auth.uid(); end if;
  if new.slug is null or new.slug = '' then
    new.slug = lower(regexp_replace(regexp_replace(unaccent(coalesce(new.title,'')), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'));
  end if;
  return new;
end $$;

drop trigger if exists trg_sov_news_updated_at on public.sov_news;
create trigger trg_sov_news_updated_at
before insert or update on public.sov_news
for each row execute function public.set_sov_news_updated_at();

-- -------------------------------------------------------------
-- 4) Storage policies for news uploads — also use safe helper
-- -------------------------------------------------------------

do $$
begin
  if to_regclass('storage.buckets') is not null then
    insert into storage.buckets (id, name, public)
    values ('sov-news', 'sov-news', true)
    on conflict (id) do update set public = true;
  end if;
exception when others then
  raise notice 'Storage bucket creation skipped: %', sqlerrm;
end $$;

do $$
begin
  if to_regclass('storage.objects') is not null then
    drop policy if exists "sov_news_storage_public_read" on storage.objects;
    create policy "sov_news_storage_public_read" on storage.objects
      for select using (bucket_id = 'sov-news');

    drop policy if exists "sov_news_storage_editor_insert" on storage.objects;
    create policy "sov_news_storage_editor_insert" on storage.objects
      for insert with check (bucket_id = 'sov-news' and public.sov_can_edit_news());

    drop policy if exists "sov_news_storage_editor_update" on storage.objects;
    create policy "sov_news_storage_editor_update" on storage.objects
      for update using (bucket_id = 'sov-news' and public.sov_can_edit_news())
      with check (bucket_id = 'sov-news' and public.sov_can_edit_news());

    drop policy if exists "sov_news_storage_editor_delete" on storage.objects;
    create policy "sov_news_storage_editor_delete" on storage.objects
      for delete using (bucket_id = 'sov-news' and public.sov_can_edit_news());
  end if;
exception when others then
  raise notice 'Storage policies skipped: %', sqlerrm;
end $$;

-- -------------------------------------------------------------
-- 5) Public news RPCs — public site should not need direct table read fallback
-- -------------------------------------------------------------

create or replace function public.sov_news_public_list(p_limit integer default 60)
returns table (
  id uuid,
  slug text,
  title text,
  summary text,
  category text,
  image_url text,
  image_alt text,
  cta_url text,
  published_at timestamptz,
  pinned boolean,
  featured boolean
)
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
  select id, slug, title, summary, category, image_url, image_alt,
         coalesce(nullif(cta_url,''), 'vijest.html?slug=' || slug) as cta_url,
         published_at, pinned, featured
  from public.sov_news
  where published = true
  order by pinned desc, featured desc, published_at desc nulls last, created_at desc
  limit greatest(1, least(coalesce(p_limit,60), 200));
$$;

create or replace function public.sov_news_public_detail(p_slug text)
returns table (
  id uuid,
  slug text,
  title text,
  summary text,
  body text,
  content_html text,
  category text,
  author_name text,
  image_url text,
  image_alt text,
  gallery_urls text[],
  attachment_urls text[],
  pdf_url text,
  cta_label text,
  cta_url text,
  published_at timestamptz,
  legacy_url text
)
language sql
stable
security definer
set search_path = public, auth
set row_security = off
as $$
  select id, slug, title, summary, body, content_html, category, author_name,
         image_url, image_alt, gallery_urls, attachment_urls, pdf_url, cta_label, cta_url,
         published_at, legacy_url
  from public.sov_news
  where slug = p_slug and published = true
  limit 1;
$$;

grant usage on schema public to anon, authenticated;
grant select on public.sov_news to anon, authenticated;
grant insert, update, delete on public.sov_news to authenticated;
grant execute on function public.sov_news_public_list(integer) to anon, authenticated;
grant execute on function public.sov_news_public_detail(text) to anon, authenticated;

-- -------------------------------------------------------------
-- 6) Optional smoke checks; do not uncomment unless testing manually.
-- -------------------------------------------------------------
-- select public.sov_current_role(), public.sov_current_status(), public.sov_can_edit_news();
-- select count(*) from public.sov_news;
