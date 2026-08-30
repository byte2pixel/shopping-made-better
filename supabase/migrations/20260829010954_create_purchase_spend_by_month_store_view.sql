-- ============================================================
-- Migration: purchase_spend_by_month_store view (History insights, SCRUM-210)
-- ============================================================
-- One row per (month x store) of the caller's trips. The client derives both the
-- monthly totals and the per-store breakdown from this, so the tab reads it once.
--
-- "total" prefers total_amount over the summed lines, matching what the trip cards
-- show -- a card and the hero total must not disagree about the same trip.
--
-- "storeId"/"storeName" stay nullable: purchase_history.store_id is
-- ON DELETE SET NULL, so a trip outlives the store it was made at.
DROP VIEW IF EXISTS public.purchase_spend_by_month_store;

CREATE VIEW public.purchase_spend_by_month_store AS
SELECT
    date_trunc('month', ph.purchased_at)::date  AS "monthStart",
    ph.store_id                                 AS "storeId",
    s.name                                      AS "storeName",
    SUM(COALESCE(ph.total_amount, items.line_total, 0))::numeric(12, 2) AS "total",
    COUNT(*)::int                               AS "tripCount"
FROM public.purchase_history ph
LEFT JOIN public.stores s ON s.id = ph.store_id
LEFT JOIN LATERAL (
    SELECT SUM(phi.quantity * phi.price_paid) AS line_total
    FROM public.purchase_history_items phi
    WHERE phi.purchase_id = ph.id
) items ON true
GROUP BY 1, 2, 3;

-- security_invoker so RLS on purchase_history scopes the aggregate to the caller.
-- Without it every user would see everyone's spending.
ALTER VIEW public.purchase_spend_by_month_store SET (security_invoker = true);

GRANT SELECT ON public.purchase_spend_by_month_store TO authenticated;
REVOKE SELECT ON public.purchase_spend_by_month_store FROM anon;
