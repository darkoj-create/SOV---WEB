-- SOV News CMS v5.58.7
-- Fix: Supabase Storage bucket/policies for news editor uploads.
-- Problem: editor UI could read/edit sov_news, but uploading to bucket sov-news failed with
--          "new row violates row-level security policy" on storage.objects.
--
-- This patch keeps public read for news assets and allows authenticated users to upload/update/delete
-- files inside the sov-news bucket. The editor page itself remains role-gated in the web UI, while
-- storage no longer depends on profile/role helper chains that caused RLS recursion and false negatives.

begin;

-- 1) Ensure bucket exists and is public for cover/gallery rendering on the public site.
do $$
begin
  if to_regclass('storage.buckets') is not null then
    insert into storage.buckets (id, name, public)
    values ('sov-news', 'sov-news', true)
    on conflict (id) do update
      set public = true,
          name = excluded.name;
  end if;
exception when others then
  raise notice 'sov-news bucket ensure skipped/failed: %', sqlerrm;
end $$;

-- 2) Replace only SOV news storage policies. Do not touch other Storage buckets/modules.
do $$
declare
  r record;
begin
  if to_regclass('storage.objects') is not null then
    for r in
      select policyname
      from pg_policies
      where schemaname = 'storage'
        and tablename = 'objects'
        and policyname like 'sov_news_storage%'
    loop
      execute format('drop policy if exists %I on storage.objects', r.policyname);
    end loop;

    -- Public website can render covers/galleries without login.
    create policy "sov_news_storage_public_read v5587"
      on storage.objects
      for select
      to anon, authenticated
      using (bucket_id = 'sov-news');

    -- Upload must not depend on public.profiles or role helper functions. Those were the source of
    -- false RLS failures in Supabase Storage. Login is still required.
    create policy "sov_news_storage_authenticated_insert v5587"
      on storage.objects
      for insert
      to authenticated
      with check (bucket_id = 'sov-news' and auth.uid() is not null);

    create policy "sov_news_storage_authenticated_update v5587"
      on storage.objects
      for update
      to authenticated
      using (bucket_id = 'sov-news' and auth.uid() is not null)
      with check (bucket_id = 'sov-news' and auth.uid() is not null);

    create policy "sov_news_storage_authenticated_delete v5587"
      on storage.objects
      for delete
      to authenticated
      using (bucket_id = 'sov-news' and auth.uid() is not null);
  end if;
end $$;

-- 3) Grants. Supabase usually already has these, but this makes the patch resilient.
do $$
begin
  if to_regclass('storage.buckets') is not null then
    execute 'grant select on storage.buckets to anon, authenticated';
  end if;
  if to_regclass('storage.objects') is not null then
    execute 'grant select on storage.objects to anon, authenticated';
    execute 'grant insert, update, delete on storage.objects to authenticated';
  end if;
exception when others then
  raise notice 'storage grants skipped/failed: %', sqlerrm;
end $$;

-- 4) Small diagnostic RPC used by sync-status.html v5.58.7.
create or replace function public.sov_news_storage_status()
returns jsonb
language plpgsql
stable
security definer
set search_path = public, storage, auth
set row_security = off
as $$
declare
  v_exists boolean := false;
  v_public boolean := false;
  v_can_edit boolean := false;
  v_uid uuid := auth.uid();
begin
  if to_regclass('storage.buckets') is not null then
    execute 'select true, public from storage.buckets where id = $1 limit 1'
      into v_exists, v_public
      using 'sov-news';
  end if;

  begin
    v_can_edit := coalesce(public.sov_can_edit_news(), false);
  exception when others then
    v_can_edit := false;
  end;

  return jsonb_build_object(
    'bucket', 'sov-news',
    'exists', coalesce(v_exists, false),
    'public', coalesce(v_public, false),
    'auth_uid', v_uid,
    'can_edit_news', coalesce(v_can_edit, false),
    'upload_policy', 'authenticated users may upload to sov-news',
    'version', '5.58.7'
  );
end;
$$;

grant execute on function public.sov_news_storage_status() to anon, authenticated;

commit;
