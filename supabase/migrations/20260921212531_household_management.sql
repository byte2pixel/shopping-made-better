-- ============================================================
-- SCRUM-292: household management
-- ============================================================
-- Three head-only RPCs: hand the head role to another member, remove a
-- member, and replace the invite code. Same shape as the SCRUM-260 RPCs:
-- SECURITY DEFINER with search_path pinned, fixed lowercase error tokens
-- the app matches on, alias-qualified columns. Removing a member clears
-- only their two profile columns; their lots, lists and trips stay theirs
-- and drop out of the household's view, as with leave_household.
-- ------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.transfer_household_head(p_member uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid  uuid := auth.uid();
  v_hid  uuid;
  v_head boolean;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  SELECT p.household_id, p.is_household_head INTO v_hid, v_head
    FROM public.profiles p WHERE p.id = v_uid;
  IF v_hid IS NULL THEN RAISE EXCEPTION 'not in a household'; END IF;
  IF NOT v_head THEN RAISE EXCEPTION 'not the head'; END IF;
  IF p_member IS NULL OR p_member = v_uid
     OR NOT EXISTS (SELECT 1 FROM public.profiles p
                    WHERE p.id = p_member AND p.household_id = v_hid) THEN
    RAISE EXCEPTION 'member not in household';
  END IF;

  UPDATE public.profiles p
     SET is_household_head = (p.id = p_member)
   WHERE p.household_id = v_hid AND p.id IN (v_uid, p_member);
END;
$$;

CREATE OR REPLACE FUNCTION public.remove_household_member(p_member uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid  uuid := auth.uid();
  v_hid  uuid;
  v_head boolean;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  SELECT p.household_id, p.is_household_head INTO v_hid, v_head
    FROM public.profiles p WHERE p.id = v_uid;
  IF v_hid IS NULL THEN RAISE EXCEPTION 'not in a household'; END IF;
  IF NOT v_head THEN RAISE EXCEPTION 'not the head'; END IF;
  IF p_member = v_uid THEN RAISE EXCEPTION 'cannot remove yourself'; END IF;
  IF p_member IS NULL
     OR NOT EXISTS (SELECT 1 FROM public.profiles p
                    WHERE p.id = p_member AND p.household_id = v_hid) THEN
    RAISE EXCEPTION 'member not in household';
  END IF;

  UPDATE public.profiles p
     SET household_id = NULL, is_household_head = false
   WHERE p.id = p_member AND p.household_id = v_hid;
END;
$$;

-- Reuses the column default, so the code keeps its eight-character
-- upper-case hex format; the collision retry matches create_household.
CREATE OR REPLACE FUNCTION public.regenerate_invite_code()
RETURNS text
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_uid     uuid := auth.uid();
  v_hid     uuid;
  v_head    boolean;
  v_code    text;
  v_attempt int := 0;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  SELECT p.household_id, p.is_household_head INTO v_hid, v_head
    FROM public.profiles p WHERE p.id = v_uid;
  IF v_hid IS NULL THEN RAISE EXCEPTION 'not in a household'; END IF;
  IF NOT v_head THEN RAISE EXCEPTION 'not the head'; END IF;

  LOOP
    BEGIN
      UPDATE public.households h SET invite_code = DEFAULT
       WHERE h.id = v_hid
       RETURNING h.invite_code INTO v_code;
      EXIT;
    EXCEPTION WHEN unique_violation THEN
      v_attempt := v_attempt + 1;
      IF v_attempt >= 5 THEN RAISE; END IF;
    END;
  END LOOP;

  RETURN v_code;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.transfer_household_head(uuid)  FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.remove_household_member(uuid)  FROM PUBLIC, anon;
REVOKE EXECUTE ON FUNCTION public.regenerate_invite_code()       FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.transfer_household_head(uuid)  TO authenticated;
GRANT EXECUTE ON FUNCTION public.remove_household_member(uuid)  TO authenticated;
GRANT EXECUTE ON FUNCTION public.regenerate_invite_code()       TO authenticated;
