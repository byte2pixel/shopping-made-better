-- ============================================================
-- SCRUM-265: add_inventory_item() — put a product in the pantry by hand
-- ============================================================
-- Until now complete_shopping_trip() was the only insert into inventory_items,
-- so anything bought elsewhere could not be tracked. This RPC keeps that same
-- insert shape — unit from products.pricing_unit, purchased_at = today — so
-- trg_inventory_set_expiry and trg_inventory_set_location still fill expires_at
-- and the storage location, and the client never has to know the unit.
--
-- SECURITY INVOKER is enough: products is readable by authenticated and the
-- insert runs under "Users insert their own lots", which is owner-only. Passing
-- 'pantry' explicitly still lets the location trigger derive fridge/freezer,
-- because it only overrides when the value is 'pantry'.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.add_inventory_item(
  p_product_id uuid,
  p_quantity   numeric,
  p_location   text DEFAULT NULL
)
RETURNS uuid
LANGUAGE plpgsql SECURITY INVOKER SET search_path = public AS $$
DECLARE
  v_uid  uuid := auth.uid();
  v_unit text;
  v_id   uuid;
BEGIN
  IF v_uid IS NULL THEN RAISE EXCEPTION 'not signed in'; END IF;
  IF p_quantity IS NULL OR p_quantity <= 0 THEN RAISE EXCEPTION 'quantity must be positive'; END IF;
  IF p_location IS NOT NULL AND p_location NOT IN ('pantry', 'fridge', 'freezer') THEN
    RAISE EXCEPTION 'invalid location';
  END IF;

  SELECT p.pricing_unit INTO v_unit FROM public.products p WHERE p.id = p_product_id;
  IF v_unit IS NULL THEN RAISE EXCEPTION 'product not found'; END IF;

  INSERT INTO public.inventory_items (user_id, product_id, quantity, unit, location, purchased_at)
  VALUES (v_uid, p_product_id, p_quantity, v_unit, COALESCE(p_location, 'pantry'), current_date)
  RETURNING inventory_items.id INTO v_id;

  RETURN v_id;
END;
$$;

REVOKE EXECUTE ON FUNCTION public.add_inventory_item(uuid, numeric, text) FROM PUBLIC, anon;
GRANT EXECUTE ON FUNCTION public.add_inventory_item(uuid, numeric, text) TO authenticated;
