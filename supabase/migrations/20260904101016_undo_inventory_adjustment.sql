-- ============================================================
-- SCRUM-222: undo an automatic inventory adjustment
-- ============================================================
-- inventory_adjustments.reverses links an 'undo' row to the row it
-- reversed; the partial unique index allows one undo per row.
-- apply_inventory_adjustment now returns the audit row's id and resets
-- pending_fraction for 'undo' as well. undo_inventory_adjustment reverses
-- one 'auto' row by applying its negated delta through that function.
-- ------------------------------------------------------------
ALTER TABLE public.inventory_adjustments
  ADD COLUMN reverses uuid REFERENCES public.inventory_adjustments(id);

CREATE UNIQUE INDEX idx_adjustments_reverses
  ON public.inventory_adjustments (reverses)
  WHERE reverses IS NOT NULL;

-- The return type changes, which CREATE OR REPLACE rejects (42P13).
DROP FUNCTION public.apply_inventory_adjustment(uuid, numeric, text);

CREATE FUNCTION public.apply_inventory_adjustment(
  p_inventory_item_id uuid,
  p_delta             numeric,
  p_reason            text
)
RETURNS TABLE (
  inventory_item_id uuid,
  delta             numeric,
  new_quantity      numeric,
  adjustment_id     uuid
)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  v_user_id       uuid;
  v_old           numeric;
  v_new           numeric;
  v_adjustment_id uuid;
BEGIN
  -- greatest(0, q + NULL) is 0, so a NULL delta would silently zero the lot.
  IF p_delta IS NULL THEN
    RAISE EXCEPTION 'p_delta must not be null';
  END IF;

  -- Column references stay table-qualified: the RETURNS TABLE names
  -- shadow as plpgsql variables.
  SELECT ii.user_id, ii.quantity INTO v_user_id, v_old
  FROM public.inventory_items ii
  WHERE ii.id = p_inventory_item_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Inventory item % not found or not accessible', p_inventory_item_id;
  END IF;

  v_new := round(greatest(0, v_old + p_delta), 3);

  UPDATE public.inventory_items ii
     SET quantity              = v_new,
         last_auto_adjusted_at = now(),
         pending_fraction      = CASE WHEN p_reason IN ('confirmed', 'manual', 'undo')
                                      THEN 0 ELSE ii.pending_fraction END
   WHERE ii.id = p_inventory_item_id;

  INSERT INTO public.inventory_adjustments (inventory_item_id, user_id, delta, reason)
  VALUES (p_inventory_item_id, v_user_id, v_new - v_old, p_reason)
  RETURNING id INTO v_adjustment_id;

  RETURN QUERY SELECT p_inventory_item_id, v_new - v_old, v_new, v_adjustment_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.apply_inventory_adjustment(uuid, numeric, text)
  TO authenticated;

CREATE FUNCTION public.undo_inventory_adjustment(p_adjustment_id uuid)
RETURNS TABLE (
  inventory_item_id uuid,
  delta             numeric,
  new_quantity      numeric,
  adjustment_id     uuid
)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  v_lot    uuid;
  v_delta  numeric;
  v_reason text;
  v_result record;
BEGIN
  -- Column references stay table-qualified: the RETURNS TABLE names
  -- shadow as plpgsql variables. RLS hides other users' rows.
  SELECT a.inventory_item_id, a.delta, a.reason INTO v_lot, v_delta, v_reason
  FROM public.inventory_adjustments a
  WHERE a.id = p_adjustment_id
  FOR UPDATE;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Adjustment % not found or not accessible', p_adjustment_id;
  END IF;

  IF v_reason <> 'auto' THEN
    RAISE EXCEPTION 'Adjustment % is not an automatic adjustment', p_adjustment_id;
  END IF;

  IF EXISTS (SELECT 1 FROM public.inventory_adjustments r
             WHERE r.reverses = p_adjustment_id) THEN
    RAISE EXCEPTION 'Adjustment % already undone', p_adjustment_id;
  END IF;

  SELECT * INTO v_result
  FROM public.apply_inventory_adjustment(v_lot, -v_delta, 'undo');

  UPDATE public.inventory_adjustments u
     SET reverses = p_adjustment_id
   WHERE u.id = v_result.adjustment_id;

  RETURN QUERY SELECT v_result.inventory_item_id, v_result.delta,
                      v_result.new_quantity, v_result.adjustment_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.undo_inventory_adjustment(uuid) TO authenticated;
