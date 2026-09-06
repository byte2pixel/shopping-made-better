-- ============================================================
-- SCRUM-222: expose the latest adjustment's id on the pantry view
-- ============================================================
-- Undo needs the id of the lot's newest inventory_adjustments row.
-- Adding a column needs DROP+CREATE (42P16); every existing column is kept.
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
    la.id                       AS "lastAdjustmentId"
FROM public.inventory_items ii
JOIN public.products p ON p.id = ii.product_id
LEFT JOIN public.user_product_stock_settings s
    ON s.product_id = ii.product_id AND s.user_id = ii.user_id
LEFT JOIN public.user_product_consumption c
    ON c.product_id = ii.product_id AND c.user_id = ii.user_id
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
