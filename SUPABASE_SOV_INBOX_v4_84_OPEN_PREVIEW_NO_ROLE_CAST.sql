-- SOV Inbox v4.84
-- Full open preview inbox bez enum/text role usporedbi.
-- Pokrenuti jednom. Ovo je otvoreni demo mode: svi s linkom mogu čitati/slati/uređivati obavijesti.

create extension if not exists pgcrypto;

create table if not exists public.sov_notifications (
  id uuid primary key default gen_random_uuid(),
  title text not null,
  body text not null,
  priority text not null default 'normal',
  target_role text not null default 'all',
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

-- Makni stare policyje koje su koristile role enum/text usporedbe.
drop policy if exists "SOV notifications read visible" on public.sov_notifications;
drop policy if exists "SOV notifications read visible v483" on public.sov_notifications;
drop policy if exists "SOV notifications insert admin armory" on public.sov_notifications;
drop policy if exists "SOV notifications insert preview v483" on public.sov_notifications;
drop policy if exists "SOV notifications update admin armory" on public.sov_notifications;
drop policy if exists "SOV notifications update preview v483" on public.sov_notifications;
drop policy if exists "SOV notifications open preview select v484" on public.sov_notifications;
drop policy if exists "SOV notifications open preview insert v484" on public.sov_notifications;
drop policy if exists "SOV notifications open preview update v484" on public.sov_notifications;
drop policy if exists "SOV notifications open preview delete v484" on public.sov_notifications;

create policy "SOV notifications open preview select v484" on public.sov_notifications
for select using (true);
create policy "SOV notifications open preview insert v484" on public.sov_notifications
for insert with check (true);
create policy "SOV notifications open preview update v484" on public.sov_notifications
for update using (true) with check (true);
create policy "SOV notifications open preview delete v484" on public.sov_notifications
for delete using (true);

drop policy if exists "SOV notification reads own select" on public.sov_notification_reads;
drop policy if exists "SOV notification reads own select v483" on public.sov_notification_reads;
drop policy if exists "SOV notification reads own upsert" on public.sov_notification_reads;
drop policy if exists "SOV notification reads own insert v483" on public.sov_notification_reads;
drop policy if exists "SOV notification reads own update" on public.sov_notification_reads;
drop policy if exists "SOV notification reads own update v483" on public.sov_notification_reads;
drop policy if exists "SOV notification reads open preview select v484" on public.sov_notification_reads;
drop policy if exists "SOV notification reads open preview insert v484" on public.sov_notification_reads;
drop policy if exists "SOV notification reads open preview update v484" on public.sov_notification_reads;
drop policy if exists "SOV notification reads open preview delete v484" on public.sov_notification_reads;

create policy "SOV notification reads open preview select v484" on public.sov_notification_reads
for select using (true);
create policy "SOV notification reads open preview insert v484" on public.sov_notification_reads
for insert with check (true);
create policy "SOV notification reads open preview update v484" on public.sov_notification_reads
for update using (true) with check (true);
create policy "SOV notification reads open preview delete v484" on public.sov_notification_reads
for delete using (true);

create index if not exists sov_notifications_target_idx on public.sov_notifications(target_role, status, created_at desc);
create index if not exists sov_notifications_user_idx on public.sov_notifications(target_user_id, status, created_at desc);
