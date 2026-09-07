-- ============================================================
-- SCRUM-260: household membership
-- ============================================================
-- household_member_ids() is the only membership lookup; policies call it.
-- profiles SELECT stays self-only; household_members is the only view of
-- a housemate. household_id and is_household_head change only through the
-- RPCs. A head cannot leave while other members remain; the last member
-- out deletes the household.
-- ------------------------------------------------------------

-- join_household matches on upper(btrim(code)).
ALTER TABLE public.households
  ADD COLUMN invite_code text NOT NULL UNIQUE
    DEFAULT upper(substr(md5(gen_random_uuid()::text), 1, 8));

-- ------------------------------------------------------------
-- The caller plus everyone with the same household_id. SECURITY DEFINER
-- because a profiles policy that reads profiles recurses (42P17).
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.household_member_ids()
RETURNS SETOF uuid
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT auth.uid()
  UNION
  SELECT p.id
  FROM public.profiles p
  WHERE p.household_id IS NOT NULL
    AND p.household_id = (SELECT me.household_id FROM public.profiles me
                          WHERE me.id = auth.uid());
$$;

REVOKE EXECUTE ON FUNCTION public.household_member_ids() FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.household_member_ids() TO authenticated;

-- ------------------------------------------------------------
-- RPCs. Error messages are fixed lowercase tokens the app matches on.
-- Column references are alias-qualified because the RETURNS TABLE names
-- shadow them.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.create_household(p_name text)
RETURNS TABLE (id uuid, name text, invite_code text)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid     uuid := auth.uid();
  v_id      uuid;
  v_attempt int  := 0;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  IF p_name IS NULL OR btrim(p_name) = '' THEN RAISE EXCEPTION 'name required'; END IF;
  IF EXISTS (SELECT 1 FROM public.profiles p
             WHERE p.id = v_uid AND p.household_id IS NOT NULL) THEN
    RAISE EXCEPTION 'already in a household';
  END IF;

  -- Retry an invite_code collision.
  LOOP
    BEGIN
      INSERT INTO public.households (name) VALUES (btrim(p_name))
      RETURNING households.id INTO v_id;
      EXIT;
    EXCEPTION WHEN unique_violation THEN
      v_attempt := v_attempt + 1;
      IF v_attempt >= 5 THEN RAISE; END IF;
    END;
  END LOOP;

  UPDATE public.profiles p
     SET household_id = v_id, is_household_head = true
   WHERE p.id = v_uid;

  RETURN QUERY SELECT h.id, h.name, h.invite_code
                 FROM public.households h WHERE h.id = v_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.join_household(p_invite_code text)
RETURNS TABLE (id uuid, name text, invite_code text)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_id  uuid;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  IF EXISTS (SELECT 1 FROM public.profiles p
             WHERE p.id = v_uid AND p.household_id IS NOT NULL) THEN
    RAISE EXCEPTION 'already in a household';
  END IF;

  SELECT h.id INTO v_id FROM public.households h
   WHERE h.invite_code = upper(btrim(coalesce(p_invite_code, '')));
  IF v_id IS NULL THEN RAISE EXCEPTION 'invalid invite code'; END IF;

  UPDATE public.profiles p
     SET household_id = v_id, is_household_head = false
   WHERE p.id = v_uid;

  RETURN QUERY SELECT h.id, h.name, h.invite_code
                 FROM public.households h WHERE h.id = v_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.leave_household()
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid    uuid := auth.uid();
  v_hid    uuid;
  v_head   boolean;
  v_others int;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  SELECT p.household_id, p.is_household_head INTO v_hid, v_head
    FROM public.profiles p WHERE p.id = v_uid;
  IF v_hid IS NULL THEN RAISE EXCEPTION 'not in a household'; END IF;

  SELECT count(*) INTO v_others FROM public.profiles p
   WHERE p.household_id = v_hid AND p.id <> v_uid;
  IF v_head AND v_others > 0 THEN
    RAISE EXCEPTION 'head cannot leave while other members remain';
  END IF;

  UPDATE public.profiles p
     SET household_id = NULL, is_household_head = false
   WHERE p.id = v_uid;
  IF v_others = 0 THEN DELETE FROM public.households h WHERE h.id = v_hid; END IF;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.create_household(text) FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.join_household(text)   FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.leave_household()      FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.create_household(text) TO authenticated;
GRANT EXECUTE ON FUNCTION public.join_household(text)   TO authenticated;
GRANT EXECUTE ON FUNCTION public.leave_household()      TO authenticated;

-- ------------------------------------------------------------
-- households: members read their row, the head renames it. INSERT and
-- DELETE go through the RPCs. invite_code is read-only from the client.
-- ------------------------------------------------------------
GRANT SELECT ON public.households TO authenticated;
GRANT UPDATE (name) ON public.households TO authenticated;

CREATE POLICY "Members can read their household"
  ON public.households
  FOR SELECT
  TO authenticated
  USING (id = (SELECT p.household_id FROM public.profiles p
               WHERE p.id = (SELECT auth.uid())));

CREATE POLICY "Head can rename the household"
  ON public.households
  FOR UPDATE
  TO authenticated
  USING (id = (SELECT p.household_id FROM public.profiles p
               WHERE p.id = (SELECT auth.uid()) AND p.is_household_head))
  WITH CHECK (id = (SELECT p.household_id FROM public.profiles p
                    WHERE p.id = (SELECT auth.uid()) AND p.is_household_head));

-- ------------------------------------------------------------
-- profiles: membership columns are RPC-only. A new client-writable column
-- is added here.
-- ------------------------------------------------------------
REVOKE UPDATE ON public.profiles FROM authenticated;
GRANT UPDATE (display_name, avatar_url, preferred_store_id,
              dietary_preferences, category_preferences, primary_goal,
              auto_adjust_enabled)
  ON public.profiles TO authenticated;

-- ------------------------------------------------------------
-- One row per member of the caller's household. Not security_invoker;
-- the helper filter is the boundary.
-- ------------------------------------------------------------
CREATE VIEW public.household_members AS
SELECT
  p.id,
  p.display_name,
  p.is_household_head,
  (p.id = auth.uid()) AS is_self
FROM public.profiles p
WHERE p.id IN (SELECT public.household_member_ids());

GRANT SELECT ON public.household_members TO authenticated;
REVOKE SELECT ON public.household_members FROM anon;
