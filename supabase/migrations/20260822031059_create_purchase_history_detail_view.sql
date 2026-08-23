-- ============================================================
-- Migration: purchase_history_detail view (History tab, SCRUM-181)
-- ============================================================
-- One row per purchased line item, with the trip header columns repeated on
-- every row. The client regroups the flat rows into trips, the same way
-- pantry_items_by_expire is regrouped into ProductGroups.
--
-- purchased_at is exposed twice on purpose:
--   * "purchasedOn"      date   -> what the card shows; decodes as LocalDate,
--                                 the only date type the app already uses.
--   * "purchasedAtEpoch" bigint -> sort key only, never displayed. Keeps
--                                 "newest first" exact for two trips on the
--                                 same day, which a date alone cannot do.
--
-- Nullability:
--   * purchase_history.store_id is ON DELETE SET NULL, so "storeName" is left
--     NULL rather than coalesced to '' -- the client substitutes a localized
--     "Unknown store".
--   * total_amount is nullable on the table. It is exposed as-is; the use case
--     falls back to sum(quantity * pricePaid) when it is NULL, so a trip
--     recorded without a total still shows a real number.
DROP VIEW IF EXISTS public.purchase_history_detail;

CREATE VIEW public.purchase_history_detail AS
SELECT
    phi.id                                       AS id,
    ph.id                                        AS "purchaseId",
    ph.purchased_at::date                        AS "purchasedOn",
    EXTRACT(EPOCH FROM ph.purchased_at)::bigint  AS "purchasedAtEpoch",
    s.name                                       AS "storeName",
    ph.total_amount                              AS "totalAmount",
    phi.product_id                               AS "productId",
    p.title                                      AS "productName",
    COALESCE(p.brand, '')                        AS brand,
    p.package_sizing                             AS size,
    COALESCE(p.image_url, '')                    AS "imageUrl",
    phi.quantity                                 AS quantity,
    phi.price_paid                               AS "pricePaid"
FROM public.purchase_history ph
JOIN public.purchase_history_items phi ON phi.purchase_id = ph.id
JOIN public.products p                 ON p.id = phi.product_id
LEFT JOIN public.stores s              ON s.id = ph.store_id
ORDER BY ph.purchased_at DESC, phi.created_at ASC;

GRANT SELECT ON public.purchase_history_detail TO authenticated;

ALTER VIEW public.purchase_history_detail SET (security_invoker = true);
