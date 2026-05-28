-- SOV Web v5.35 / APK 1.3.1 — Audit Log + User Devices
-- SAFE additive patch. Does not alter speleo object tables.

create table if not exists public.sov_audit_log (
  id uuid primary key default gen_random_uuid(),
  actor_id uuid references auth.users(id) on delete set null,
  actor_email text,
  action text not null,
  entity_type text not null default 'system',
  entity_id text,
  status text not null default 'ok' check (status in ('ok','warning','failed','queued','synced')),
  source text not null default 'web' check (source in ('web','apk','sql','system','import')),
  metadata jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create index if not exists sov_audit_log_actor_created_idx on public.sov_audit_log(actor_id, created_at desc);
create index if not exists sov_audit_log_entity_idx on public.sov_audit_log(entity_type, entity_id, created_at desc);
create index if not exists sov_audit_log_action_idx on public.sov_audit_log(action, created_at desc);

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

alter table public.sov_user_devices add column if not exists device_id text;
alter table public.sov_user_devices add column if not exists app_build text;
alter table public.sov_user_devices add column if not exists permissions_role text;
alter table public.sov_user_devices add column if not exists offline_queue_count integer not null default 0;
alter table public.sov_user_devices add column if not exists last_error text;

create unique index if not exists sov_user_devices_user_device_uidx on public.sov_user_devices(user_id, device_id) where device_id is not null;
create index if not exists sov_user_devices_last_seen_idx on public.sov_user_devices(last_seen_at desc);

alter table public.sov_audit_log enable row level security;
alter table public.sov_user_devices enable row level security;

drop policy if exists "Users can insert own audit log" on public.sov_audit_log;
create policy "Users can insert own audit log" on public.sov_audit_log
for insert with check (actor_id = auth.uid() or actor_id is null);

drop policy if exists "Users can see own audit log" on public.sov_audit_log;
create policy "Users can see own audit log" on public.sov_audit_log
for select using (actor_id = auth.uid());

drop policy if exists "Users can see own devices" on public.sov_user_devices;
create policy "Users can see own devices" on public.sov_user_devices
for select using (user_id = auth.uid());

drop policy if exists "Users can upsert own devices" on public.sov_user_devices;
create policy "Users can upsert own devices" on public.sov_user_devices
for all using (user_id = auth.uid()) with check (user_id = auth.uid());

create or replace view public.sov_user_devices_recent as
select
  d.id,
  d.user_id,
  coalesce(sp.email, au.email) as user_email,
  d.device_label,
  d.device_id,
  d.platform,
  d.app_version,
  d.app_build,
  d.permissions_role,
  d.sync_state,
  d.offline_queue_count,
  d.last_error,
  d.last_seen_at,
  d.last_sync_at,
  d.created_at
from public.sov_user_devices d
left join public.sov_profiles sp on sp.user_id = d.user_id
left join auth.users au on au.id = d.user_id;

create or replace view public.sov_audit_log_recent as
select
  a.id,
  a.actor_id,
  coalesce(a.actor_email, sp.email, au.email) as actor_email,
  a.action,
  a.entity_type,
  a.entity_id,
  a.status,
  a.source,
  a.metadata,
  a.created_at
from public.sov_audit_log a
left join public.sov_profiles sp on sp.user_id = a.actor_id
left join auth.users au on au.id = a.actor_id
order by a.created_at desc;

create or replace function public.sov_log_audit(
  p_action text,
  p_entity_type text default 'system',
  p_entity_id text default null,
  p_status text default 'ok',
  p_source text default 'web',
  p_metadata jsonb default '{}'::jsonb
) returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id uuid;
  v_email text;
begin
  select email into v_email from auth.users where id = auth.uid();
  insert into public.sov_audit_log(actor_id, actor_email, action, entity_type, entity_id, status, source, metadata)
  values (auth.uid(), v_email, p_action, p_entity_type, p_entity_id, p_status, p_source, coalesce(p_metadata, '{}'::jsonb))
  returning id into v_id;
  return v_id;
end;
$$;

grant execute on function public.sov_log_audit(text,text,text,text,text,jsonb) to authenticated;
grant select on public.sov_user_devices_recent to authenticated;
grant select on public.sov_audit_log_recent to authenticated;


-- Admin visibility for control center. Uses sov_profiles compatibility view created by v5.34.2 safe fix.
drop policy if exists "Admins can see all audit log" on public.sov_audit_log;
create policy "Admins can see all audit log" on public.sov_audit_log
for select using (exists (select 1 from public.sov_profiles sp where sp.user_id = auth.uid() and sp.role::text = 'admin' and sp.status::text = 'approved'));

drop policy if exists "Admins can see all devices" on public.sov_user_devices;
create policy "Admins can see all devices" on public.sov_user_devices
for select using (exists (select 1 from public.sov_profiles sp where sp.user_id = auth.uid() and sp.role::text = 'admin' and sp.status::text = 'approved'));

create or replace function public.sov_register_device(
  p_device_id text,
  p_device_label text default null,
  p_platform text default 'android',
  p_app_version text default null,
  p_app_build text default null,
  p_permissions_role text default null,
  p_offline_queue_count integer default 0,
  p_sync_state text default 'unknown',
  p_last_error text default null
) returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id uuid;
begin
  if auth.uid() is null then
    raise exception 'Not authenticated';
  end if;

  insert into public.sov_user_devices(
    user_id, device_id, device_label, platform, app_version, app_build,
    permissions_role, offline_queue_count, sync_state, last_error,
    last_seen_at, last_sync_at
  ) values (
    auth.uid(), p_device_id, p_device_label, coalesce(p_platform, 'android'), p_app_version, p_app_build,
    p_permissions_role, coalesce(p_offline_queue_count, 0), coalesce(p_sync_state, 'unknown'), p_last_error,
    now(), now()
  )
  on conflict (user_id, device_id) where device_id is not null do update set
    device_label = excluded.device_label,
    platform = excluded.platform,
    app_version = excluded.app_version,
    app_build = excluded.app_build,
    permissions_role = excluded.permissions_role,
    offline_queue_count = excluded.offline_queue_count,
    sync_state = excluded.sync_state,
    last_error = excluded.last_error,
    last_seen_at = now(),
    last_sync_at = now()
  returning id into v_id;

  return v_id;
end;
$$;

grant execute on function public.sov_register_device(text,text,text,text,text,text,integer,text,text) to authenticated;
