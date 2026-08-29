-- ============================================================
-- Migration: purchase_trip_cost_by_store view (History insights, SCRUM-210)
-- ============================================================
-- One row per (trip x store): what that basket would cost at that store today.
-- Feeds the trip detail's comparison (filtered by "purchaseId") and the tab's
-- savings card (filtered by "purchasedOn").
--
-- The comparison is across stores rather than over time: store_product_pricing
-- carries one is_current row per store/product, so "today's price" at the store a
-- trip was made at is just what was paid. Prices do differ between stores, which is
-- where the real delta comes from.
--
-- The lateral picks the newest is_current price rather than joining on is_current
-- directly: the unique constraint is per (store, product, effective_date), so two
-- current rows are possible and inside a SUM a duplicate silently doubles a line.
--
-- "paidForSameItems" covers only the items this store prices, so a store missing
-- half the basket cannot look cheap against the full amount paid. Paired with
-- "itemsPriced"/"itemsTotal", the client drops partial coverage instead of
-- comparing.
DROP VIEW IF EXISTS public.purchase_trip_cost_by_store;

CREATE VIEW public.purchase_trip_cost_by_store AS
SELECT
    ph.id                                       AS "purchaseId",
    ph.purchased_at::date                       AS "purchasedOn",
    s.id                                        AS "storeId",
    s.name                                      AS "storeName",
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
GROUP BY ph.id, ph.purchased_at, s.id, s.name;

-- security_invoker so RLS on purchase_history scopes this to the caller's trips.
-- stores and store_product_pricing are already readable by authenticated.
ALTER VIEW public.purchase_trip_cost_by_store SET (security_invoker = true);

GRANT SELECT ON public.purchase_trip_cost_by_store TO authenticated;
REVOKE SELECT ON public.purchase_trip_cost_by_store FROM anon;
