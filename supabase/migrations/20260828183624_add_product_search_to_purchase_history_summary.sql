-- ============================================================
-- Migration: purchase_history_summary gains "productSearch" (History search, SCRUM-211)
-- ============================================================
-- The History tab can now be searched by what was bought. The summary view holds
-- one row per trip and no product names at all, so a search had nothing on it to
-- match: this folds every item's title and brand for a trip into one text column.
--
-- Aggregated into the view rather than joined at query time so the search stays a
-- single filter over one row per trip.
--
-- The column is for filtering only; no card reads it.
--
-- Scaling note: the aggregate is evaluated for every trip a user has before the
-- page's LIMIT applies, because the filter runs on its result. Need a GIN solution if it was
-- a large app with large history.
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
    COALESCE(items.product_search, '')           AS "productSearch"
FROM public.purchase_history ph
LEFT JOIN public.stores s ON s.id = ph.store_id
LEFT JOIN LATERAL (
    SELECT
        COUNT(*)                                AS item_count,
        SUM(phi.quantity * phi.price_paid)      AS line_total,
        -- concat_ws drops a null brand rather than yielding a null row, and the
        -- join is LEFT so an unresolved product cannot quietly drop its item from
        -- item_count -- the count and the totals must not change because a search
        -- column was added beside them.
        string_agg(concat_ws(' ', p.title, p.brand), ' ') AS product_search
    FROM public.purchase_history_items phi
    LEFT JOIN public.products p ON p.id = phi.product_id
    WHERE phi.purchase_id = ph.id
) items ON true;

-- security_invoker so the view is filtered by the caller's RLS on
-- purchase_history ("Users manage their own purchase history"), not the view
-- owner's. Without it, every user would page through everyone's trips.
ALTER VIEW public.purchase_history_summary SET (security_invoker = true);

GRANT SELECT ON public.purchase_history_summary TO authenticated;
REVOKE SELECT ON public.purchase_history_summary FROM anon;
