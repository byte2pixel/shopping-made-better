-- ============================================================
-- Migration: per-user, per-product low stock threshold
--   Stores the "warn me when running low" level as a per-user,
--   per-product setting rather than on inventory_items, so it
--   survives removing and re-adding the product to the pantry
--   (inventory_items rows are transient: created on purchase,
--   deleted on remove). Surfaced on pantry_items_by_expire via
--   a LEFT JOIN on (user_id, product_id).
-- ============================================================
CREATE TABLE public.user_product_stock_settings (
  id                  uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  user_id             uuid        NOT NULL DEFAULT auth.uid()
                        REFERENCES public.profiles(id) ON DELETE CASCADE,
  product_id          uuid        NOT NULL REFERENCES public.products(id) ON DELETE CASCADE,
  low_stock_threshold int         NOT NULL CHECK (low_stock_threshold > 0),
  created_at          timestamptz DEFAULT now() NOT NULL,
  updated_at          timestamptz DEFAULT now() NOT NULL,
  UNIQUE (user_id, product_id)
);

CREATE INDEX idx_stock_settings_user ON public.user_product_stock_settings (user_id);

CREATE TRIGGER trg_stock_settings_updated_at
  BEFORE UPDATE ON public.user_product_stock_settings
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- RLS
ALTER TABLE public.user_product_stock_settings ENABLE ROW LEVEL SECURITY;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.user_product_stock_settings TO authenticated;

CREATE POLICY "Users manage their own stock settings"
  ON public.user_product_stock_settings
  FOR ALL
  TO authenticated
  USING (user_id = (SELECT auth.uid()))
  WITH CHECK (user_id = (SELECT auth.uid()));

-- Recreate the pantry view to surface the per-product threshold
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
    s.low_stock_threshold       AS "lowStockThreshold"
FROM public.inventory_items ii
JOIN public.products p ON p.id = ii.product_id
LEFT JOIN public.user_product_stock_settings s
    ON s.product_id = ii.product_id AND s.user_id = ii.user_id
ORDER BY ii.expires_at ASC NULLS LAST;

GRANT SELECT ON public.pantry_items_by_expire TO authenticated;

ALTER VIEW public.pantry_items_by_expire SET (security_invoker = true);
