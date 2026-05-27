-- v5.25 News Editor: admin/editor can manage public news
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

create index if not exists sov_news_published_idx on public.sov_news (published, pinned desc, published_at desc);

alter table public.sov_news enable row level security;

drop policy if exists "sov_news_public_read_published" on public.sov_news;
create policy "sov_news_public_read_published"
  on public.sov_news for select
  using (published = true);

drop policy if exists "sov_news_admin_editor_all" on public.sov_news;
create policy "sov_news_admin_editor_all"
  on public.sov_news for all
  using (
    exists (
      select 1 from public.profiles p
      where p.id = auth.uid()
      and lower(coalesce(p.role::text,'')) in ('admin','editor')
    )
  )
  with check (
    exists (
      select 1 from public.profiles p
      where p.id = auth.uid()
      and lower(coalesce(p.role::text,'')) in ('admin','editor')
    )
  );

create or replace function public.set_sov_news_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  new.updated_by = auth.uid();
  if new.created_by is null then new.created_by = auth.uid(); end if;
  return new;
end $$;

drop trigger if exists trg_sov_news_updated_at on public.sov_news;
create trigger trg_sov_news_updated_at
before insert or update on public.sov_news
for each row execute function public.set_sov_news_updated_at();

-- Seed expedition news only if not already present
insert into public.sov_news (title, summary, body, image_url, pdf_url, cta_label, cta_url, pinned, published, published_at)
select
  'Speleološka ekspedicija Sjeverni Velebit 2026',
  'SO PDS Velebit poziva na ekspediciju na području Hajdučkih i Rožanskih kukova od 25.7. do 9.8.2026.',
  'Ciljevi ekspedicije su rekognosciranje terena, prikupljanje podataka o poznatim objektima, monitoring leda i nastavak istraživanja u jami Nedam. Prijave su otvorene do 20.7.',
  'assets/news/ekspedicija-sov-2026-hero.png',
  'assets/news/ekspedicija-sov-2026.pdf',
  'Prijavi se',
  'https://docs.google.com/forms/d/e/1FAIpQLSfhDMbQJi0Nb6xykRwlYlBng_Hw_BsLga1HMO3Ao-Y1fr3fjg/viewform',
  true,
  true,
  now()
where not exists (
  select 1 from public.sov_news where title ilike '%Sjeverni Velebit 2026%'
);
