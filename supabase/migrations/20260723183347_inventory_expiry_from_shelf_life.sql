-- ============================================================
-- Migration: derive inventory expiry from product shelf life
-- ============================================================
-- When an item is added to a user's pantry we can estimate when it will
-- go bad as:  expires_at = purchased_at + products.shelf_life_days
-- (shelf_life_days comes from the keyword classifier -- see
-- scripts/shelf_life.py, migration 20260720212327_add_shelf_life_to_products).
--
-- This trigger fills expires_at automatically so callers only have to record
-- WHEN something was bought. It is deliberately non-destructive:
--   * Runs only when expires_at was NOT supplied, an explicit value always wins.
--   * Needs a purchased_at to start from, without one it leaves expires_at NULL.
--   * Products with no shelf life non-food / unclassified have shelf_life_days
--     IS NULL) get no expiry, which is correct.
--
-- SECURITY DEFINER so the lookup against products.shelf_life_days succeeds
-- regardless of the caller's RLS context (shelf life is non-sensitive
-- ------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.set_inventory_expiry()
RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path = public AS $$
DECLARE
  v_shelf_life_days int;
BEGIN
  IF NEW.expires_at IS NULL AND NEW.purchased_at IS NOT NULL THEN
    SELECT shelf_life_days
      INTO v_shelf_life_days
      FROM public.products
     WHERE id = NEW.product_id;

    IF v_shelf_life_days IS NOT NULL THEN
      NEW.expires_at := NEW.purchased_at + v_shelf_life_days;
    END IF;
  END IF;

  RETURN NEW;
END;
$$;

-- Fire on INSERT and on UPDATE: an UPDATE that sets/changes purchased_at while
-- leaving expires_at NULL will recompute the expire.
CREATE TRIGGER trg_inventory_set_expiry
  BEFORE INSERT OR UPDATE ON public.inventory_items
  FOR EACH ROW EXECUTE FUNCTION public.set_inventory_expiry();