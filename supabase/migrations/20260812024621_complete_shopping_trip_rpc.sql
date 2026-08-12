-- ============================================================
-- Migration: complete_shopping_trip RPC
-- ============================================================
-- Atomically converts a shopping list into a completed trip:
--   1. a purchase_history header (one row per trip),
--   2. purchase_history_items line items (price_paid from current pricing),
--   3. inventory_items (pantry) rows for items flagged add_to_inventory,
--   4. then clears the list's items (the reusable shopping_lists row is kept).
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.complete_shopping_trip(p_list_id uuid)
RETURNS uuid
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  v_user_id     uuid := auth.uid();
  v_store_id    uuid;
  v_purchase_id uuid;
  v_item_count  int;
BEGIN
  -- List must be visible to the caller (RLS enforces ownership); grab its store.
  SELECT store_id INTO v_store_id
  FROM public.shopping_lists
  WHERE id = p_list_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'Shopping list % not found or not accessible', p_list_id;
  END IF;

  SELECT count(*) INTO v_item_count
  FROM public.shopping_list_items
  WHERE shopping_list_id = p_list_id;

  IF v_item_count = 0 THEN
    RAISE EXCEPTION 'Shopping list % has no items to purchase', p_list_id;
  END IF;

  -- 1) Purchase-history header. total_amount = sum(quantity * current price).
  INSERT INTO public.purchase_history (user_id, store_id, purchased_at, total_amount)
  SELECT
    v_user_id,
    v_store_id,
    now(),
    COALESCE(SUM(sli.quantity * spp.price), 0)
  FROM public.shopping_list_items sli
  LEFT JOIN public.store_product_pricing spp
    ON spp.product_id = sli.product_id
   AND spp.store_id   = v_store_id
   AND spp.is_current = true
  WHERE sli.shopping_list_id = p_list_id
  RETURNING id INTO v_purchase_id;

  -- 2) Purchase-history line items (price_paid from current pricing, 0 if none).
  INSERT INTO public.purchase_history_items
    (purchase_id, product_id, quantity, price_paid, added_to_inventory)
  SELECT
    v_purchase_id,
    sli.product_id,
    sli.quantity,
    COALESCE(spp.price, 0),
    sli.add_to_inventory
  FROM public.shopping_list_items sli
  LEFT JOIN public.store_product_pricing spp
    ON spp.product_id = sli.product_id
   AND spp.store_id   = v_store_id
   AND spp.is_current = true
  WHERE sli.shopping_list_id = p_list_id;

  -- 3) Pantry rows for items flagged add_to_inventory. unit <- products.pricing_unit.
  --    expires_at + location are filled by existing BEFORE INSERT triggers.
  INSERT INTO public.inventory_items
    (user_id, product_id, quantity, unit, purchased_at)
  SELECT
    v_user_id,
    sli.product_id,
    sli.quantity,
    p.pricing_unit,
    current_date
  FROM public.shopping_list_items sli
  JOIN public.products p ON p.id = sli.product_id
  WHERE sli.shopping_list_id = p_list_id
    AND sli.add_to_inventory = true;

  -- 4) Clear the list's items; keep the (reusable) list row.
  DELETE FROM public.shopping_list_items
  WHERE shopping_list_id = p_list_id;

  RETURN v_purchase_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.complete_shopping_trip(uuid) TO authenticated;