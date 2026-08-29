-- ============================================================
-- Migration: purchase_history_detail gains "addedToInventory" (SCRUM-212)
-- ============================================================
-- complete_shopping_trip has recorded purchase_history_items.added_to_inventory
-- since SCRUM-160, but the view never exposed it, so the trip detail screen could
-- not say which items were tracked into the pantry.
--
-- DROP + CREATE rather than CREATE OR REPLACE: the latter cannot change a view's
-- column list (42P16). That resets the view's options and privileges, so
-- security_invoker, the GRANT and the anon REVOKE are all re-applied below.
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
    phi.price_paid                               AS "pricePaid",
    phi.added_to_inventory                       AS "addedToInventory"
FROM public.purchase_history ph
JOIN public.purchase_history_items phi ON phi.purchase_id = ph.id
JOIN public.products p                 ON p.id = phi.product_id
LEFT JOIN public.stores s              ON s.id = ph.store_id
ORDER BY ph.purchased_at DESC, phi.created_at ASC;

ALTER VIEW public.purchase_history_detail SET (security_invoker = true);

GRANT SELECT ON public.purchase_history_detail TO authenticated;
REVOKE SELECT ON public.purchase_history_detail FROM anon;
