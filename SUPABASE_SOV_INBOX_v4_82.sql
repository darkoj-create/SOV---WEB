-- SOV v4.83 — Inbox / obavijesti ROLE CAST FIX
-- Pokrenuti jednom u Supabase SQL editoru.
-- Fix: profiles.role moze biti enum (sov_user_role), zato ga svugdje castamo u text.

create table if not exists public.sov_notifications (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  body text not null,
  priority text not null default 'normal',
  target_role text not null default 'all', -- all/user/armory/admin/oruzar
  target_user_id uuid null,
  author_id uuid null,
  author_name text null,
  status text not null default 'active',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  expires_at timestamptz null
);

create table if not exists public.sov_notification_reads (
  notification_id uuid not null references public.sov_notifications(id) on delete cascade,
  user_id uuid not null,
  read_at timestamptz not null default now(),
  primary key(notification_id, user_id)
);

alter table public.sov_notifications enable row level security;
alter table public.sov_notification_reads enable row level security;

-- Helper bez type mismatcha: enum/text role uvijek vraca text.
create or replace function public.sov_current_role_text()
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select p.role::text from public.profiles p where p.id = auth.uid()), 'user')
$$;

grant execute on function public.sov_current_role_text() to anon, authenticated;

-- Preview/demo mode: svi s linkom mogu citati aktivne globalne obavijesti.
-- Ako je korisnik ulogiran, vide se i targetirane obavijesti po roli / useru.
drop policy if exists "SOV notifications read visible" on public.sov_notifications;
drop policy if exists "SOV notifications read visible v483" on public.sov_notifications;
create policy "SOV notifications read visible v483" on public.sov_notifications
for select using (
  status = 'active' and (
    target_role in ('all','svi','user')
    or target_user_id = auth.uid()
    or target_role = public.sov_current_role_text()
    or (target_role in ('armory','oruzar') and public.sov_current_role_text() in ('admin','oruzar'))
    or (target_role = 'admin' and public.sov_current_role_text() = 'admin')
  )
);

-- U full-open previewu dopustamo slanje iz UI-a i bez logina.
-- Kad se vrati zatvoreni login mode, ovu policy treba postroziti na admin/oruzar.
drop policy if exists "SOV notifications insert admin armory" on public.sov_notifications;
drop policy if exists "SOV notifications insert preview v483" on public.sov_notifications;
create policy "SOV notifications insert preview v483" on public.sov_notifications
for insert with check (true);

drop policy if exists "SOV notifications update admin armory" on public.sov_notifications;
drop policy if exists "SOV notifications update preview v483" on public.sov_notifications;
create policy "SOV notifications update preview v483" on public.sov_notifications
for update using (true) with check (true);

-- Reads table: ulogirani korisnici imaju svoj read state.
-- Za anon preview frontend smije koristiti local fallback/localStorage.
drop policy if exists "SOV notification reads own select" on public.sov_notification_reads;
drop policy if exists "SOV notification reads own select v483" on public.sov_notification_reads;
create policy "SOV notification reads own select v483" on public.sov_notification_reads
for select using (user_id = auth.uid());

drop policy if exists "SOV notification reads own upsert" on public.sov_notification_reads;
drop policy if exists "SOV notification reads own insert v483" on public.sov_notification_reads;
create policy "SOV notification reads own insert v483" on public.sov_notification_reads
for insert with check (user_id = auth.uid());

drop policy if exists "SOV notification reads own update" on public.sov_notification_reads;
drop policy if exists "SOV notification reads own update v483" on public.sov_notification_reads;
create policy "SOV notification reads own update v483" on public.sov_notification_reads
for update using (user_id = auth.uid()) with check (user_id = auth.uid());

create index if not exists sov_notifications_target_idx on public.sov_notifications(target_role, status, created_at desc);
create index if not exists sov_notifications_user_idx on public.sov_notifications(target_user_id, status, created_at desc);
