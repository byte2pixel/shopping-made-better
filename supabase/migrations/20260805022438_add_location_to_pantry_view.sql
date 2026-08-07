-- ============================================================
-- Migration: add location to pantry_items_by_expire view
-- ============================================================
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
    ii.location                 AS location
FROM public.inventory_items ii
JOIN public.products p ON p.id = ii.product_id
ORDER BY ii.expires_at ASC NULLS LAST;

GRANT SELECT ON public.pantry_items_by_expire TO authenticated;

ALTER VIEW public.pantry_items_by_expire SET (security_invoker = true);