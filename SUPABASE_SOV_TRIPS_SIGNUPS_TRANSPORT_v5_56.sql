-- SOV Trips Cloud v5.56 — prijave + prijevoz
-- Safe patch: ne dira postojeće izlete, dodaje RPC + transport view i self-signup RLS.

-- 1) RLS: član smije sam upisati/ažurirati svoju prijavu na izlet koji smije čitati.
DROP POLICY IF EXISTS sov_trip_members_insert_self_signup ON public.sov_trip_members;
CREATE POLICY sov_trip_members_insert_self_signup ON public.sov_trip_members
FOR INSERT
WITH CHECK (
  auth.uid() IS NOT NULL
  AND user_id = auth.uid()
  AND public.sov_trip_can_read(trip_id)
);

DROP POLICY IF EXISTS sov_trip_members_update_self_signup ON public.sov_trip_members;
CREATE POLICY sov_trip_members_update_self_signup ON public.sov_trip_members
FOR UPDATE
USING (
  auth.uid() IS NOT NULL
  AND user_id = auth.uid()
  AND public.sov_trip_can_read(trip_id)
)
WITH CHECK (
  auth.uid() IS NOT NULL
  AND user_id = auth.uid()
  AND public.sov_trip_can_read(trip_id)
);

-- 2) RPC: jedan stabilan poziv za web/app prijavu i podatke o prijevozu.
CREATE OR REPLACE FUNCTION public.sov_trip_signup(
  p_trip_id uuid,
  p_attendance_status text DEFAULT 'confirmed',
  p_transport_mode text DEFAULT 'needs_ride',
  p_seats_available integer DEFAULT 0,
  p_departure_place text DEFAULT NULL,
  p_note text DEFAULT NULL,
  p_member_name text DEFAULT NULL,
  p_member_email text DEFAULT NULL
)
RETURNS public.sov_trip_members
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_email text;
  v_name text;
  v_existing_id uuid;
  v_row public.sov_trip_members;
  v_status text := lower(coalesce(nullif(trim(p_attendance_status), ''), 'confirmed'));
  v_transport text := lower(coalesce(nullif(trim(p_transport_mode), ''), 'needs_ride'));
  v_seats integer := greatest(0, coalesce(p_seats_available, 0));
BEGIN
  IF v_uid IS NULL THEN
    RAISE EXCEPTION 'Login required for trip signup';
  END IF;

  IF NOT public.sov_trip_can_read(p_trip_id) THEN
    RAISE EXCEPTION 'Trip is not visible to current user';
  END IF;

  IF v_status NOT IN ('confirmed','maybe','declined','planned','cancelled') THEN
    v_status := 'confirmed';
  END IF;
  IF v_transport NOT IN ('needs_ride','driver','own') THEN
    v_transport := 'needs_ride';
  END IF;
  IF v_transport <> 'driver' THEN
    v_seats := 0;
  END IF;

  SELECT email INTO v_email FROM auth.users WHERE id = v_uid;
  v_name := coalesce(nullif(trim(p_member_name), ''), nullif(split_part(coalesce(v_email, ''), '@', 1), ''), 'Član');
  v_email := coalesce(nullif(trim(p_member_email), ''), v_email, '');

  SELECT id INTO v_existing_id
  FROM public.sov_trip_members
  WHERE trip_id = p_trip_id AND user_id = v_uid
  ORDER BY updated_at DESC NULLS LAST, created_at DESC
  LIMIT 1;

  IF v_existing_id IS NULL THEN
    INSERT INTO public.sov_trip_members(
      trip_id, user_id, member_name, member_email, role, attendance_status, meta
    ) VALUES (
      p_trip_id,
      v_uid,
      v_name,
      v_email,
      CASE WHEN v_transport = 'driver' THEN 'driver' ELSE 'participant' END,
      v_status,
      jsonb_build_object(
        'transport_mode', v_transport,
        'has_car', v_transport = 'driver',
        'seats_available', v_seats,
        'departure_place', coalesce(p_departure_place, ''),
        'note', coalesce(p_note, ''),
        'source', 'web_v5_56_trip_signup'
      )
    ) RETURNING * INTO v_row;
  ELSE
    UPDATE public.sov_trip_members
    SET
      member_name = v_name,
      member_email = v_email,
      role = CASE WHEN v_transport = 'driver' THEN 'driver' ELSE 'participant' END,
      attendance_status = v_status,
      meta = coalesce(meta, '{}'::jsonb) || jsonb_build_object(
        'transport_mode', v_transport,
        'has_car', v_transport = 'driver',
        'seats_available', v_seats,
        'departure_place', coalesce(p_departure_place, ''),
        'note', coalesce(p_note, ''),
        'source', 'web_v5_56_trip_signup'
      ),
      updated_at = now()
    WHERE id = v_existing_id
    RETURNING * INTO v_row;
  END IF;

  RETURN v_row;
END;
$$;

GRANT EXECUTE ON FUNCTION public.sov_trip_signup(uuid,text,text,integer,text,text,text,text) TO authenticated;

-- 3) View za detalje prijava i automatsku tablicu prijevoza.
DROP VIEW IF EXISTS public.sov_trip_members_transport_view;
CREATE VIEW public.sov_trip_members_transport_view
WITH (security_invoker = true) AS
SELECT
  m.id,
  m.trip_id,
  m.user_id,
  m.member_name,
  m.member_email,
  m.role,
  m.attendance_status,
  coalesce(m.meta->>'transport_mode', CASE WHEN m.role = 'driver' THEN 'driver' ELSE '' END) AS transport_mode,
  CASE WHEN lower(coalesce(m.meta->>'has_car','')) IN ('true','1','yes','da') OR m.role = 'driver' THEN true ELSE false END AS has_car,
  coalesce(nullif(m.meta->>'seats_available','')::integer, 0) AS seats_available,
  coalesce(m.meta->>'departure_place', '') AS departure_place,
  coalesce(m.meta->>'note', '') AS note,
  m.created_at,
  m.updated_at
FROM public.sov_trip_members m;

GRANT SELECT ON public.sov_trip_members_transport_view TO authenticated;

-- 4) Quick verification
SELECT 'sov_trip_signup_ready' AS check_name, to_regprocedure('public.sov_trip_signup(uuid,text,text,integer,text,text,text,text)') IS NOT NULL AS ok;
SELECT 'sov_trip_members_transport_view_ready' AS check_name, to_regclass('public.sov_trip_members_transport_view') IS NOT NULL AS ok;
