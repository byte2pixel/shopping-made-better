-- ============================================================
-- Migration: pantry_items_by_expire view
-- ============================================================
-- NOTE (local dev): the view is intentionally NOT filtered by the
-- current user. The app talks to Postgrest with the anon key
-- so there is no auth.uid() to scope by. With a single demo user
-- locally this returns their pantry.
--
-- per-user scoping is needed later, add `security_invoker = true` and a
-- `WHERE user_id = auth.uid()` filter once the app passes a session.
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW public.pantry_items_by_expire AS
SELECT
  ii.id                       AS id,
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