-- SOV Calendar Events v6.1.11 / APK v1.4.16j
-- Dodatni događaji za SOV Cloud kalendar. Izleti ostaju u sov_trips / sov_trips_mobile_feed.

create extension if not exists pgcrypto;

create table if not exists public.sov_calendar_events (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    description text not null default '',
    event_type text not null default 'ostalo',
    start_at timestamptz not null,
    end_at timestamptz not null default now(),
    location text not null default '',
    visibility text not null default 'members',
    created_by uuid references auth.users(id) on delete set null default auth.uid(),
    created_by_email text default auth.email(),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    constraint sov_calendar_events_visibility_check check (visibility in ('public','members','staff','private'))
);

alter table public.sov_calendar_events enable row level security;

create index if not exists sov_calendar_events_start_idx on public.sov_calendar_events(start_at);
create index if not exists sov_calendar_events_type_idx on public.sov_calendar_events(event_type);
create index if not exists sov_calendar_events_created_by_idx on public.sov_calendar_events(created_by);

create or replace function public.sov_touch_calendar_event_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists trg_sov_calendar_events_updated_at on public.sov_calendar_events;
create trigger trg_sov_calendar_events_updated_at
before update on public.sov_calendar_events
for each row execute function public.sov_touch_calendar_event_updated_at();

-- čitanje: svi logirani korisnici vide public/members/staff evente; private samo creator
-- Ako želiš kasnije strože role, ovo spojimo s tvojim role viewom.
drop policy if exists "sov_calendar_events_select" on public.sov_calendar_events;
create policy "sov_calendar_events_select"
on public.sov_calendar_events
for select
to authenticated
using (
    visibility in ('public','members','staff')
    or created_by = auth.uid()
);

-- unos: svi logirani korisnici mogu dodati događaj
drop policy if exists "sov_calendar_events_insert" on public.sov_calendar_events;
create policy "sov_calendar_events_insert"
on public.sov_calendar_events
for insert
to authenticated
with check (created_by is null or created_by = auth.uid());

-- uređivanje/brisanje: creator može svoje; admin/webmaster kasnije preko role policyja ako želiš proširimo
drop policy if exists "sov_calendar_events_update_own" on public.sov_calendar_events;
create policy "sov_calendar_events_update_own"
on public.sov_calendar_events
for update
to authenticated
using (created_by = auth.uid())
with check (created_by = auth.uid());

drop policy if exists "sov_calendar_events_delete_own" on public.sov_calendar_events;
create policy "sov_calendar_events_delete_own"
on public.sov_calendar_events
for delete
to authenticated
using (created_by = auth.uid());

create or replace view public.sov_calendar_events_mobile_feed as
select
    id,
    title,
    description,
    event_type,
    start_at,
    end_at,
    location,
    visibility,
    created_by,
    created_by_email,
    created_at,
    updated_at
from public.sov_calendar_events
order by start_at asc;

grant select on public.sov_calendar_events_mobile_feed to authenticated;
grant select, insert, update, delete on public.sov_calendar_events to authenticated;
