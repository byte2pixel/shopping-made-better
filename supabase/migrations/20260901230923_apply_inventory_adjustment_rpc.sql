-- ============================================================
-- Migration: apply_inventory_adjustment RPC
-- ============================================================
-- The app-facing per-lot write path for pantry quantity changes that
-- must leave an audit trail: confirm-on-open corrections (SCRUM-220,
-- 'confirmed'), the zero/low-stock gate (SCRUM-223, 'confirmed') and
-- undo (SCRUM-222/224, 'undo'). Automatic 'auto' rows are written by
-- apply_consumption_adjustments (20260901061706) from the nightly cron
-- job, which is set-based and does not call this function.
--
-- In one transaction:
--   quantity = greatest(0, quantity + p_delta). Floored at zero, row
--              never deleted.
--   stamp    = last_auto_adjusted_at = now(), so the nightly job accrues
--              from this observation instead of re-applying consumption
--              estimated before it.
--   pending  = reset to 0 for 'confirmed' and 'manual': a quantity the
--              user observed supersedes the fraction accrued before they
--              looked. Untouched for 'undo', 'dismissed' and 'auto'.
--   audit    = one inventory_adjustments row per call holding the
--              EFFECTIVE delta (after the floor), so undo can restore
--              exactly. A zero effective delta still writes the row: a
--              'confirmed' with no change is a real signal.
-- No elapsed-time guard: p_delta is explicit, so two calls apply twice.
--
-- SECURITY INVOKER: the lot is read under RLS, so another user's lot is
-- simply not found. The audit user_id comes from the lot rather than
-- auth.uid(), so the function also runs as postgres. p_reason is
-- validated by the inventory_adjustments CHECK constraint.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.apply_inventory_adjustment(
  p_inventory_item_id uuid,
  p_delta             numeric,
  p_reason            text
)
RETURNS TABLE (
  inventory_item_id uuid,
  delta             numeric,
  new_quantity      numeric
)
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  v_user_id uuid;
  v_old     numeric;
  v_new     numeric;
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
         pending_fraction      = CASE WHEN p_reason IN ('confirmed', 'manual')
                                      THEN 0 ELSE ii.pending_fraction END
   WHERE ii.id = p_inventory_item_id;

  INSERT INTO public.inventory_adjustments (inventory_item_id, user_id, delta, reason)
  VALUES (p_inventory_item_id, v_user_id, v_new - v_old, p_reason);

  RETURN QUERY SELECT p_inventory_item_id, v_new - v_old, v_new;
END;
$$;

GRANT EXECUTE ON FUNCTION public.apply_inventory_adjustment(uuid, numeric, text)
  TO authenticated;
