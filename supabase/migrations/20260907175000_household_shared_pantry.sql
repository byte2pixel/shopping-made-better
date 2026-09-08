-- ============================================================
-- SCRUM-261: household branch on pantry policies and views
-- ============================================================
-- The pantry is the union of members' lots. A lot keeps user_id = the
-- user who added it; members read, update and delete each other's lots
-- and insert only as themselves. inventory_adjustments follows because
-- apply_/undo_inventory_adjustment stamp and read audit rows with the
-- lot owner's id. Side effect: inventory_adjustments_detail (unchanged)
-- now lists housemates' automatic adjustments too, so the digest is
-- household-wide; its quantity and threshold still come from the lot
-- owner. Stock settings, consumption rates and the nightly job stay per
-- user.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- inventory_items. Four policies, not FOR ALL: a FOR ALL WITH CHECK also
-- governs UPDATE, so an owner-only check would reject a housemate's edit.
-- The UPDATE check keeps a lot from being moved outside the household.
-- ------------------------------------------------------------
ALTER TABLE public.inventory_items ALTER COLUMN user_id SET DEFAULT auth.uid();

DROP POLICY "Users manage their own inventory items" ON public.inventory_items;

CREATE POLICY "Household members read lots"
  ON public.inventory_items
  FOR SELECT
  TO authenticated
  USING (user_id IN (SELECT public.household_member_ids()));

CREATE POLICY "Users insert their own lots"
  ON public.inventory_items
  FOR INSERT
  TO authenticated
  WITH CHECK (user_id = (SELECT auth.uid()));

CREATE POLICY "Household members update lots"
  ON public.inventory_items
  FOR UPDATE
  TO authenticated
  USING (user_id IN (SELECT public.household_member_ids()))
  WITH CHECK (user_id IN (SELECT public.household_member_ids()));

CREATE POLICY "Household members delete lots"
  ON public.inventory_items
  FOR DELETE
  TO authenticated
  USING (user_id IN (SELECT public.household_member_ids()));

-- ------------------------------------------------------------
-- inventory_adjustments
-- ------------------------------------------------------------
DROP POLICY "Users manage their own inventory adjustments" ON public.inventory_adjustments;

CREATE POLICY "Household members manage adjustments"
  ON public.inventory_adjustments
  FOR ALL
  TO authenticated
  USING (user_id IN (SELECT public.household_member_ids()))
  WITH CHECK (user_id IN (SELECT public.household_member_ids()));

-- ------------------------------------------------------------
-- pantry_items_by_expire. The stock-settings join is keyed on the caller:
-- a housemate's settings row is hidden by RLS, and the card reads its
-- threshold from the soonest-expiring lot. Consumption stays on the lot
-- owner. addedBy and isOwn are appended. DROP+CREATE (42P16).
-- ------------------------------------------------------------
DROP VIEW IF EXISTS public.pantry_items_by_expire;

CREATE VIEW public.pantry_items_by_expire AS
SELECT
    ii.id                       AS id,
    ii.product_id               AS "productId",
    p.title                     AS name,
    COALESCE(p.brand, '')       AS brand,
    COALESCE(p.description, '') AS description,
    p.package_sizing            AS size,
    ii.quantity::int            AS quantity,
    COALESCE(p.image_url, '')   AS "imageUrl",
    ii.expires_at               AS "expiryDate",
    ii.location                 AS location,
    s.low_stock_threshold       AS "lowStockThreshold",
    EXTRACT(EPOCH FROM ii.last_auto_adjusted_at)::bigint
                                AS "lastAutoAdjustedAtEpoch",
    c.source                    AS "estimateSource",
    la.reason                   AS "lastAdjustmentReason",
    EXTRACT(EPOCH FROM la.created_at)::bigint
                                AS "lastAdjustedAtEpoch",
    la.id                       AS "lastAdjustmentId",
    hm.display_name             AS "addedBy",
    COALESCE(ii.user_id = (SELECT auth.uid()), false)
                                AS "isOwn"
FROM public.inventory_items ii
JOIN public.products p ON p.id = ii.product_id
LEFT JOIN public.user_product_stock_settings s
    ON s.product_id = ii.product_id AND s.user_id = (SELECT auth.uid())
LEFT JOIN public.user_product_consumption c
    ON c.product_id = ii.product_id AND c.user_id = ii.user_id
LEFT JOIN public.household_members hm
    ON hm.id = ii.user_id
LEFT JOIN LATERAL (
    SELECT a.id, a.reason, a.created_at
    FROM public.inventory_adjustments a
    WHERE a.inventory_item_id = ii.id
    ORDER BY a.created_at DESC, a.id DESC
    LIMIT 1
) la ON true
ORDER BY ii.expires_at ASC NULLS LAST;

GRANT SELECT ON public.pantry_items_by_expire TO authenticated;

ALTER VIEW public.pantry_items_by_expire SET (security_invoker = true);

-- ------------------------------------------------------------
-- product_details. Same caller-keyed settings join; quantity becomes
-- household-wide through RLS on inventory_items.
-- ------------------------------------------------------------
DROP VIEW IF EXISTS public.product_details;

CREATE VIEW public.product_details AS
SELECT
    p.id                        AS id,
    p.title                     AS name,
    COALESCE(p.brand, '')       AS brand,
    COALESCE(p.description, '') AS description,
    p.package_sizing            AS size,
    COALESCE(p.image_url, '')   AS "imageUrl",
    COALESCE(inv.quantity, 0)   AS quantity,
    inv."expiryDate"            AS "expiryDate",
    s.low_stock_threshold       AS "lowStockThreshold"
FROM public.products p
LEFT JOIN LATERAL (
    SELECT
        SUM(ii.quantity)::int AS quantity,
        MIN(ii.expires_at)    AS "expiryDate"
    FROM public.inventory_items ii
    WHERE ii.product_id = p.id
) inv ON true
LEFT JOIN public.user_product_stock_settings s
    ON s.product_id = p.id AND s.user_id = (SELECT auth.uid());

GRANT SELECT ON public.product_details TO authenticated;

ALTER VIEW public.product_details SET (security_invoker = true);
