-- ============================================================
-- Migration: inventory_adjustments_detail view (weekly digest, SCRUM-224)
-- ============================================================
-- This week's automatic adjustments, one row each, with everything the digest
-- screen renders: the product, the lot's quantity after the change, the
-- product's total across the caller's lots with its low-stock threshold, and
-- the estimate's source.
--
-- Excluded: rows another row already reverses, and rows a later 'confirmed' or
-- 'manual' row on the same lot supersedes -- the user has since restated that
-- lot's quantity, so the estimate is moot. 'undo' and 'dismissed' do not
-- supersede.
--
-- delta and the quantities cast to int: they are whole units, and the client
-- decodes them into Int fields.
DROP VIEW IF EXISTS public.inventory_adjustments_detail;

CREATE VIEW public.inventory_adjustments_detail AS
SELECT
    a.id                        AS "adjustmentId",
    a.inventory_item_id         AS "inventoryItemId",
    ii.product_id               AS "productId",
    p.title                     AS "productName",
    COALESCE(p.image_url, '')   AS "imageUrl",
    a.delta::int                AS delta,
    ii.quantity::int            AS "quantityNow",
    tot.quantity::int           AS "productQuantity",
    s.low_stock_threshold       AS "lowStockThreshold",
    c.source                    AS "estimateSource",
    EXTRACT(EPOCH FROM a.created_at)::bigint AS "createdAtEpoch"
FROM public.inventory_adjustments a
JOIN public.inventory_items ii ON ii.id = a.inventory_item_id
JOIN public.products p        ON p.id = ii.product_id
LEFT JOIN public.user_product_stock_settings s
    ON s.product_id = ii.product_id AND s.user_id = a.user_id
LEFT JOIN public.user_product_consumption c
    ON c.product_id = ii.product_id AND c.user_id = a.user_id
LEFT JOIN LATERAL (
    SELECT SUM(x.quantity) AS quantity
    FROM public.inventory_items x
    WHERE x.product_id = ii.product_id AND x.user_id = a.user_id
) tot ON true
WHERE a.reason = 'auto'
  AND a.created_at >= now() - interval '7 days'
  AND NOT EXISTS (
        SELECT 1 FROM public.inventory_adjustments r
        WHERE r.reverses = a.id)
  AND NOT EXISTS (
        SELECT 1 FROM public.inventory_adjustments l
        WHERE l.inventory_item_id = a.inventory_item_id
          AND l.reason IN ('confirmed', 'manual')
          AND (l.created_at, l.id) > (a.created_at, a.id))
ORDER BY a.created_at DESC, a.id DESC;

-- security_invoker so RLS on inventory_adjustments scopes the joins to the
-- caller. The user_id predicates keep the settings, consumption and total joins
-- right for postgres too, which the seed relies on.
ALTER VIEW public.inventory_adjustments_detail SET (security_invoker = true);

GRANT SELECT ON public.inventory_adjustments_detail TO authenticated;
REVOKE SELECT ON public.inventory_adjustments_detail FROM anon;
