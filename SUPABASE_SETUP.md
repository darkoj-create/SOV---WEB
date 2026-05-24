# SOV Cloud — Supabase login setup

Ovaj build koristi Supabase Auth + tablicu `profiles` za role i odobravanje korisnika.

## 1) Konfiguracija frontenda

Uredi:

`assets/supabase-config.js`

Upiši:

```js
window.SOV_SUPABASE_URL = 'https://xxxxx.supabase.co';
window.SOV_SUPABASE_ANON_KEY = 'tvoj-anon-public-key';
```

Anon public key smije biti u browseru. Service role key se NE stavlja u frontend.

---

## 2) SQL u Supabase SQL editor

Pokreni ovo jednom:

```sql
create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text unique,
  full_name text,
  role text not null default 'user' check (role in ('admin','user','editor','oruzar')),
  status text not null default 'pending' check (status in ('pending','approved','rejected')),
  note text,
  created_at timestamptz default now(),
  approved_at timestamptz
);

alter table public.profiles enable row level security;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.profiles
    where id = auth.uid()
      and role = 'admin'
      and status = 'approved'
  );
$$;

create policy "read own profile or admin reads all"
on public.profiles for select
using (id = auth.uid() or public.is_admin());

create policy "insert own pending profile"
on public.profiles for insert
with check (
  id = auth.uid()
  and role = 'user'
  and status = 'pending'
);

create policy "update own basic profile"
on public.profiles for update
using (id = auth.uid())
with check (
  id = auth.uid()
  and role = (select role from public.profiles where id = auth.uid())
  and status = (select status from public.profiles where id = auth.uid())
);

create policy "admin can update profiles"
on public.profiles for update
using (public.is_admin())
with check (public.is_admin());
```

---

## 3) Prvi admin

Nakon što se prvi korisnik registrira kroz web, u Supabase SQL editoru ga ručno promoviraj:

```sql
update public.profiles
set role = 'admin', status = 'approved', approved_at = now()
where email = 'tvoj-email@example.com';
```

Nakon toga taj admin može odobravati ostale korisnike iz:

`admin-users.html`

---

## 4) Role logika

- `admin` — odobrava korisnike, mijenja role, uređuje/odobrava vijesti, ima pristup oružarstvu.
- `editor` — uređuje i odobrava vijesti, nema pristup odobravanju korisnika.
- `oruzar` — pristup oružarstvu i članskim alatima, nema vijesti/admin users.
- `user` — standardni član, baza, izleti, dokumenti, zapisnici, pregled.

---

## 5) Zaštita stranica

Sve registered stranice sada traže odobren Supabase račun.

Posebna pravila:

- `admin-users.html` → samo `admin`
- `oruzarstvo.html` → `admin` ili `oruzar`
- ostale članske stranice → svi approved korisnici

