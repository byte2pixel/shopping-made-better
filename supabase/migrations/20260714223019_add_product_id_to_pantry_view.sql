-- ============================================================
-- Migration: add product_id to pantry_items_by_expire view
-- ============================================================
-- The pantry view exposed inventory_items.id (the pantry row id) but not the
-- underlying products FK. Adding a pantry item to a shopping list needs the
-- product_id, since shopping_list_items.product_id references products(id).
--
-- Same NOT-user-scoped note as the original view applies: the app uses the anon
-- key so there is no auth.uid() to filter by; with a single demo user locally
-- this returns their pantry.
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
  COALESCE(p.image_url, '')   AS "imageUrl"
FROM public.inventory_items ii
JOIN public.products p ON p.id = ii.product_id
ORDER BY ii.expires_at ASC NULLS LAST;

GRANT SELECT ON public.pantry_items_by_expire TO anon, authenticated;