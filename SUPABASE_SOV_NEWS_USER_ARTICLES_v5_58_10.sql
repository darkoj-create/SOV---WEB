-- SOV News user article submissions v5.58.10
-- Adds a safe member-facing RPC: users can submit articles as hidden drafts.
-- Editors/Admins still publish through news-editor.html / sov_news_save().

begin;

create extension if not exists pgcrypto;
create extension if not exists unaccent;

-- Resilient baseline if earlier news SQL was only partially applied.
create table if not exists public.sov_news (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  summary text,
  body text,
  image_url text,
  pdf_url text,
  cta_label text,
  cta_url text,
  published boolean not null default false,
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

create unique index if not exists sov_news_slug_key on public.sov_news (slug);
create index if not exists sov_news_published_idx on public.sov_news (published, pinned desc, featured desc, published_at desc);
create index if not exists sov_news_category_idx on public.sov_news (category);
create index if not exists sov_news_source_idx on public.sov_news (source);
create index if not exists sov_news_created_by_idx on public.sov_news (created_by);

create or replace function public.sov_news_submit_article(p_payload jsonb default '{}'::jsonb)
returns jsonb
language plpgsql
security definer
set search_path = public, auth
set row_security = off
as $$
declare
  v_row public.sov_news%rowtype;
  v_title text;
  v_base_slug text;
  v_slug text;
  v_body text;
  v_now timestamptz := now();
  v_try int := 0;
begin
  if auth.uid() is null then
    raise exception 'Moraš biti prijavljen za predaju članka.' using errcode = '28000';
  end if;

  v_title := nullif(trim(coalesce(p_payload ->> 'title', '')), '');
  v_body := nullif(trim(coalesce(p_payload ->> 'body', '')), '');

  if v_title is null then
    raise exception 'Naslov je obavezan.' using errcode = '22023';
  end if;
  if v_body is null then
    raise exception 'Tekst članka je obavezan.' using errcode = '22023';
  end if;

  v_base_slug := lower(regexp_replace(regexp_replace(unaccent(v_title), '[^a-zA-Z0-9]+', '-', 'g'), '(^-|-$)', '', 'g'));
  if v_base_slug is null or v_base_slug = '' then
    v_base_slug := 'clanak';
  end if;
  v_base_slug := left(v_base_slug, 70);
  v_slug := v_base_slug || '-draft-' || left(replace(gen_random_uuid()::text, '-', ''), 8);

  loop
    exit when not exists (select 1 from public.sov_news where slug = v_slug);
    v_try := v_try + 1;
    v_slug := v_base_slug || '-draft-' || left(replace(gen_random_uuid()::text, '-', ''), 8);
    if v_try > 10 then
      raise exception 'Ne mogu generirati jedinstveni slug za članak.' using errcode = '23505';
    end if;
  end loop;

  insert into public.sov_news (
    title,
    slug,
    category,
    author_name,
    published_at,
    summary,
    image_url,
    image_alt,
    body,
    content_html,
    gallery_urls,
    pdf_url,
    cta_label,
    cta_url,
    published,
    pinned,
    featured,
    source,
    created_by,
    updated_by,
    created_at,
    updated_at
  ) values (
    v_title,
    v_slug,
    coalesce(nullif(trim(p_payload ->> 'category'), ''), 'Novosti'),
    nullif(trim(coalesce(p_payload ->> 'author_name', '')), ''),
    v_now,
    nullif(trim(coalesce(p_payload ->> 'summary', '')), ''),
    nullif(trim(coalesce(p_payload ->> 'image_url', '')), ''),
    nullif(trim(coalesce(p_payload ->> 'image_alt', '')), ''),
    v_body,
    coalesce(nullif(p_payload ->> 'content_html', ''), '<p>' || replace(replace(replace(replace(v_body, '&', '&amp;'), '<', '&lt;'), '>', '&gt;'), chr(10), '<br>') || '</p>'),
    '{}'::text[],
    null,
    null,
    'vijest.html?slug=' || v_slug,
    false,
    false,
    false,
    'user-submission-v5.58.10',
    auth.uid(),
    auth.uid(),
    v_now,
    v_now
  ) returning * into v_row;

  return jsonb_build_object(
    'ok', true,
    'id', v_row.id,
    'slug', v_row.slug,
    'title', v_row.title,
    'published', v_row.published,
    'source', v_row.source,
    'message', 'Članak je predan uredniku kao draft.'
  );
end;
$$;

grant execute on function public.sov_news_submit_article(jsonb) to authenticated;

comment on function public.sov_news_submit_article(jsonb) is 'v5.58.10: member-facing article submission. Inserts hidden sov_news draft; editor publishes later.';

commit;
