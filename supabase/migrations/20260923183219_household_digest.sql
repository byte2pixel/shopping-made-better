-- ============================================================
-- SCRUM-291: household digest
-- ============================================================
-- inventory_adjustments.actor_id records who called the RPC; user_id stays
-- the lot owner. The cron and the seed run as postgres, so their rows get
-- NULL. inventory_adjustments_detail keys its threshold and total to the
-- viewer when there is a JWT and to the lot owner when there is none, so
-- the seed's postgres read is unchanged, and appends lotOwner and isOwn
-- for the person chip. The consumption join stays on the lot owner: the
-- rate belongs to whose purchases produced it.
-- ------------------------------------------------------------

ALTER TABLE public.inventory_adjustments
  ADD COLUMN actor_id uuid REFERENCES public.profiles(id) ON DELETE SET NULL;

-- Same signature and return type as 20260904101016, so OR REPLACE is allowed;
-- only the audit INSERT changes.
CREATE OR REPLACE FUNCTION public.apply_inventory_adjustment(
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

  INSERT INTO public.inventory_adjustments (inventory_item_id, user_id, actor_id, delta, reason)
  VALUES (p_inventory_item_id, v_user_id, auth.uid(), v_new - v_old, p_reason)
  RETURNING id INTO v_adjustment_id;

  RETURN QUERY SELECT p_inventory_item_id, v_new - v_old, v_new, v_adjustment_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.apply_inventory_adjustment(uuid, numeric, text)
  TO authenticated;

-- The column list changes, so DROP + CREATE (42P16).
DROP VIEW IF EXISTS public.inventory_adjustments_detail;

CREATE VIEW public.inventory_adjustments_detail AS
SELECT
    a.id                        AS "adjustmentId",
    a.inventory_item_id         AS "inventoryItemId",
    ii.product_id               AS "productId",
    p.title                     AS "productName",
    COALESCE(p.image_url, '')   AS "imageUrl",
    a.delta::int                AS delta,
    ii.quantity::int            AS "quantityNow",
    tot.quantity::int           AS "productQuantity",
    s.low_stock_threshold       AS "lowStockThreshold",
    c.source                    AS "estimateSource",
    EXTRACT(EPOCH FROM a.created_at)::bigint AS "createdAtEpoch",
    hm.display_name             AS "lotOwner",
    COALESCE(a.user_id = (SELECT auth.uid()), false) AS "isOwn"
FROM public.inventory_adjustments a
JOIN public.inventory_items ii ON ii.id = a.inventory_item_id
JOIN public.products p        ON p.id = ii.product_id
LEFT JOIN public.user_product_stock_settings s
    ON s.product_id = ii.product_id
   AND s.user_id = COALESCE((SELECT auth.uid()), a.user_id)
LEFT JOIN public.user_product_consumption c
    ON c.product_id = ii.product_id AND c.user_id = a.user_id
LEFT JOIN public.household_members hm ON hm.id = a.user_id
LEFT JOIN LATERAL (
    SELECT SUM(x.quantity) AS quantity
    FROM public.inventory_items x
    WHERE x.product_id = ii.product_id
      AND (x.user_id = a.user_id OR x.user_id IN (SELECT public.household_member_ids()))
) tot ON true
WHERE a.reason = 'auto'
  AND a.created_at >= now() - interval '7 days'
  AND NOT EXISTS (
        SELECT 1 FROM public.inventory_adjustments r
        WHERE r.reverses = a.id)
  AND NOT EXISTS (
        SELECT 1 FROM public.inventory_adjustments l
        WHERE l.inventory_item_id = a.inventory_item_id
          AND l.reason IN ('confirmed', 'manual')
          AND (l.created_at, l.id) > (a.created_at, a.id))
ORDER BY a.created_at DESC, a.id DESC;

-- security_invoker so RLS on inventory_adjustments scopes the rows to the
-- caller's household. With no JWT auth.uid() is NULL and household_member_ids()
-- yields {NULL}, so the settings row and the total fall back to the owner's.
ALTER VIEW public.inventory_adjustments_detail SET (security_invoker = true);

GRANT SELECT ON public.inventory_adjustments_detail TO authenticated;
REVOKE SELECT ON public.inventory_adjustments_detail FROM anon;
