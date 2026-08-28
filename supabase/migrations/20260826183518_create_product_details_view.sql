-- ============================================================
-- Migration: product_details view (shared product detail screen, SCRUM-209)
-- ============================================================
-- One row per product in the catalog, with the current user's pantry position
-- folded in. History and the pantry both open the same product detail screen,
-- and a product bought on a past trip may no longer be held -- so the pantry
-- columns are LEFT JOINed and default to "none on hand" rather than dropping
-- the product from the view.
--
--   quantity          -> total on hand across every lot of the product, 0 when
--                        the user holds none. The pantry card shows the same
--                        aggregate on its header chip.
--   "expiryDate"      -> the soonest-expiring lot, NULL when none is held or no
--                        lot carries a date. The client turns it into a day count.
--   "lowStockThreshold" -> the per-user, per-product setting. It lives in
--                        user_product_stock_settings, not on inventory_items, so
--                        it survives removing the product from the pantry and is
--                        readable here even for a product that was never held.
--
-- security_invoker is what scopes both joins to the caller: the owner FOR ALL
-- policies on inventory_items and user_product_stock_settings filter them to the
-- current user, while products stays readable by every authenticated user.
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
LEFT JOIN public.user_product_stock_settings s ON s.product_id = p.id;

GRANT SELECT ON public.product_details TO authenticated;

ALTER VIEW public.product_details SET (security_invoker = true);
