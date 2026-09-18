-- ============================================================
-- SCRUM-288: household branch on purchase history policies and views
-- ============================================================
-- Members read every member's trips, items and spend, as they do the
-- pantry and the lists. Every write stays with the buyer, and
-- complete_shopping_trip keeps stamping whoever completes the list.
-- estimate_consumption_rates() is guarded to the caller: with history
-- readable across the household it would group housemates' trips and
-- fail on user_product_consumption's owner-only WITH CHECK. All four
-- history views are recreated so a later Mine / Household switch is
-- app-only work.
-- ------------------------------------------------------------

-- ------------------------------------------------------------
-- purchase_history. Only SELECT widens; the default matches
-- inventory_items and shopping_lists (the RPC and the seed still pass
-- user_id explicitly).
-- ------------------------------------------------------------
ALTER TABLE public.purchase_history ALTER COLUMN user_id SET DEFAULT auth.uid();

DROP POLICY "Users manage their own purchase history" ON public.purchase_history;

CREATE POLICY "Household members read trips"
  ON public.purchase_history
  FOR SELECT
  TO authenticated
  USING (user_id IN (SELECT public.household_member_ids()));

CREATE POLICY "Users insert their own trips"
  ON public.purchase_history
  FOR INSERT
  TO authenticated
  WITH CHECK (user_id = (SELECT auth.uid()));

CREATE POLICY "Users update their own trips"
  ON public.purchase_history
  FOR UPDATE
  TO authenticated
  USING (user_id = (SELECT auth.uid()))
  WITH CHECK (user_id = (SELECT auth.uid()));

CREATE POLICY "Users delete their own trips"
  ON public.purchase_history
  FOR DELETE
  TO authenticated
  USING (user_id = (SELECT auth.uid()));

-- ------------------------------------------------------------
-- purchase_history_items. Ownership via the parent trip; the member
-- check sits inside the EXISTS so it does not depend on the parent's
-- SELECT policy.
-- ------------------------------------------------------------
DROP POLICY "Users manage items in their own purchases" ON public.purchase_history_items;

CREATE POLICY "Household members read trip items"
  ON public.purchase_history_items
  FOR SELECT
  TO authenticated
  USING (EXISTS (
    SELECT 1 FROM public.purchase_history ph
    WHERE ph.id = purchase_history_items.purchase_id
      AND ph.user_id IN (SELECT public.household_member_ids())));

CREATE POLICY "Users insert items in their own trips"
  ON public.purchase_history_items
  FOR INSERT
  TO authenticated
  WITH CHECK (EXISTS (
    SELECT 1 FROM public.purchase_history ph
    WHERE ph.id = purchase_history_items.purchase_id
      AND ph.user_id = (SELECT auth.uid())));

CREATE POLICY "Users update items in their own trips"
  ON public.purchase_history_items
  FOR UPDATE
  TO authenticated
  USING (EXISTS (
    SELECT 1 FROM public.purchase_history ph
    WHERE ph.id = purchase_history_items.purchase_id
      AND ph.user_id = (SELECT auth.uid())))
  WITH CHECK (EXISTS (
    SELECT 1 FROM public.purchase_history ph
    WHERE ph.id = purchase_history_items.purchase_id
      AND ph.user_id = (SELECT auth.uid())));

CREATE POLICY "Users delete items in their own trips"
  ON public.purchase_history_items
  FOR DELETE
  TO authenticated
  USING (EXISTS (
    SELECT 1 FROM public.purchase_history ph
    WHERE ph.id = purchase_history_items.purchase_id
      AND ph.user_id = (SELECT auth.uid())));

-- ------------------------------------------------------------
-- estimate_consumption_rates(). SECURITY INVOKER: purchase_history SELECT
-- is now household-wide, so the WHERE keeps an app caller to their own
-- pairs. postgres and the cron see auth.uid() NULL and compute everyone.
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.estimate_consumption_rates()
RETURNS integer
LANGUAGE plpgsql
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  v_upserted integer;
  v_uid      uuid := auth.uid();   -- NULL for postgres and the cron: compute everyone
BEGIN
  WITH pair_stats AS (
    SELECT
      ph.user_id,
      phi.product_id,
      count(DISTINCT phi.purchase_id)                               AS trips,
      sum(phi.quantity)                                             AS total_qty,
      extract(day from max(ph.purchased_at) - min(ph.purchased_at)) AS span_days,
      p.shelf_life_days
    FROM public.purchase_history_items phi
    JOIN public.purchase_history ph ON ph.id = phi.purchase_id
    JOIN public.products p          ON p.id  = phi.product_id
    WHERE p.pricing_unit = 'ea'  -- skip weight-priced products
      AND (v_uid IS NULL OR ph.user_id = v_uid)
    GROUP BY ph.user_id, phi.product_id, p.shelf_life_days
  ),
  rates AS (
    SELECT
      user_id,
      product_id,
      CASE
        WHEN trips >= 2 AND span_days >= 14 THEN
          least(1.0, greatest(
            (total_qty - total_qty / trips) / span_days,
            coalesce(1.0 / shelf_life_days, 0)))
        -- least() ignores nulls, so a missing shelf life needs its own branch
        WHEN shelf_life_days IS NOT NULL THEN least(1.0, 1.0 / shelf_life_days)
        ELSE NULL
      END AS est_daily_rate,
      CASE
        WHEN trips >= 2 AND span_days >= 14 THEN 'history'
        ELSE 'shelf_life'
      END AS source,
      CASE
        WHEN trips >= 2 AND span_days >= 14 THEN least(0.90, trips::numeric / 10)
        ELSE 0.10
      END AS confidence
    FROM pair_stats
  )
  INSERT INTO public.user_product_consumption
    (user_id, product_id, est_daily_rate, source, confidence, last_computed_at)
  SELECT user_id, product_id, round(est_daily_rate, 4), source, confidence, now()
  FROM rates
  WHERE est_daily_rate IS NOT NULL
  ON CONFLICT (user_id, product_id) DO UPDATE SET
    est_daily_rate   = excluded.est_daily_rate,
    source           = excluded.source,
    confidence       = excluded.confidence,
    last_computed_at = excluded.last_computed_at
  WHERE user_product_consumption.source <> 'manual';

  GET DIAGNOSTICS v_upserted = ROW_COUNT;
  RETURN v_upserted;
END;
$$;

GRANT EXECUTE ON FUNCTION public.estimate_consumption_rates() TO authenticated;

-- ------------------------------------------------------------
-- Views. Each is DROP+CREATE (42P16) with security_invoker, the
-- authenticated grant and the anon revoke restated. household_members
-- runs as owner, so a housemate's name is visible without widening
-- profiles; it returns no rows without a JWT, so "purchasedBy" is NULL
-- for postgres.
-- ------------------------------------------------------------

-- purchase_history_summary: "purchasedBy" and "isOwn" appended.
DROP VIEW IF EXISTS public.purchase_history_summary;

CREATE VIEW public.purchase_history_summary AS
SELECT
    ph.id                                        AS id,
    ph.purchased_at::date                        AS "purchasedOn",
    EXTRACT(EPOCH FROM ph.purchased_at)::bigint  AS "purchasedAtEpoch",
    ph.store_id                                  AS "storeId",
    s.name                                       AS "storeName",
    ph.total_amount                              AS "totalAmount",
    COALESCE(items.line_total, 0)                AS "lineTotal",
    COALESCE(items.item_count, 0)::int           AS "itemCount",
    COALESCE(items.product_search, '')           AS "productSearch",
    hm.display_name                              AS "purchasedBy",
    COALESCE(ph.user_id = (SELECT auth.uid()), false) AS "isOwn"
FROM public.purchase_history ph
LEFT JOIN public.stores s ON s.id = ph.store_id
LEFT JOIN public.household_members hm ON hm.id = ph.user_id
LEFT JOIN LATERAL (
    SELECT
        COUNT(*)                                AS item_count,
        SUM(phi.quantity * phi.price_paid)      AS line_total,
        -- concat_ws drops a null brand; the LEFT join keeps an unresolved
        -- product's item in item_count.
        string_agg(concat_ws(' ', p.title, p.brand), ' ') AS product_search
    FROM public.purchase_history_items phi
    LEFT JOIN public.products p ON p.id = phi.product_id
    WHERE phi.purchase_id = ph.id
) items ON true;

ALTER VIEW public.purchase_history_summary SET (security_invoker = true);
GRANT SELECT ON public.purchase_history_summary TO authenticated;
REVOKE SELECT ON public.purchase_history_summary FROM anon;

-- purchase_history_detail: "storeId" (SCRUM-264), "purchasedBy" and
-- "isOwn" added.
DROP VIEW IF EXISTS public.purchase_history_detail;

CREATE VIEW public.purchase_history_detail AS
SELECT
    phi.id                                       AS id,
    ph.id                                        AS "purchaseId",
    ph.purchased_at::date                        AS "purchasedOn",
    EXTRACT(EPOCH FROM ph.purchased_at)::bigint  AS "purchasedAtEpoch",
    ph.store_id                                  AS "storeId",
    s.name                                       AS "storeName",
    ph.total_amount                              AS "totalAmount",
    phi.product_id                               AS "productId",
    p.title                                      AS "productName",
    COALESCE(p.brand, '')                        AS brand,
    p.package_sizing                             AS size,
    COALESCE(p.image_url, '')                    AS "imageUrl",
    phi.quantity                                 AS quantity,
    phi.price_paid                               AS "pricePaid",
    phi.added_to_inventory                       AS "addedToInventory",
    hm.display_name                              AS "purchasedBy",
    COALESCE(ph.user_id = (SELECT auth.uid()), false) AS "isOwn"
FROM public.purchase_history ph
JOIN public.purchase_history_items phi ON phi.purchase_id = ph.id
JOIN public.products p                 ON p.id = phi.product_id
LEFT JOIN public.stores s              ON s.id = ph.store_id
LEFT JOIN public.household_members hm  ON hm.id = ph.user_id
ORDER BY ph.purchased_at DESC, phi.created_at ASC;

ALTER VIEW public.purchase_history_detail SET (security_invoker = true);
GRANT SELECT ON public.purchase_history_detail TO authenticated;
REVOKE SELECT ON public.purchase_history_detail FROM anon;

-- purchase_spend_by_month_store: "isOwn" joins the grouping, so a month
-- at a store returns up to two rows; summing them gives the household
-- total.
DROP VIEW IF EXISTS public.purchase_spend_by_month_store;

CREATE VIEW public.purchase_spend_by_month_store AS
SELECT
    date_trunc('month', ph.purchased_at)::date  AS "monthStart",
    ph.store_id                                 AS "storeId",
    s.name                                      AS "storeName",
    COALESCE(ph.user_id = (SELECT auth.uid()), false) AS "isOwn",
    SUM(COALESCE(ph.total_amount, items.line_total, 0))::numeric(12, 2) AS "total",
    COUNT(*)::int                               AS "tripCount"
FROM public.purchase_history ph
LEFT JOIN public.stores s ON s.id = ph.store_id
LEFT JOIN LATERAL (
    SELECT SUM(phi.quantity * phi.price_paid) AS line_total
    FROM public.purchase_history_items phi
    WHERE phi.purchase_id = ph.id
) items ON true
GROUP BY 1, 2, 3, 4;

ALTER VIEW public.purchase_spend_by_month_store SET (security_invoker = true);
GRANT SELECT ON public.purchase_spend_by_month_store TO authenticated;
REVOKE SELECT ON public.purchase_spend_by_month_store FROM anon;

-- purchase_trip_cost_by_store: "isOwn" added. Per trip, so no split.
DROP VIEW IF EXISTS public.purchase_trip_cost_by_store;

CREATE VIEW public.purchase_trip_cost_by_store AS
SELECT
    ph.id                                       AS "purchaseId",
    ph.purchased_at::date                       AS "purchasedOn",
    s.id                                        AS "storeId",
    s.name                                      AS "storeName",
    COALESCE(ph.user_id = (SELECT auth.uid()), false) AS "isOwn",
    COALESCE(SUM(phi.quantity * cur.price), 0)::numeric(12, 2) AS "costHere",
    COALESCE(
        SUM(phi.quantity * phi.price_paid) FILTER (WHERE cur.price IS NOT NULL), 0
    )::numeric(12, 2)                           AS "paidForSameItems",
    COUNT(cur.price)::int                       AS "itemsPriced",
    COUNT(*)::int                               AS "itemsTotal"
FROM public.purchase_history ph
JOIN public.purchase_history_items phi ON phi.purchase_id = ph.id
CROSS JOIN public.stores s
LEFT JOIN LATERAL (
    SELECT spp.price
    FROM public.store_product_pricing spp
    WHERE spp.store_id = s.id
      AND spp.product_id = phi.product_id
      AND spp.is_current = true
    ORDER BY spp.effective_date DESC
    LIMIT 1
) cur ON true
GROUP BY ph.id, ph.purchased_at, ph.user_id, s.id, s.name;

ALTER VIEW public.purchase_trip_cost_by_store SET (security_invoker = true);
GRANT SELECT ON public.purchase_trip_cost_by_store TO authenticated;
REVOKE SELECT ON public.purchase_trip_cost_by_store FROM anon;
