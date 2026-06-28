-- SOV Web v6.1.45b — Oružarstvo loans materializer hotfix
-- Status: APPLIED on live Supabase ncomefzkuixyfixisrhi on 2026-06-27
-- Purpose: when an equipment request is marked issued/partial_return, create matching equipment_loans/equipment_loan_items rows.

create or replace function public.sov_armory_materialize_loan_from_request(p_request_id uuid)
returns uuid
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_req public.equipment_requests%rowtype;
  v_loan_id uuid;
begin
  select * into v_req from public.equipment_requests where id = p_request_id;
  if not found then return null; end if;
  if coalesce(v_req.status,'') not in ('issued','partial_return') then return null; end if;

  select id into v_loan_id from public.equipment_loans where request_id = p_request_id limit 1;

  if v_loan_id is null then
    insert into public.equipment_loans (
      request_id, borrower_id, borrower_name, borrower_email,
      user_id, user_name, user_email, trip_name,
      issued_by, issued_at, due_date, status, note, updated_at
    ) values (
      v_req.id, v_req.requester_id, v_req.requester_name, v_req.requester_email,
      v_req.requester_id, v_req.requester_name, v_req.requester_email, coalesce(v_req.trip_name, v_req.trip),
      v_req.decided_by, coalesce(v_req.decided_at, now()), v_req.date_to,
      case when v_req.status = 'partial_return' then 'partial_return' else 'issued' end,
      v_req.note, now()
    ) returning id into v_loan_id;
  end if;

  insert into public.equipment_loan_items (
    loan_id, equipment_item_id, equipment_legacy_id, item_name, name,
    quantity, condition_out, note, returned_quantity, return_status
  )
  select v_loan_id, ri.equipment_item_id, ri.equipment_legacy_id,
    coalesce(nullif(ri.item_name,''), nullif(ri.name,''), 'Artikl'),
    coalesce(nullif(ri.name,''), nullif(ri.item_name,''), 'Artikl'),
    coalesce(ri.quantity, 1), 'issued', ri.note, 0, 'out'
  from public.equipment_request_items ri
  where ri.request_id = p_request_id
    and not exists (
      select 1 from public.equipment_loan_items li
      where li.loan_id = v_loan_id
        and coalesce(li.equipment_legacy_id,'') = coalesce(ri.equipment_legacy_id,'')
        and coalesce(li.item_name,'') = coalesce(nullif(ri.item_name,''), nullif(ri.name,''), 'Artikl')
    );

  return v_loan_id;
end;
$$;

create or replace function public.sov_armory_request_loan_materialize_trigger()
returns trigger
language plpgsql
security definer
set search_path = public, auth
as $$
begin
  if new.status in ('issued','partial_return') and (old.status is distinct from new.status) then
    perform public.sov_armory_materialize_loan_from_request(new.id);
  end if;
  return new;
end;
$$;

drop trigger if exists equipment_requests_materialize_loan_on_issue on public.equipment_requests;
create trigger equipment_requests_materialize_loan_on_issue
after update of status on public.equipment_requests
for each row
execute function public.sov_armory_request_loan_materialize_trigger();

select public.sov_armory_materialize_loan_from_request(id)
from public.equipment_requests
where status in ('issued','partial_return');
