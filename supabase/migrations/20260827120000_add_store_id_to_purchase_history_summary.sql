-- ============================================================
-- Migration: purchase_history_summary gains "storeId" (History filters, SCRUM-211)
-- ============================================================
-- The History tab can now be filtered by store. The filter matches on the
-- store's uuid rather than its name, for two reasons a name cannot cover:
--   * renaming a store would silently drop every one of its past trips out of
--     an active filter;
--   * two stores in the chain can share a name ("ALDI" on either side of town),
--     and a name filter would fold them into one.
--
-- The column is for filtering only -- the card still shows "storeName". It is
-- nullable for the same reason "storeName" is: purchase_history.store_id is
-- ON DELETE SET NULL, so a trip outlives the store it was made at.
--
-- Recreated rather than ALTERed: Postgres has no ADD COLUMN for a view, so the
-- whole definition is restated. It is otherwise identical to
-- 20260826021500_create_purchase_history_summary_view.sql.
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
    COALESCE(items.item_count, 0)::int           AS "itemCount"
FROM public.purchase_history ph
LEFT JOIN public.stores s ON s.id = ph.store_id
LEFT JOIN LATERAL (
    SELECT
        COUNT(*)                                AS item_count,
        SUM(phi.quantity * phi.price_paid)      AS line_total
    FROM public.purchase_history_items phi
    WHERE phi.purchase_id = ph.id
) items ON true;

-- security_invoker so the view is filtered by the caller's RLS on
-- purchase_history ("Users manage their own purchase history"), not the view
-- owner's. Without it, every user would page through everyone's trips.
ALTER VIEW public.purchase_history_summary SET (security_invoker = true);

GRANT SELECT ON public.purchase_history_summary TO authenticated;
REVOKE SELECT ON public.purchase_history_summary FROM anon;
