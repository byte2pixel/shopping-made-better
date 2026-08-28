-- ============================================================
-- Migration: purchase_history_summary view (History tab paging, SCRUM-213)
-- ============================================================
-- One row per completed trip. The History list only ever shows a date, a store,
-- a total and an item count.
--
-- purchased_at is exposed twice, the same way purchase_history_detail does it:
--   * "purchasedOn"      date   -> what the card shows; decodes as LocalDate.
--   * "purchasedAtEpoch" bigint -> sort key only, never displayed. Keeps
--                                 "newest first" exact for two trips made on
--                                 the same day, which a date alone cannot do.
--
-- The total is exposed as two columns rather than one coalesced number:
--   * "totalAmount" -- what the user actually paid, as recorded. Nullable.
--   * "lineTotal"   -- sum(quantity * price_paid) over the trip's items.
-- The client prefers "totalAmount" and only falls back to "lineTotal"
--
-- LEFT JOIN, not JOIN: a trip recorded with no items still has a card, showing
-- 0 items and a 0.00 line total.
DROP VIEW IF EXISTS public.purchase_history_summary;

CREATE VIEW public.purchase_history_summary AS
SELECT
    ph.id                                        AS id,
    ph.purchased_at::date                        AS "purchasedOn",
    EXTRACT(EPOCH FROM ph.purchased_at)::bigint  AS "purchasedAtEpoch",
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

-- Re-assert the same posture on the detail view. It was created with both, but
-- stating it here keeps the two history views auditable from one migration.
ALTER VIEW public.purchase_history_detail SET (security_invoker = true);

GRANT SELECT ON public.purchase_history_detail TO authenticated;
REVOKE SELECT ON public.purchase_history_detail FROM anon;
