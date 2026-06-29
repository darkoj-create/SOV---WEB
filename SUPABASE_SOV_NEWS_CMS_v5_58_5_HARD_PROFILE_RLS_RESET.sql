-- SOV web v5.58.5 — News CMS hard profiles RLS reset
-- Pokreni POVRH v5.58.3/v5.58.4.
-- Cilj: maknuti "infinite recursion detected in policy for relation profiles".
-- Razlog: stari profiles policyji su imali uvjete koji preko helper funkcija opet citaju profiles.
-- Ovaj patch agresivno brise SVE policyje na public.profiles i vraca samo nerekurzivne self/JWT policyje.
-- Vijesti se zatim citaju bez pozivanja profiles policyja.

create extension if not exists pgcrypto;
create extension if not exists unaccent;

-- -------------------------------------------------------------------
-- 1) Hard reset public.profiles policies: NO helper/function calls here.
-- -------------------------------------------------------------------
do $block$
declare
  r record;
  v_has_id boolean := false;
  v_has_user_id boolean := false;
  v_admin_jwt_expr text := 'lower(coalesce(auth.jwt() -> ''app_metadata'' ->> ''role'', auth.jwt() -> ''user_metadata'' ->> ''role'', auth.jwt() ->> ''role'', '''')) in (''admin'',''administrator'')';
begin
  if to_regclass('public.profiles') is not null then
    -- Delete EVERY policy, not only known names. This is the important part.
    for r in
      select policyname
      from pg_policies
      where schemaname = 'public' and tablename = 'profiles'
    loop
      execute format('drop policy if exists %I on public.profiles', r.policyname);
    end loop;

    execute 'alter table public.profiles enable row level security';

    select exists (
      select 1 from information_schema.columns
      where table_schema='public' and table_name='profiles' and column_name='id'
    ) into v_has_id;

    select exists (
      select 1 from information_schema.columns
      where table_schema='public' and table_name='profiles' and column_name='user_id'
    ) into v_has_user_id;

    if v_has_id then
      execute 'create policy "profiles self read v5585 no recursion" on public.profiles for select using (id = auth.uid() or ' || v_admin_jwt_expr || ')';
      execute 'create policy "profiles self insert v5585 no recursion" on public.profiles for insert with check (id = auth.uid())';
      execute 'create policy "profiles self update v5585 no recursion" on public.profiles for update using (id = auth.uid() or ' || v_admin_jwt_expr || ') with check (id = auth.uid() or ' || v_admin_jwt_expr || ')';
    elsif v_has_user_id then
      execute 'create policy "profiles self read v5585 no recursion" on public.profiles for select using (user_id = auth.uid() or ' || v_admin_jwt_expr || ')';
      execute 'create policy "profiles self insert v5585 no recursion" on public.profiles for insert with check (user_id = auth.uid())';
      execute 'create policy "profiles self update v5585 no recursion" on public.profiles for update using (user_id = auth.uid() or ' || v_admin_jwt_expr || ') with check (user_id = auth.uid() or ' || v_admin_jwt_expr || ')';
    else
      -- Last-resort schema tolerance: do not recreate recursive policies on an unknown profiles shape.
      execute 'create policy "profiles authenticated read v5585 no recursion" on public.profiles for select to authenticated using (true)';
    end if;
  end if;
end $block$;

-- -------------------------------------------------------------------
-- 2) RLS-safe role helpers. They may read profiles only inside SECURITY DEFINER
--    with row_security=off, and they are NOT used inside profiles policies.
-- -------------------------------------------------------------------
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

  v_role := lower(nullif(trim(coalesce(
    v_jwt -> 'app_metadata' ->> 'role',
    v_jwt -> 'user_metadata' ->> 'role',
    v_jwt ->> 'role'
  )), ''));
  if v_role is not null then
    return v_role;
  end if;

  if to_regclass('public.profiles') is not null
     and exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='role') then
    if exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='id') then
      v_id_col := 'id';
    elsif exists (select 1 from information_schema.columns where table_schema='public' and table_name='profiles' and column_name='user_id') then
      v_id_col := 'user_id';
    end if;

    if v_id_col is not null then
      execute format('select lower(coalesce(role::text,'''')) from public.profiles where %I = $1 limit 1', v_id_col)
        into v_role using v_uid;
      if v_role is not null and length(trim(v_role)) > 0 then
        return lower(trim(v_role));
      end if;
    end if;
  end if;

  return 'user';
exception when others then
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
    end if;

    if v_id_col is not null then
      execute format('select lower(coalesce(status::text,'''')) from public.profiles where %I = $1 limit 1', v_id_col)
        into v_status using v_uid;
      if v_status is not null and length(trim(v_status)) > 0 then
        return lower(trim(v_status));
      end if;
    end if;
  end if;

  return 'approved';
exception when others then
  return 'approved';
end;
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
  v_col text;
begin
  if auth.uid() is null then return false; end if;
  if v_status not in ('approved','active','enabled','ok') then return false; end if;
  if v_role = 'admin' then return true; end if;

  v_col := case permission_name
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
    else null
  end;

  if v_col is not null and to_regclass('public.sov_role_permissions') is not null
     and exists (select 1 from information_schema.columns where table_schema='public' and table_name='sov_role_permissions' and column_name=v_col) then
    execute format('select coalesce(%I,false) from public.sov_role_permissions where role::text = $1 limit 1', v_col)
      into v_allowed using v_role;
  end if;

  if permission_name = 'edit_news' and v_role in ('editor','urednik') then return true; end if;
  return coalesce(v_allowed,false);
exception when others then
  if permission_name = 'edit_news' and v_role in ('admin','editor','urednik') then return true; end if;
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
grant execute on function public.sov_has_permission(text) to anon, authenticated;
grant execute on function public.sov_can_edit_news() to anon, authenticated;

-- -------------------------------------------------------------------
-- 3) Ensure sov_news exists and then reset its policies.
--    SELECT policies deliberately do NOT call profiles/role helpers.
-- -------------------------------------------------------------------
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

do $$
declare r record;
begin
  for r in select policyname from pg_policies where schemaname='public' and tablename='sov_news'
  loop
    execute format('drop policy if exists %I on public.sov_news', r.policyname);
  end loop;
end $$;

-- Public site can read only published news. No role/profile helper here.
create policy "sov_news public read published v5585"
  on public.sov_news for select
  to anon, authenticated
  using (published = true);

-- Logged-in users may load the editor list. Editing is still restricted below.
-- This avoids profile recursion during plain SELECT from news-editor.html.
create policy "sov_news authenticated read all v5585"
  on public.sov_news for select
  to authenticated
  using (auth.uid() is not null);

create policy "sov_news editor insert v5585"
  on public.sov_news for insert
  to authenticated
  with check (public.sov_can_edit_news());

create policy "sov_news editor update v5585"
  on public.sov_news for update
  to authenticated
  using (public.sov_can_edit_news())
  with check (public.sov_can_edit_news());

create policy "sov_news editor delete v5585"
  on public.sov_news for delete
  to authenticated
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

grant usage on schema public to anon, authenticated;
grant select on public.sov_news to anon, authenticated;
grant insert, update, delete on public.sov_news to authenticated;

-- -------------------------------------------------------------------
-- 4) Public RPCs bypass RLS; public pages should prefer these.
-- -------------------------------------------------------------------
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

grant execute on function public.sov_news_public_list(integer) to anon, authenticated;
grant execute on function public.sov_news_public_detail(text) to anon, authenticated;

-- -------------------------------------------------------------------
-- 5) Storage bucket/policies. Drop only our sov-news storage policies.
-- -------------------------------------------------------------------
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
declare r record;
begin
  if to_regclass('storage.objects') is not null then
    for r in
      select policyname from pg_policies
      where schemaname='storage' and tablename='objects' and policyname like 'sov_news_storage%'
    loop
      execute format('drop policy if exists %I on storage.objects', r.policyname);
    end loop;

    create policy "sov_news_storage_public_read v5585" on storage.objects
      for select using (bucket_id = 'sov-news');

    create policy "sov_news_storage_editor_insert v5585" on storage.objects
      for insert to authenticated
      with check (bucket_id = 'sov-news' and public.sov_can_edit_news());

    create policy "sov_news_storage_editor_update v5585" on storage.objects
      for update to authenticated
      using (bucket_id = 'sov-news' and public.sov_can_edit_news())
      with check (bucket_id = 'sov-news' and public.sov_can_edit_news());

    create policy "sov_news_storage_editor_delete v5585" on storage.objects
      for delete to authenticated
      using (bucket_id = 'sov-news' and public.sov_can_edit_news());
  end if;
exception when others then
  raise notice 'Storage policies skipped: %', sqlerrm;
end $$;

-- -------------------------------------------------------------------
-- 6) Smoke checks. These should run without profiles recursion.
-- -------------------------------------------------------------------
do $$
declare
  v_news_count integer;
begin
  select count(*) into v_news_count from public.sov_news;
  raise notice 'SOV news CMS v5.58.5 installed. sov_news rows: %', v_news_count;
end $$;
