-- SOV Web v5.34 — Unified Identity + Sync Control
-- Additive / safe foundation. Does not alter object database tables.

create table if not exists public.sov_sync_queue (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete set null,
  entity_type text not null check (entity_type in ('trip','waypoint','track','news','drawing','object_note','equipment')),
  operation text not null check (operation in ('create','update','delete','upsert')),
  local_ref text,
  remote_ref text,
  payload jsonb not null default '{}'::jsonb,
  status text not null default 'pending' check (status in ('pending','processing','done','failed','ignored')),
  attempts integer not null default 0,
  last_error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  processed_at timestamptz
);

create index if not exists sov_sync_queue_user_status_idx on public.sov_sync_queue(user_id, status, created_at desc);
create index if not exists sov_sync_queue_entity_idx on public.sov_sync_queue(entity_type, remote_ref);

create table if not exists public.sov_user_devices (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users(id) on delete cascade,
  device_label text,
  platform text not null default 'android',
  app_version text,
  last_seen_at timestamptz not null default now(),
  last_sync_at timestamptz,
  sync_state text not null default 'unknown',
  created_at timestamptz not null default now()
);

create index if not exists sov_user_devices_user_idx on public.sov_user_devices(user_id, last_seen_at desc);

create or replace view public.sov_current_user_permissions as
select
  p.user_id,
  coalesce(p.email, au.email) as email,
  coalesce(p.full_name, '') as full_name,
  coalesce(p.role::text, 'user') as role,
  coalesce(p.status::text, 'pending') as status,
  case when coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_view_sov_base,
  case when coalesce(p.role::text, 'user') in ('admin','arhivar','urednik') and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_view_katastar,
  case when coalesce(p.role::text, 'user') in ('admin','arhivar') and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_edit_objects,
  case when coalesce(p.role::text, 'user') in ('admin','arhivar','urednik') and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_upload_drawings,
  case when coalesce(p.role::text, 'user') in ('admin','arhivar') and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_verify_drawings,
  case when coalesce(p.role::text, 'user') in ('admin','urednik') and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_manage_trips,
  case when coalesce(p.role::text, 'user') in ('admin','oruzar') and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_manage_equipment,
  case when coalesce(p.role::text, 'user') in ('admin','urednik') and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_edit_news,
  case when coalesce(p.role::text, 'user') = 'admin' and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_use_sql_tools,
  case when coalesce(p.role::text, 'user') = 'admin' and coalesce(p.status::text, 'pending') = 'approved' then true else false end as can_manage_users
from public.sov_profiles p
left join auth.users au on au.id = p.user_id
where p.user_id = auth.uid();

alter table public.sov_sync_queue enable row level security;
alter table public.sov_user_devices enable row level security;

drop policy if exists "Users can see own sync queue" on public.sov_sync_queue;
create policy "Users can see own sync queue" on public.sov_sync_queue
for select using (user_id = auth.uid());

drop policy if exists "Users can insert own sync queue" on public.sov_sync_queue;
create policy "Users can insert own sync queue" on public.sov_sync_queue
for insert with check (user_id = auth.uid());

drop policy if exists "Users can update own sync queue" on public.sov_sync_queue;
create policy "Users can update own sync queue" on public.sov_sync_queue
for update using (user_id = auth.uid()) with check (user_id = auth.uid());

drop policy if exists "Users can see own devices" on public.sov_user_devices;
create policy "Users can see own devices" on public.sov_user_devices
for select using (user_id = auth.uid());

drop policy if exists "Users can upsert own devices" on public.sov_user_devices;
create policy "Users can upsert own devices" on public.sov_user_devices
for all using (user_id = auth.uid()) with check (user_id = auth.uid());
