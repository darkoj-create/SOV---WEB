-- SOV News CMS v5.58.9
-- Fix: news-editor save no longer depends on PostgREST .single() coercion.
-- Problem in UI: "Greška spremanja: Cannot coerce the result to a single JSON object".
-- Cause: direct insert/update + .select().single() can return 0 rows under RLS/returning edge cases.
-- Solution: one SECURITY DEFINER RPC that validates editor rights, writes the row, and returns exactly one JSON object.

begin;

create extension if not exists pgcrypto;
create extension if not exists unaccent;

-- Keep schema resilient if v5.58.3 was not fully applied.
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

-- Core save RPC used by news-editor.html v5.58.9.
create or replace function public.sov_news_save(p_id uuid default null, p_payload jsonb default '{}'::jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_row public.sov_news%rowtype;
  v_title text;
  v_slug text;
  v_body text;
  v_gallery text[] := '{}'::text[];
  v_now timestamptz := now();
begin
  if auth.uid() is null then
    raise exception 'Moraš biti prijavljen za uređivanje vijesti.' using errcode = '28000';
  end if;

  if not coalesce(public.sov_can_edit_news(), false) then
    raise exception 'Nemaš prava za uređivanje vijesti.' using errcode = '42501';
  end if;

  v_title := nullif(trim(coalesce(p_payload ->> 'title', '')), '');
  if v_title is null then
    raise exception 'Naslov je obavezan.' using errcode = '22023';
  end if;

  v_slug := nullif(trim(coalesce(p_payload ->> 'slug', '')), '');
  if v_slug is null then
    v_slug := v_title;
  end if;
  v_slug := lower(regexp_replace(regexp_replace(unaccent(v_slug), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'));
  if v_slug is null or v_slug = '' then
    v_slug := 'vijest-' || replace(gen_random_uuid()::text, '-', '');
  end if;

  v_body := coalesce(p_payload ->> 'body', '');

  if jsonb_typeof(p_payload -> 'gallery_urls') = 'array' then
    select coalesce(array_agg(x), '{}'::text[])
      into v_gallery
    from jsonb_array_elements_text(p_payload -> 'gallery_urls') as t(x);
  else
    v_gallery := '{}'::text[];
  end if;

  if p_id is null then
    insert into public.sov_news (
      title, slug, category, author_name, published_at, summary, image_url, image_alt,
      body, content_html, gallery_urls, pdf_url, cta_label, cta_url,
      published, pinned, featured, source, created_by, updated_by, created_at, updated_at
    ) values (
      v_title,
      v_slug,
      coalesce(nullif(trim(p_payload ->> 'category'), ''), 'Novosti'),
      nullif(trim(coalesce(p_payload ->> 'author_name', '')), ''),
      coalesce(nullif(p_payload ->> 'published_at', '')::timestamptz, v_now),
      nullif(trim(coalesce(p_payload ->> 'summary', '')), ''),
      nullif(trim(coalesce(p_payload ->> 'image_url', '')), ''),
      nullif(trim(coalesce(p_payload ->> 'image_alt', '')), ''),
      v_body,
      coalesce(p_payload ->> 'content_html', ''),
      v_gallery,
      nullif(trim(coalesce(p_payload ->> 'pdf_url', '')), ''),
      nullif(trim(coalesce(p_payload ->> 'cta_label', '')), ''),
      nullif(trim(coalesce(p_payload ->> 'cta_url', '')), ''),
      coalesce((p_payload ->> 'published')::boolean, true),
      coalesce((p_payload ->> 'pinned')::boolean, false),
      coalesce((p_payload ->> 'featured')::boolean, false),
      'news-editor-v5.58.9',
      auth.uid(),
      auth.uid(),
      v_now,
      v_now
    )
    returning * into v_row;
  else
    update public.sov_news
    set
      title = v_title,
      slug = v_slug,
      category = coalesce(nullif(trim(p_payload ->> 'category'), ''), 'Novosti'),
      author_name = nullif(trim(coalesce(p_payload ->> 'author_name', '')), ''),
      published_at = coalesce(nullif(p_payload ->> 'published_at', '')::timestamptz, published_at, v_now),
      summary = nullif(trim(coalesce(p_payload ->> 'summary', '')), ''),
      image_url = nullif(trim(coalesce(p_payload ->> 'image_url', '')), ''),
      image_alt = nullif(trim(coalesce(p_payload ->> 'image_alt', '')), ''),
      body = v_body,
      content_html = coalesce(p_payload ->> 'content_html', ''),
      gallery_urls = v_gallery,
      pdf_url = nullif(trim(coalesce(p_payload ->> 'pdf_url', '')), ''),
      cta_label = nullif(trim(coalesce(p_payload ->> 'cta_label', '')), ''),
      cta_url = nullif(trim(coalesce(p_payload ->> 'cta_url', '')), ''),
      published = coalesce((p_payload ->> 'published')::boolean, published),
      pinned = coalesce((p_payload ->> 'pinned')::boolean, pinned),
      featured = coalesce((p_payload ->> 'featured')::boolean, featured),
      updated_by = auth.uid(),
      updated_at = v_now
    where id = p_id
    returning * into v_row;

    if v_row.id is null then
      raise exception 'Vijest nije pronađena ili nije mogla biti ažurirana: %', p_id using errcode = 'P0002';
    end if;
  end if;

  return to_jsonb(v_row);
exception
  when unique_violation then
    raise exception 'Slug već postoji. Promijeni URL/slug pa spremi ponovno.' using errcode = '23505';
end;
$$;

grant execute on function public.sov_news_save(uuid, jsonb) to authenticated;

-- Optional delete RPC for future editor hardening. Existing UI may still use direct delete.
create or replace function public.sov_news_delete(p_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_old public.sov_news%rowtype;
begin
  if auth.uid() is null then
    raise exception 'Moraš biti prijavljen za brisanje vijesti.' using errcode = '28000';
  end if;
  if not coalesce(public.sov_can_edit_news(), false) then
    raise exception 'Nemaš prava za brisanje vijesti.' using errcode = '42501';
  end if;

  delete from public.sov_news where id = p_id returning * into v_old;
  if v_old.id is null then
    raise exception 'Vijest nije pronađena: %', p_id using errcode = 'P0002';
  end if;
  return jsonb_build_object('deleted', true, 'id', v_old.id, 'slug', v_old.slug, 'title', v_old.title);
end;
$$;

grant execute on function public.sov_news_delete(uuid) to authenticated;

commit;
